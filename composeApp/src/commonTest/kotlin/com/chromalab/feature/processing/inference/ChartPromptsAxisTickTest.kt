package com.chromalab.feature.processing.inference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartPromptsAxisTickTest {

    @Test
    fun parseResponseKeepsAxisTickPositionsWhenModelProvidesThem() {
        val response = """
            {"x":[0,10,20],"y":[0,100000,200000],"x_ticks":[{"text":"0","value":0,"position":0.1},{"text":"20","value":20,"position":0.9}],"y_ticks":[{"text":"200000","value":200000,"position":0.05},{"text":"0","value":0,"position":0.95}],"x_unit":"min","y_unit":"abundance"}
        """.trimIndent()

        val analysis = ChartPrompts.parseResponse(response)

        assertEquals(listOf(0f, 10f, 20f), analysis.xValues)
        assertEquals(listOf(0f, 100000f, 200000f), analysis.yValues)
        assertEquals(0.1f, analysis.xTicks.first().position)
        assertEquals(0.95f, analysis.yTicks.last().position)
        assertEquals("min", analysis.xUnit)
        assertEquals("abundance", analysis.yUnit)
    }

    @Test
    fun parseResponseStillAcceptsLegacyAxisArraysWithoutTickObjects() {
        val response = """{"x":[0,10,20],"y":[0,100000,200000],"x_unit":"min","y_unit":"abundance"}"""

        val analysis = ChartPrompts.parseResponse(response)

        assertEquals(listOf(0f, 10f, 20f), analysis.xValues)
        assertEquals(listOf(0f, 100000f, 200000f), analysis.yValues)
        assertEquals(emptyList(), analysis.xTicks)
        assertEquals(emptyList(), analysis.yTicks)
    }

    @Test
    fun parseResponseKeepsVisibleAxisValuesFromTruncatedPrimitiveArray() {
        val response = """{"x":[0.0,10.0,15.0,20.0,25.0,30.0,35.0,40.0,45.0,50.0,55.0],"y":[0,50000,100000"""

        val analysis = ChartPrompts.parseResponse(response)

        assertEquals(listOf(0f, 10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f), analysis.xValues)
        assertEquals(listOf(0f, 50000f, 100000f), analysis.yValues)
    }

    @Test
    fun qwenStructuredPromptsDisableThinkingBeforeJsonOutput() {
        val prompts = listOf(
            ChartPrompts.graphRegionPrompt(PromptStyle.CHATML),
            ChartPrompts.graphRegionRetryPrompt(PromptStyle.CHATML),
            ChartPrompts.axisExtractionPrompt(PromptStyle.CHATML),
            ChartPrompts.axisExtractionRetryPrompt(PromptStyle.CHATML),
            ChartPrompts.axisStructurePrompt(PromptStyle.CHATML),
            ChartPrompts.axisStructureRetryPrompt(PromptStyle.CHATML),
        )

        prompts.forEach { prompt ->
            assertTrue("/no_think" in prompt)
            assertTrue("<think>\n\n</think>" in prompt)
            assertTrue("first generated character" in prompt)
        }
    }
}
