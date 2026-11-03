package io.ejekta.bountiful.chaos

import io.ejekta.bountiful.config.JsonFormats

/** How many entries/rules a [mergeDefaults] call took from the bundled defaults. */
data class ChaosMergeResult(
    val required: Int = 0,
    val optional: Int = 0,
    val tags: Int = 0,
    val regex: Int = 0,
    val ignoreRegex: Int = 0
) {
    val total: Int get() = required + optional + tags + regex + ignoreRegex
}

object ChaosDefaults {

    const val RESOURCE_PATH = "/data/bountiful/chaos/bountiful-chaos.default.json"

    /** Parses the curated defaults bundled in the mod jar. Throws if the resource is missing or malformed. */
    fun load(): BountifulChaosData {
        val stream = ChaosDefaults::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("Bundled chaos defaults are missing from the mod jar: $RESOURCE_PATH")
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        return JsonFormats.Config.json.decodeFromString(BountifulChaosData.serializer(), text)
    }

    /**
     * Merges [defaults] into [data] without ever changing a value that is already set.
     *
     * - `required` / `optional`: a missing or `null` entry takes the default's price when that price is non-null.
     *   Default entries that [data]'s own regex rules already price, or that its enabled ignore rules
     *   exclude, are skipped: they would only be removed or left unused.
     * - `matching.tag` / `regex` / `ignoreRegex`: keys that are missing are added; existing keys are untouched.
     *
     * Running it again on its own output changes nothing.
     */
    fun mergeDefaults(data: BountifulChaosData, defaults: BountifulChaosData): ChaosMergeResult {
        var tags = 0
        for ((key, value) in defaults.matching.tag) {
            if (key !in data.matching.tag) {
                data.matching.tag[key] = value
                tags++
            }
        }
        var regex = 0
        for ((key, value) in defaults.matching.regex) {
            if (key !in data.matching.regex) {
                data.matching.regex[key] = value
                regex++
            }
        }
        var ignore = 0
        for ((key, value) in defaults.matching.ignoreRegex) {
            if (key !in data.matching.ignoreRegex) {
                data.matching.ignoreRegex[key] = value
                ignore++
            }
        }

        // Ids that need no price: already priced by a regex rule (the solver would drop them as redundant) or ignored
        val skipRegexes = data.matching.regex.keys.map { Regex(it) } +
            data.matching.ignoreRegex.filterValues { it }.keys.map { Regex(it) }
        var required = 0
        for ((id, price) in defaults.required) {
            if (price == null || data.required[id] != null) continue
            if (skipRegexes.any { it.matches(id.toString()) }) continue
            data.required[id] = price
            required++
        }
        var optional = 0
        for ((id, price) in defaults.optional) {
            if (price == null || data.optional[id] != null || data.required[id] != null) continue
            if (skipRegexes.any { it.matches(id.toString()) }) continue
            data.optional[id] = price
            optional++
        }

        data.required = data.required.toSortedMap(compareBy { it.toString() }).toMutableMap()
        data.optional = data.optional.toSortedMap(compareBy { it.toString() }).toMutableMap()
        return ChaosMergeResult(required, optional, tags, regex, ignore)
    }
}
