package io.ejekta.bountiful.chaos

import kotlin.test.Test
import kotlin.test.assertEquals

class DepthSolverPricingTest {

    private val prices = mapOf("a" to 2.0, "b" to 3.0)

    @Test
    fun `should leave recipe unsolved when a slot has no priceable option`() {
        val result = DepthSolver.priceIngredients(listOf(listOf("a"), listOf("unknown"))) { prices[it] }

        assertEquals(1, result.unsolved)
        assertEquals(2.0, result.cost)
    }

    @Test
    fun `should report zero unsolved when every slot is priced`() {
        val result = DepthSolver.priceIngredients(listOf(listOf("a"), listOf("b"))) { prices[it] }

        assertEquals(0, result.unsolved)
        assertEquals(5.0, result.cost)
    }

    @Test
    fun `should take the first priceable option in a slot`() {
        val result = DepthSolver.priceIngredients(listOf(listOf("unknown", "b", "a"))) { prices[it] }

        assertEquals(0, result.unsolved)
        assertEquals(3.0, result.cost)
    }

    @Test
    fun `should count an empty option set as unsolved`() {
        val result = DepthSolver.priceIngredients(listOf(listOf("a"), emptyList())) { prices[it] }

        assertEquals(1, result.unsolved)
    }

    @Test
    fun `should report zero cost and zero unsolved for a recipe with no slots`() {
        val result = DepthSolver.priceIngredients(emptyList<List<String>>()) { prices[it] }

        assertEquals(0, result.unsolved)
        assertEquals(0.0, result.cost)
    }
}
