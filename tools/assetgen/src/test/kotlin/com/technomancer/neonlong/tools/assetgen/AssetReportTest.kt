package com.technomancer.neonlong.tools.assetgen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AssetReportTest {
    @Test
    fun dragonFitsTheTriangleBudget() {
        val report = Report()
        for ((mesh, _) in allModels()) report.model(mesh)
        val total = report.dragonTriangles()
        println("[AssetReportTest] dragon triangles $total")
        assertTrue(total <= 200_000, "dragon draws $total triangles")
    }

    @Test
    fun headAndJawBlockCounts() {
        assertEquals(24, HeadParts.headBlockCount)
        assertEquals(5, HeadParts.jawBlockCount)
    }
}
