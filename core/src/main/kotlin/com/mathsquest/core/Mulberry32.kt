package com.mathsquest.core

/**
 * Small deterministic PRNG (mulberry32). The server uses the same algorithm, so a
 * (seed, index) pair always produces the same question on the device and on the API.
 */
class Mulberry32(seed: Int) {
    private var state: Int = seed

    /** Returns a double in [0, 1). */
    fun next(): Double {
        state += 0x6D2B79F5
        var r = (state xor (state ushr 15)) * (1 or state)
        r = r xor (r + (r xor (r ushr 7)) * (61 or r))
        val out = (r xor (r ushr 14)).toLong() and 0xFFFF_FFFFL
        return out.toDouble() / 4_294_967_296.0
    }

    /** Uniform integer in [lo, hi]. */
    fun nextInt(lo: Long, hi: Long): Long = lo + (next() * (hi - lo + 1)).toLong()
}
