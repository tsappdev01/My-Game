package com.mathsquest.app.ui.quiz

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.sound.SoundPlayer
import com.mathsquest.app.sound.SoundPlayer.Sfx
import com.mathsquest.app.ui.Args
import com.mathsquest.core.CoinReason
import com.mathsquest.core.CoinRules
import com.mathsquest.core.DailyChallenge
import com.mathsquest.core.Difficulty
import com.mathsquest.core.Question
import com.mathsquest.core.QuestionEngine
import com.mathsquest.core.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class Phase { LOADING, ASK, RIGHT, WRONG, REVEAL, DONE }

data class QuizState(
    val phase: Phase = Phase.LOADING,
    val childId: Long = 0,
    val daily: Boolean = false,
    val grade: Int = 0,
    val topic: Topic = Topic.ADD,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val index: Int = 0,
    val total: Int = ROUND_LENGTH,
    val question: Question? = null,
    val typed: String = "",
    val attempt: Int = 1,
    val worthCoins: Int = 0,
    val worthText: String = "",
    val earned: Int = 0,
    val xp: Int = 0,
    val note: String? = null,
    // Round summary
    val firstTryRight: Int = 0,
    val roundCoins: Int = 0,
    val roundXp: Int = 0,
    val summaryNotes: List<String> = emptyList(),
    val perfect: Boolean = false,
    /** Bumped on every celebration (right answer, round end) to trigger confetti and Leo's dance. */
    val celebrate: Int = 0,
    /** The new level when the last answer levelled the child up; shows the level-up party. */
    val levelUp: Int? = null,
) {
    val answered: Int get() = index + if (phase == Phase.RIGHT || phase == Phase.REVEAL || phase == Phase.DONE) 1 else 0
}

const val ROUND_LENGTH = 5

@HiltViewModel
class QuizViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: MathsQuestRepository,
    private val sound: SoundPlayer,
) : ViewModel() {
    private val childId: Long = checkNotNull(handle[Args.CHILD_ID])
    private val daily: Boolean = handle[Args.DAILY] ?: false
    private val topic = Topic.valueOf(handle[Args.TOPIC] ?: "ADD")
    private val difficulty = Difficulty.fromLevel(handle[Args.DIFFICULTY] ?: 2)
    private var grade: Int = handle[Args.GRADE] ?: 5
    private var childGrade = grade
    private var seed = 0L
    private var day = 0L
    private var shownAt = 0L
    private val reasons = mutableListOf<CoinReason>()

    private val _state = MutableStateFlow(QuizState(childId = childId, daily = daily))
    val state: StateFlow<QuizState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val child = repo.child(childId) ?: return@launch
            childGrade = child.grade
            day = repo.today()
            if (daily) grade = child.grade
            seed = Random.nextLong(1, 1_000_000)
            _state.update {
                it.copy(
                    grade = grade, topic = if (daily) Topic.MIXED else topic, difficulty = difficulty,
                    total = if (daily) DailyChallenge.difficulties.size else ROUND_LENGTH,
                )
            }
            show(0)
        }
    }

    private fun questionAt(i: Int): Question =
        if (daily) DailyChallenge.question(day, grade, i) else QuestionEngine.generate(grade, topic, difficulty, i, seed)

    private suspend fun show(i: Int, attempt: Int = 1) {
        val q = if (attempt == 1) questionAt(i) else _state.value.question!!
        val child = repo.child(childId) ?: return
        val earned = repo.earnedThisMonth(childId)
        val worth = CoinRules.worth(childGrade, q.grade, q.difficulty, attempt, earned, child.monthlyCapCoins)
        val worthText = when {
            CoinRules.isPracticeGrade(childGrade, q.grade) -> "Practice level · no coins"
            earned >= child.monthlyCapCoins -> "Coin jar full this month"
            attempt > 1 -> "Second try · worth ${coins(worth)}"
            daily -> "${q.difficulty.label} · worth ${coins(worth)}"
            else -> "Worth ${coins(worth)}"
        }
        shownAt = SystemClock.elapsedRealtime()
        _state.update {
            it.copy(
                phase = Phase.ASK, index = i, question = q, typed = "", attempt = attempt,
                worthCoins = worth, worthText = worthText, earned = 0, xp = 0, note = null,
            )
        }
    }

    fun type(digit: Char) {
        val s = _state.value
        if (s.phase != Phase.ASK || s.typed.length >= 7 || (s.typed.isEmpty() && digit == '0')) return
        sound.play(Sfx.TAP)
        _state.update { it.copy(typed = it.typed + digit) }
    }

    fun delete() = _state.update { s -> if (s.phase != Phase.ASK) s else s.copy(typed = s.typed.dropLast(1)) }

    fun check() {
        val s = _state.value
        val q = s.question ?: return
        if (s.phase != Phase.ASK || s.typed.isEmpty()) return
        val elapsed = SystemClock.elapsedRealtime() - shownAt
        val correct = s.typed.toLongOrNull() == q.answer
        _state.update { it.copy(phase = Phase.LOADING) }
        viewModelScope.launch {
            val xpBefore = repo.child(childId)?.xp ?: 0
            val outcome = repo.recordAnswer(childId, q, s.attempt, correct, elapsed, daily)
            if (outcome != null) {
                if (outcome.reason != CoinReason.NONE) reasons += outcome.reason
                val newLevel = CoinRules.level(xpBefore + outcome.xp)
                val levelledUp = newLevel > CoinRules.level(xpBefore)
                sound.play(Sfx.CORRECT)
                if (outcome.coins > 0) launch { delay(220); sound.play(Sfx.COIN) }
                if (levelledUp) launch { delay(500); sound.play(Sfx.LEVEL_UP) }
                _state.update {
                    it.copy(
                        phase = Phase.RIGHT,
                        celebrate = it.celebrate + 1,
                        levelUp = if (levelledUp) newLevel else null,
                        earned = outcome.coins,
                        xp = outcome.xp,
                        note = noteFor(outcome.reason, outcome.coins, q.grade),
                        firstTryRight = it.firstTryRight + if (s.attempt == 1) 1 else 0,
                        roundCoins = it.roundCoins + outcome.coins,
                        roundXp = it.roundXp + outcome.xp,
                    )
                }
            } else {
                sound.play(Sfx.WRONG)
                _state.update { it.copy(phase = if (s.attempt == 1) Phase.WRONG else Phase.REVEAL) }
            }
        }
    }

    /** Coin-drop clink as each part of the question lands. */
    fun playDrop() = sound.play(Sfx.DROP)

    fun dismissLevelUp() = _state.update { it.copy(levelUp = null) }

    fun retry() {
        viewModelScope.launch { show(_state.value.index, attempt = 2) }
    }

    fun next() {
        val s = _state.value
        val nextIndex = s.index + 1
        viewModelScope.launch {
            if (nextIndex < s.total) {
                show(nextIndex)
            } else {
                val perfect = s.firstTryRight == s.total
                val result = repo.finishRound(childId, perfect, daily)
                val notes = buildList {
                    if (result.streakHit) add("${result.streak} days in a row! " + if (result.bonusCoins > 0) "+${coins(result.bonusCoins)} bonus and the 7-Day Streak badge." else "You earned the 7-Day Streak badge.")
                    if (CoinReason.PRACTICE in reasons) add("This grade is practice for you. Pick Grade ${childGrade - 1} or higher to earn coins.")
                    val fast = reasons.count { it == CoinReason.TOO_FAST }
                    if (fast > 0) add("$fast ${if (fast == 1) "answer was" else "answers were"} too quick to earn coins. Take a moment on each one.")
                    if (CoinReason.CAP in reasons) add("Your coin jar is full for this month. Your parent can make it bigger.")
                    if (CoinReason.RETRY in reasons) add("Second tries earn one coin less, so it pays to check before you tap.")
                }
                sound.play(Sfx.COMPLETE)
                _state.update {
                    it.copy(
                        phase = Phase.DONE, perfect = perfect, summaryNotes = notes, levelUp = null,
                        roundCoins = it.roundCoins + result.bonusCoins, celebrate = it.celebrate + 1,
                    )
                }
            }
        }
    }

    private fun noteFor(reason: CoinReason, coins: Int, questionGrade: Int): String? = when (reason) {
        CoinReason.NONE -> null
        CoinReason.PRACTICE -> "Grade $questionGrade is practice for you, so it earns XP but not coins."
        CoinReason.TOO_FAST -> "Wow, that was quick! Coins count when you take a moment to work it out."
        CoinReason.RETRY -> if (coins > 0) "Second tries earn one coin less." else "Nice fix! Easy questions earn coins on the first try."
        CoinReason.CAP -> if (coins > 0) "That filled your coin jar for this month!" else "Your coin jar is full for this month. You still earn XP!"
    }

    private fun coins(n: Int) = "$n ${if (n == 1) "coin" else "coins"}"
}
