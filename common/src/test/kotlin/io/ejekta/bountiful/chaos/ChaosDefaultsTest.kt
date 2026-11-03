package io.ejekta.bountiful.chaos

import io.ejekta.bountiful.config.JsonFormats
import net.minecraft.resources.Identifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChaosDefaultsTest {

    private fun id(s: String) = Identifier.parse(s)

    private fun defaults() = BountifulChaosData(
        BountifulChaosMatching(
            tag = mutableMapOf(id("minecraft:logs") to 240.0),
            regex = mutableMapOf("minecraft:.*_concrete" to 10.0),
            ignoreRegex = mutableMapOf("minecraft:waxed_.*" to true)
        ),
        required = mutableMapOf(id("minecraft:apple") to 200.0, id("minecraft:bamboo") to 50.0, id("minecraft:bone") to null),
        optional = mutableMapOf(id("minecraft:tropical_fish") to 165.0, id("minecraft:azalea") to null)
    )

    @Test
    fun `should seed an empty config with every default`() {
        val data = BountifulChaosData()

        val result = ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(200.0, data.required[id("minecraft:apple")])
        assertEquals(50.0, data.required[id("minecraft:bamboo")])
        assertEquals(165.0, data.optional[id("minecraft:tropical_fish")])
        assertEquals(240.0, data.matching.tag[id("minecraft:logs")])
        assertEquals(10.0, data.matching.regex["minecraft:.*_concrete"])
        assertEquals(true, data.matching.ignoreRegex["minecraft:waxed_.*"])
        assertEquals(ChaosMergeResult(required = 2, optional = 1, tags = 1, regex = 1, ignoreRegex = 1), result)
    }

    @Test
    fun `should not add default entries that have no price`() {
        val data = BountifulChaosData()

        ChaosDefaults.mergeDefaults(data, defaults())

        assertTrue(id("minecraft:bone") !in data.required)
        assertTrue(id("minecraft:azalea") !in data.optional)
    }

    @Test
    fun `should fill null and missing entries from defaults`() {
        val data = BountifulChaosData(required = mutableMapOf(id("minecraft:apple") to null))

        ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(200.0, data.required[id("minecraft:apple")])
        assertEquals(50.0, data.required[id("minecraft:bamboo")])
    }

    @Test
    fun `should never overwrite a price the user set`() {
        val data = BountifulChaosData(
            required = mutableMapOf(id("minecraft:apple") to 7.0),
            optional = mutableMapOf(id("minecraft:tropical_fish") to 1.0)
        )

        val result = ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(7.0, data.required[id("minecraft:apple")])
        assertEquals(1.0, data.optional[id("minecraft:tropical_fish")])
        assertEquals(1, result.required)
        assertEquals(0, result.optional)
    }

    @Test
    fun `should add missing rule keys but keep user rule values`() {
        val data = BountifulChaosData(
            BountifulChaosMatching(
                tag = mutableMapOf(id("minecraft:logs") to 1.0),
                regex = mutableMapOf("minecraft:.*_concrete" to 2.0),
                ignoreRegex = mutableMapOf("minecraft:waxed_.*" to false, "minecraft:custom" to true)
            )
        )

        val result = ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(1.0, data.matching.tag[id("minecraft:logs")])
        assertEquals(2.0, data.matching.regex["minecraft:.*_concrete"])
        assertEquals(false, data.matching.ignoreRegex["minecraft:waxed_.*"])
        assertEquals(true, data.matching.ignoreRegex["minecraft:custom"])
        assertEquals(0, result.tags + result.regex + result.ignoreRegex)
    }

    @Test
    fun `should keep the user's removed-from-defaults entries`() {
        val data = BountifulChaosData(required = mutableMapOf(id("minecraft:custom_item") to 3.0))

        ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(3.0, data.required[id("minecraft:custom_item")])
    }

    @Test
    fun `should not re-add an optional default that is already priced in required`() {
        val data = BountifulChaosData(required = mutableMapOf(id("minecraft:tropical_fish") to 165.0))

        val result = ChaosDefaults.mergeDefaults(data, defaults())

        assertTrue(id("minecraft:tropical_fish") !in data.optional)
        assertEquals(0, result.optional)
    }

    @Test
    fun `should skip default prices already covered by a regex rule`() {
        val defaults = defaults().apply { required[id("minecraft:red_concrete")] = 99.0 }
        val data = BountifulChaosData()

        ChaosDefaults.mergeDefaults(data, defaults)

        assertNull(data.required[id("minecraft:red_concrete")])
    }

    @Test
    fun `should skip default prices for ids the ignore rules exclude`() {
        val defaults = defaults().apply { required[id("minecraft:waxed_copper_block")] = 99.0 }
        val data = BountifulChaosData()

        ChaosDefaults.mergeDefaults(data, defaults)

        assertNull(data.required[id("minecraft:waxed_copper_block")])
    }

    @Test
    fun `should sort merged entries by id`() {
        val data = BountifulChaosData(required = mutableMapOf(id("minecraft:zzz") to 1.0, id("minecraft:bone") to null))

        ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(data.required.keys.map { it.toString() }.sorted(), data.required.keys.map { it.toString() })
    }

    @Test
    fun `should change nothing when merged twice`() {
        val data = BountifulChaosData()
        ChaosDefaults.mergeDefaults(data, defaults())
        val snapshot = JsonFormats.Config.json.encodeToString(BountifulChaosData.serializer(), data)

        val second = ChaosDefaults.mergeDefaults(data, defaults())

        assertEquals(0, second.total)
        assertEquals(snapshot, JsonFormats.Config.json.encodeToString(BountifulChaosData.serializer(), data))
    }

    @Test
    fun `should load the bundled defaults with curated prices and rules`() {
        val bundled = ChaosDefaults.load()

        assertTrue(bundled.required.values.count { it != null } > 100)
        assertNotNull(bundled.required[id("minecraft:apple")])
        assertTrue(bundled.matching.tag.isNotEmpty() && bundled.matching.regex.isNotEmpty())
        assertEquals(true, bundled.matching.ignoreRegex["minecraft:waxed_.*"])
        assertEquals(true, bundled.matching.ignoreRegex["minecraft:weathered_.*"])
        assertEquals(true, bundled.matching.ignoreRegex["minecraft:oxidized_.*"])
    }

    @Test
    fun `should repair a user config where every price is null`() {
        val text = javaClass.getResourceAsStream("/chaos/all-null-user-config.json")!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val data = JsonFormats.Config.json.decodeFromString(BountifulChaosData.serializer(), text)
        assertTrue(data.required.isNotEmpty() && data.required.values.all { it == null })
        assertTrue(data.matching.regex.isEmpty())
        val nullKeys = data.required.keys + data.optional.keys
        val bundled = ChaosDefaults.load()

        val result = ChaosDefaults.mergeDefaults(data, bundled)

        assertTrue(result.required > 50, "most bundled prices apply to the user's discovered terminators, got ${result.required}")
        assertEquals(bundled.matching.regex.keys, data.matching.regex.keys)
        assertEquals(bundled.matching.tag.keys, data.matching.tag.keys)
        assertEquals(bundled.matching.ignoreRegex.keys, data.matching.ignoreRegex.keys)
        assertTrue(data.required.values.count { it != null } >= result.required)
        // entries the user's file already listed stay listed
        assertTrue(nullKeys.all { it in data.required || it in data.optional })
        assertEquals(0, ChaosDefaults.mergeDefaults(data, bundled).total)
    }
}
