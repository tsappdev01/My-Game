package com.mathsquest.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mathsquest.app.ui.daily.DailyScreen
import com.mathsquest.app.ui.home.HomeScreen
import com.mathsquest.app.ui.parent.AddChildScreen
import com.mathsquest.app.ui.parent.ParentDashboardScreen
import com.mathsquest.app.ui.parent.PinScreen
import com.mathsquest.app.ui.parent.SetupScreen
import com.mathsquest.app.ui.profiles.ProfilesScreen
import com.mathsquest.app.ui.progress.ProgressScreen
import com.mathsquest.app.ui.quest.DifficultyScreen
import com.mathsquest.app.ui.quest.GradeScreen
import com.mathsquest.app.ui.quest.TopicScreen
import com.mathsquest.app.ui.quiz.QuizScreen
import com.mathsquest.app.ui.rewards.RewardsScreen
import com.mathsquest.app.ui.start.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val PROFILES = "profiles"
    const val HOME = "home/{childId}"
    const val GRADE = "grade/{childId}"
    const val TOPIC = "topic/{childId}/{grade}"
    const val DIFFICULTY = "difficulty/{childId}/{grade}/{topic}"
    const val QUIZ = "quiz/{childId}/{grade}/{topic}/{difficulty}/{daily}"
    const val DAILY = "daily/{childId}"
    const val PROGRESS = "progress/{childId}"
    const val REWARDS = "rewards/{childId}"
    const val PIN = "pin"
    const val PARENT = "parent"
    const val ADD_CHILD = "add-child"

    fun home(childId: Long) = "home/$childId"
    fun grade(childId: Long) = "grade/$childId"
    fun topic(childId: Long, grade: Int) = "topic/$childId/$grade"
    fun difficulty(childId: Long, grade: Int, topic: String) = "difficulty/$childId/$grade/$topic"
    fun quiz(childId: Long, grade: Int, topic: String, difficulty: Int, daily: Boolean) = "quiz/$childId/$grade/$topic/$difficulty/$daily"
    fun daily(childId: Long) = "daily/$childId"
    fun progress(childId: Long) = "progress/$childId"
    fun rewards(childId: Long) = "rewards/$childId"
}

object Args {
    const val CHILD_ID = "childId"
    const val GRADE = "grade"
    const val TOPIC = "topic"
    const val DIFFICULTY = "difficulty"
    const val DAILY = "daily"
}

private val childArg = navArgument(Args.CHILD_ID) { type = NavType.LongType }
private val gradeArg = navArgument(Args.GRADE) { type = NavType.IntType }
private val topicArg = navArgument(Args.TOPIC) { type = NavType.StringType }

@Composable
fun MathsQuestNavHost(nav: NavHostController = rememberNavController()) {
    fun goHome(childId: Long) = nav.navigate(Routes.home(childId)) { popUpTo(Routes.PROFILES) }
    fun goProfiles() = nav.navigate(Routes.PROFILES) { popUpTo(0) { inclusive = true } }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onStart = { setUp -> nav.navigate(if (setUp) Routes.PROFILES else Routes.SETUP) { popUpTo(0) { inclusive = true } } })
        }
        composable(Routes.SETUP) { SetupScreen(onDone = { goProfiles() }) }
        composable(Routes.PROFILES) {
            ProfilesScreen(onPick = { nav.navigate(Routes.home(it)) }, onParent = { nav.navigate(Routes.PIN) })
        }
        composable(Routes.HOME, listOf(childArg)) {
            HomeScreen(
                onPlay = { id -> nav.navigate(Routes.grade(id)) },
                onDaily = { id -> nav.navigate(Routes.daily(id)) },
                onProgress = { id -> nav.navigate(Routes.progress(id)) },
                onRewards = { id -> nav.navigate(Routes.rewards(id)) },
                onSwitch = { goProfiles() },
            )
        }
        composable(Routes.GRADE, listOf(childArg)) {
            GradeScreen(onBack = { nav.popBackStack() }, onPick = { id, g -> nav.navigate(Routes.topic(id, g)) })
        }
        composable(Routes.TOPIC, listOf(childArg, gradeArg)) {
            TopicScreen(onBack = { nav.popBackStack() }, onPick = { id, g, t -> nav.navigate(Routes.difficulty(id, g, t)) })
        }
        composable(Routes.DIFFICULTY, listOf(childArg, gradeArg, topicArg)) {
            DifficultyScreen(onBack = { nav.popBackStack() }, onPick = { id, g, t, d -> nav.navigate(Routes.quiz(id, g, t, d, false)) })
        }
        composable(
            Routes.QUIZ,
            listOf(
                childArg, gradeArg, topicArg,
                navArgument(Args.DIFFICULTY) { type = NavType.IntType },
                navArgument(Args.DAILY) { type = NavType.BoolType },
            ),
        ) {
            QuizScreen(
                onExit = { id -> goHome(id) },
                onPlayAgain = { id, g, t, d -> nav.navigate(Routes.quiz(id, g, t, d, false)) { popUpTo(Routes.home(id)) } },
            )
        }
        composable(Routes.DAILY, listOf(childArg)) {
            DailyScreen(onBack = { nav.popBackStack() }, onStart = { id, g -> nav.navigate(Routes.quiz(id, g, "MIXED", 1, true)) })
        }
        composable(Routes.PROGRESS, listOf(childArg)) { ProgressScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.REWARDS, listOf(childArg)) { RewardsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PIN) {
            PinScreen(onBack = { nav.popBackStack() }, onUnlocked = { nav.navigate(Routes.PARENT) { popUpTo(Routes.PROFILES) } })
        }
        composable(Routes.PARENT) {
            ParentDashboardScreen(onExit = { goProfiles() }, onAddChild = { nav.navigate(Routes.ADD_CHILD) })
        }
        composable(Routes.ADD_CHILD) { AddChildScreen(onDone = { nav.popBackStack() }) }
    }
}
