package com.technomancer.neonlong.logic.math

/** Mutable linear RGB color. Components may exceed 1 to express emissive gain. */
class Color3(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f) {
    fun set(r: Float, g: Float, b: Float): Color3 {
        this.r = r; this.g = g; this.b = b
        return this
    }

    fun set(o: Color3): Color3 = set(o.r, o.g, o.b)

    fun scale(s: Float): Color3 = set(r * s, g * s, b * s)

    /** this = a + (b − a) · t */
    fun lerp(a: Color3, b: Color3, t: Float): Color3 =
        set(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t)

    companion object {
        /** Parses "#rrggbb" or "rrggbb" into 0..1 components. */
        fun fromHex(hex: String): Color3 {
            val h = hex.removePrefix("#")
            require(h.length == 6) { "expected #rrggbb, got $hex" }
            val v = h.toInt(16)
            return Color3(((v shr 16) and 0xff) / 255f, ((v shr 8) and 0xff) / 255f, (v and 0xff) / 255f)
        }
    }
}
