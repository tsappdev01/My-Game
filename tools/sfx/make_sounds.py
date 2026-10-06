"""Synthesises Maths Quest's sound effects as small mono WAV files (no external assets).

Usage: python3 tools/sfx/make_sounds.py   (run from the repo root)
Writes app/src/main/res/raw/sfx_*.wav
"""
import math, os, struct, wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'res', 'raw')


def note(freq, dur, vol=0.5, wave_shape='sine', attack=0.005, decay=None, slide_to=None):
    """One note with a fast attack and an exponential (bell-like) decay."""
    n = int(dur * RATE)
    decay = decay if decay is not None else dur / 4
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = freq if slide_to is None else freq + (slide_to - freq) * (i / n)
        phase += 2 * math.pi * f / RATE
        if wave_shape == 'sine':
            s = math.sin(phase) + 0.25 * math.sin(2 * phase) + 0.1 * math.sin(3 * phase)
            s /= 1.35
        elif wave_shape == 'triangle':
            s = 2 / math.pi * math.asin(math.sin(phase))
        else:
            s = math.sin(phase)
        env = min(1.0, t / attack) * math.exp(-t / decay)
        out.append(s * env * vol)
    return out


def silence(dur):
    return [0.0] * int(dur * RATE)


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def seq(*parts):
    out = []
    for p in parts:
        out += p
    return out


def offset(track, at):
    return silence(at) + track


def write(name, samples):
    peak = max(1e-6, max(abs(s) for s in samples))
    scale = min(1.0, 0.9 / peak)
    fade = int(0.01 * RATE)
    for i in range(1, fade + 1):  # avoid a click at the end
        samples[-i] *= i / fade
    path = os.path.join(OUT, f'sfx_{name}.wav')
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, s * scale)) * 32767)) for s in samples))
    return path


C5, E5, G5, A5, C6, E6, G6, B6, C7 = 523.25, 659.25, 783.99, 880.0, 1046.5, 1318.5, 1568.0, 1975.5, 2093.0

SOUNDS = {
    # Soft keypad tick.
    'tap': note(1800, 0.035, vol=0.25, wave_shape='pure', decay=0.008),
    # Bright two-note "ding-ding" for a right answer.
    'correct': mix(note(E6, 0.35, 0.45, decay=0.12), offset(note(B6, 0.5, 0.45, decay=0.18), 0.09)),
    # Quick rising sparkle when coins land.
    'coin': mix(*[offset(note(f, 0.18, 0.3, decay=0.05), i * 0.045) for i, f in enumerate([C6, E6, G6, C7])]),
    # Gentle descending "bwomp": a nudge, not a punishment.
    'wrong': seq(note(392.0, 0.16, 0.35, 'triangle', decay=0.09, slide_to=370.0),
                 note(311.1, 0.28, 0.35, 'triangle', decay=0.12, slide_to=277.2)),
    # Fanfare for a new level: arpeggio then a held chord.
    'levelup': seq(
        mix(*[offset(note(f, 0.22, 0.35, decay=0.08), i * 0.1) for i, f in enumerate([C5, E5, G5, C6])]),
        mix(note(C6, 0.7, 0.3, decay=0.3), note(E6, 0.7, 0.25, decay=0.3), note(G6, 0.7, 0.22, decay=0.3)),
    ),
    # Short "ta-da" for finishing a round.
    'complete': seq(
        mix(note(G5, 0.14, 0.35, decay=0.06), note(C6, 0.14, 0.25, decay=0.06)),
        silence(0.03),
        mix(note(C6, 0.55, 0.4, decay=0.22), note(E6, 0.55, 0.3, decay=0.22), note(G6, 0.55, 0.25, decay=0.22)),
    ),
}

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, samples in SOUNDS.items():
        p = write(name, samples)
        print(f'{os.path.basename(p)}: {os.path.getsize(p) // 1024} KB, {len(samples) / RATE:.2f} s')
