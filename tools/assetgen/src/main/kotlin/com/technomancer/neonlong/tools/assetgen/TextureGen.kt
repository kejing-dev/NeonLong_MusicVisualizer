package com.technomancer.neonlong.tools.assetgen

import com.technomancer.neonlong.logic.math.Rng
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Procedural textures, drawn with Java2D. White-on-black maps are used as emissive masks. */
object TextureGen {
    private fun white(a: Double) = Color(1f, 1f, 1f, a.coerceIn(0.0, 1.0).toFloat())

    private fun canvas(w: Int, h: Int, bg: Color = Color.BLACK): Pair<BufferedImage, Graphics2D> {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.color = bg
        g.fillRect(0, 0, w, h)
        return img to g
    }

    /**
     * Building facade for blocks and towers: rows of lit and dark windows, faint floor lines,
     * and a glowing band along the texture's edges (the neon edge trim).
     *
     * The window pattern is designed on a 256 × 512 grid. It's drawn at [scale]× resolution and
     * repeated [tiles] × [tiles] times inside one image, so each block face shows several
     * rows of small, sharp windows without relying on texture wrapping; the edge band is only
     * on the outer edge.
     */
    fun facade(scale: Int = 2, tiles: Int = 2): BufferedImage {
        val tileW = 256.0 * scale
        val tileH = 512.0 * scale
        val (img, g) = canvas((tileW * tiles).toInt(), (tileH * tiles).toInt())
        val rng = Rng(3)
        // One window layout, reused for every tile so the pattern stays deterministic.
        val alphas = DoubleArray(16 * 8) {
            val lit = rng.next() > 0.42
            if (lit) 0.3 + 0.7 * rng.next() else 0.025
        }
        for (ty in 0 until tiles) for (tx in 0 until tiles) {
            val ox = tx * tileW
            val oy = ty * tileH
            for (row in 0 until 16) {
                g.color = white(0.07)
                g.fill(Rectangle2D.Double(ox, oy + row * 32.0 * scale, tileW, 2.0 * scale))
                for (col in 0 until 8) {
                    g.color = white(alphas[row * 8 + col])
                    g.fill(Rectangle2D.Double(ox + (col * 32.0 + 6) * scale, oy + (row * 32.0 + 8) * scale, 20.0 * scale, 17.0 * scale))
                }
            }
        }
        g.dispose()
        addEdgeGlow(img, width = 0.035, gain = 1.3)
        return img
    }

    /**
     * Night environment for the dragon's metal (image-based light): an equirectangular sky (2:1),
     * dark blue-violet from #07060d at the zenith to #1a0f33 at the horizon, dark below, with
     * one soft magenta key toward +X at 20% intensity.
     */
    fun nightEnvironment(width: Int = 1024): BufferedImage {
        val h = width / 2
        val img = BufferedImage(width, h, BufferedImage.TYPE_INT_RGB)
        val zenith = floatArrayOf(0x07 / 255f, 0x06 / 255f, 0x0d / 255f)
        val horizon = floatArrayOf(0x1a / 255f, 0x0f / 255f, 0x33 / 255f)
        for (y in 0 until h) for (x in 0 until width) {
            val lat = Math.PI / 2 - (y + 0.5) / h * Math.PI // +π/2 up … −π/2 down
            val lon = (x + 0.5) / width * 2 * Math.PI - Math.PI // −π … π, 0 = +X
            val t = if (lat >= 0) 1 - lat / (Math.PI / 2) else 1 + 2 * lat / Math.PI // 1 at horizon
            val base = FloatArray(3) { (zenith[it] + (horizon[it] - zenith[it]) * t.toFloat().coerceIn(0f, 1f)) }
            // Soft magenta key toward +X, a little above the horizon.
            val dx = kotlin.math.cos(lat) * kotlin.math.cos(lon)
            val dy = kotlin.math.sin(lat)
            val dz = kotlin.math.cos(lat) * kotlin.math.sin(lon)
            val keyDot = (dx * 0.94 + dy * 0.34 + dz * 0.0).coerceAtLeast(0.0)
            val key = Math.pow(keyDot, 24.0).toFloat() * 0.2f
            val r = (base[0] + key * 1.0f).coerceIn(0f, 1f)
            val gg = (base[1] + key * 0.18f).coerceIn(0f, 1f)
            val b = (base[2] + key * 0.53f).coerceIn(0f, 1f)
            img.setRGB(x, y, ((r * 255).roundToInt() shl 16) or ((gg * 255).roundToInt() shl 8) or (b * 255).roundToInt())
        }
        return img
    }

    /** Adds (1 − smoothstep(0, [width], distance to the nearest UV edge)) · [gain], clamped. */
    private fun addEdgeGlow(img: BufferedImage, width: Double, gain: Double) {
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val u = (x + 0.5) / img.width
            val v = (y + 0.5) / img.height
            val d = min(min(u, 1 - u), min(v, 1 - v))
            val t = (d / width).coerceIn(0.0, 1.0)
            val glow = (1 - t * t * (3 - 2 * t)) * gain
            val c = img.getRGB(x, y) and 0xff
            val out = min(255, (c + glow * 255).roundToInt())
            img.setRGB(x, y, (0xff shl 24) or (out shl 16) or (out shl 8) or out)
        }
    }

    /**
     * Armor panel (512²): seams, rivets, trims, a 回 key-fret band, circuit traces and small
     * window clusters. With [bump] = true, draws the same panel layout as dark lines on gray
     * for a bump map, without traces or windows.
     */
    fun armour(bump: Boolean = false): BufferedImage {
        val s = 512.0
        val (img, g) = canvas(512, 512, if (bump) Color(0x8a, 0x8a, 0x8a) else Color.BLACK)
        val rng = Rng(7)
        fun ink(a: Double) = if (bump) Color(0f, 0f, 0f, a.toFloat()) else white(a)

        // Seams.
        g.stroke = BasicStroke(1.5f)
        g.color = ink(0.22)
        for (k in 0 until 9) g.draw(Line2D.Double(64.0 * k, 0.12 * s, 64.0 * k, 0.90 * s))
        g.draw(Line2D.Double(0.0, 0.5 * s, s, 0.5 * s))
        // Rivets.
        g.color = ink(0.5)
        repeat(26) {
            val x = rng.next() * s
            val y = (0.15 + rng.next() * 0.70) * s
            g.fill(Ellipse2D.Double(x - 2.5, y - 2.5, 5.0, 5.0))
        }
        // Trims.
        g.color = ink(1.0)
        g.fill(Rectangle2D.Double(0.0, 0.0, s, 0.035 * s))
        g.fill(Rectangle2D.Double(0.0, s - 0.035 * s, s, 0.035 * s))
        g.color = ink(0.35)
        g.fill(Rectangle2D.Double(0.0, 0.06 * s, s, 0.012 * s))
        g.fill(Rectangle2D.Double(0.0, 0.928 * s, s, 0.012 * s))
        // 回 key-fret band.
        g.color = ink(0.75)
        g.stroke = BasicStroke(2f)
        val y0 = 0.16 * s
        val cell = 0.10 * s
        var x = 0.0
        while (x < s) {
            val p = Path2D.Double()
            p.moveTo(x, y0 + cell); p.lineTo(x, y0); p.lineTo(x + cell, y0); p.lineTo(x + cell, y0 + 0.8 * cell)
            p.lineTo(x + 0.3 * cell, y0 + 0.8 * cell); p.lineTo(x + 0.3 * cell, y0 + 0.35 * cell)
            p.lineTo(x + 0.65 * cell, y0 + 0.35 * cell); p.lineTo(x + 0.65 * cell, y0 + 0.55 * cell)
            g.draw(p)
            g.draw(Line2D.Double(x, y0 + cell, x + 1.2 * cell, y0 + cell))
            x += 1.2 * cell
        }
        if (!bump) {
            // Circuit traces: three steps each, right or diagonal, ending in a dot.
            repeat(14) {
                var px = rng.next() * s * 0.8
                var py = (0.55 + rng.next() * 0.35) * s
                val alpha = 0.35 + rng.next() * 0.55
                val width = 1.5 + rng.next() * 1.5
                g.color = white(alpha)
                g.stroke = BasicStroke(width.toFloat())
                repeat(3) {
                    val len = 20 + rng.next() * 50
                    val diagonal = rng.next() < 0.5
                    val nx = px + if (diagonal) len * 0.7 else len
                    val ny = py + if (diagonal) (if (rng.next() < 0.5) -1 else 1) * len * 0.7 else 0.0
                    g.draw(Line2D.Double(px, py, nx, ny))
                    px = nx; py = ny
                }
                g.fill(Ellipse2D.Double(px - 3.5, py - 3.5, 7.0, 7.0))
            }
            // Window clusters in the lower 45%.
            repeat(5) {
                val cx = rng.next() * (s - 60)
                val cy = (0.55 + rng.next() * 0.35) * s
                for (r in 0 until 3) for (c in 0 until 5) {
                    g.color = white(0.2 + 0.6 * rng.next())
                    g.fill(Rectangle2D.Double(cx + c * 11, cy + r * 10, 7.0, 6.0))
                }
            }
        }
        g.dispose()
        return img
    }

    /**
     * Skin for the body and head blocks (1024²): a 回 key-fret band under the top edge, rows of
     * small lit windows, panel seams with rivets, circuit traces ending in pads, a thin inner
     * outline, and the bright neon band along the outer edges.
     */
    fun bodySkin(): BufferedImage {
        val s = 1024.0
        val (img, g) = canvas(1024, 1024)
        val rng = Rng(11)

        // Windows: 12 rows × 14 columns between the fret band and the bottom margin.
        val top = 0.27 * s
        val rowH = (0.90 * s - top) / 12
        val colW = (0.88 * s) / 14
        for (row in 0 until 12) {
            g.color = white(0.06)
            g.fill(Rectangle2D.Double(0.06 * s, top + row * rowH, 0.88 * s, 2.0))
            for (col in 0 until 14) {
                // Mostly dark, so the face stays black from a distance and the lit windows stand out.
                val lit = rng.next() > 0.68
                g.color = white(if (lit) 0.55 + 0.45 * rng.next() else 0.0)
                g.fill(Rectangle2D.Double(0.06 * s + col * colW + colW * 0.2, top + row * rowH + rowH * 0.28, colW * 0.5, rowH * 0.42))
            }
        }
        // Panel seams and rivets.
        g.stroke = BasicStroke(2f)
        g.color = white(0.22)
        for (k in 1 until 4) g.draw(Line2D.Double(s * k / 4, 0.25 * s, s * k / 4, 0.93 * s))
        g.draw(Line2D.Double(0.05 * s, 0.585 * s, 0.95 * s, 0.585 * s))
        g.color = white(0.65)
        for (k in 1 until 4) for (j in 0..4) {
            val y = (0.29 + 0.15 * j) * s
            g.fill(Ellipse2D.Double(s * k / 4 - 5, y - 5, 10.0, 10.0))
        }
        // 回 key-fret band.
        g.color = white(0.85)
        g.stroke = BasicStroke(4f)
        val y0 = 0.10 * s
        val cell = 0.105 * s
        var x = 0.065 * s
        while (x + cell < 0.94 * s) {
            val p = Path2D.Double()
            p.moveTo(x, y0 + cell); p.lineTo(x, y0); p.lineTo(x + cell, y0); p.lineTo(x + cell, y0 + 0.8 * cell)
            p.lineTo(x + 0.3 * cell, y0 + 0.8 * cell); p.lineTo(x + 0.3 * cell, y0 + 0.35 * cell)
            p.lineTo(x + 0.65 * cell, y0 + 0.35 * cell); p.lineTo(x + 0.65 * cell, y0 + 0.55 * cell)
            g.draw(p)
            x += 1.2 * cell
        }
        g.stroke = BasicStroke(4f)
        g.draw(Line2D.Double(0.06 * s, y0 + cell + 12, 0.94 * s, y0 + cell + 12))
        // Circuit traces: three steps each, right or diagonal, ending in a pad.
        repeat(9) {
            var px = (0.08 + rng.next() * 0.6) * s
            var py = (0.32 + rng.next() * 0.55) * s
            g.color = white(0.55 + rng.next() * 0.45)
            g.stroke = BasicStroke((2.5 + rng.next() * 2.0).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            repeat(3) {
                val len = 40 + rng.next() * 100
                val diagonal = rng.next() < 0.5
                val nx = min(px + if (diagonal) len * 0.7 else len, 0.93 * s)
                val ny = (py + if (diagonal) (if (rng.next() < 0.5) -1 else 1) * len * 0.7 else 0.0).coerceIn(0.28 * s, 0.92 * s)
                g.draw(Line2D.Double(px, py, nx, ny))
                px = nx; py = ny
            }
            g.fill(Ellipse2D.Double(px - 8, py - 8, 16.0, 16.0))
        }
        // Thin inner outline.
        g.color = white(0.4)
        g.stroke = BasicStroke(3f)
        g.draw(Rectangle2D.Double(0.055 * s, 0.055 * s, 0.89 * s, 0.89 * s))
        g.dispose()
        addEdgeGlow(img, width = 0.04, gain = 1.5)
        return img
    }

    /**
     * Soft round glow for additive halos (128²): white fading to black from the center, with
     * no transparency, so it adds light without darkening anything.
     */
    fun halo(): BufferedImage {
        val n = 128
        val img = BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until n) for (x in 0 until n) {
            val dx = (x + 0.5) / n * 2 - 1
            val dy = (y + 0.5) / n * 2 - 1
            val r = kotlin.math.sqrt(dx * dx + dy * dy)
            // A bright core with a long, soft tail.
            val a = if (r >= 1.0) 0.0 else (1 - r) * (1 - r) * (0.55 + 0.45 * kotlin.math.exp(-r * r * 18))
            val v = (a.coerceIn(0.0, 1.0) * 255).roundToInt()
            img.setRGB(x, y, (0xff shl 24) or (v shl 16) or (v shl 8) or v)
        }
        return img
    }

    /**
     * The 龍 glyph (128²): transparent, a 4 px white border inset 4 px, and the character in
     * Noto Serif SC Black at 96 px with a soft 10 px glow. Uses [fontFile] if it exists,
     * otherwise the system serif font (and says so in [note]).
     */
    fun glyph(fontFile: File, note: (String) -> Unit): BufferedImage {
        val n = 128
        val font = if (fontFile.isFile) {
            java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile).deriveFont(96f)
        } else {
            note("Noto Serif SC Black not found at ${fontFile.path}; GEN_Glyph_Long uses the system serif font")
            java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 96)
        }
        // The character alone, to blur into a glow.
        val text = BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB)
        text.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            this.font = font
            color = Color.WHITE
            val fm = fontMetrics
            val w = fm.stringWidth("龍")
            drawString("龍", (n - w) / 2, (n - fm.height) / 2 + fm.ascent)
            dispose()
        }
        val glow = boxBlurAlpha(text, 10)
        val out = BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until n) for (x in 0 until n) {
            val border = x in 4 until n - 4 && y in 4 until n - 4 &&
                (x < 8 || x >= n - 8 || y < 8 || y >= n - 8)
            val a = max(max((text.getRGB(x, y) ushr 24), (glow[y * n + x] * 0.8).roundToInt()), if (border) 255 else 0)
            out.setRGB(x, y, (a shl 24) or 0xffffff)
        }
        return out
    }

    /** Alpha of [img] blurred with a box of half-width [r], two passes. */
    private fun boxBlurAlpha(img: BufferedImage, r: Int): DoubleArray {
        val w = img.width; val h = img.height
        var a = DoubleArray(w * h) { ((img.getRGB(it % w, it / w) ushr 24) and 0xff).toDouble() }
        repeat(2) {
            val tmp = DoubleArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                var s = 0.0; var c = 0
                for (k in -r..r) { val xx = x + k; if (xx in 0 until w) { s += a[y * w + xx]; c++ } }
                tmp[y * w + x] = s / c
            }
            val out = DoubleArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                var s = 0.0; var c = 0
                for (k in -r..r) { val yy = y + k; if (yy in 0 until h) { s += tmp[yy * w + x]; c++ } }
                out[y * w + x] = s / c
            }
            a = out
        }
        return a
    }

    fun writePng(img: BufferedImage, file: File) {
        file.parentFile?.mkdirs()
        ImageIO.write(img, "png", file)
    }
}
