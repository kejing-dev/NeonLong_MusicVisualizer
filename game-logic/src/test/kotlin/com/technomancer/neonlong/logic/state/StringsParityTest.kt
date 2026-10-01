package com.technomancer.neonlong.logic.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The English and Simplified Chinese string files must define the same keys. */
class StringsParityTest {
    private fun keys(path: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val out = HashSet<String>()
        for (tag in listOf("string", "plurals", "string-array")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as org.w3c.dom.Element
                if (el.getAttribute("translatable") != "false") out += "$tag:${el.getAttribute("name")}"
            }
        }
        return out
    }

    @Test
    fun englishAndChineseHaveTheSameKeys() {
        val res = "../app/src/main/res"
        val en = keys("$res/values/strings.xml")
        val zh = keys("$res/values-zh-rCN/strings.xml")
        assertTrue(en.isNotEmpty())
        assertEquals(emptySet<String>(), en - zh, "missing in Chinese")
        assertEquals(emptySet<String>(), zh - en, "only in Chinese")
    }
}
