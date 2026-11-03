package io.ejekta.bountiful.test

import io.ejekta.bountiful.content.JigsawBountyHelper
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext

/**
 * Generates village structure starts in memory (the same code path as `/place structure` and chunk generation,
 * `Structure.generate` -> `JigsawStructure` -> `JigsawPlacement.addPieces`) and counts the bounty gazebo pieces
 * in each start. Nothing is placed in the world.
 */
object VillageBoardCounts {

    val VANILLA_VILLAGES = listOf("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga")
    val TREK_VILLAGES = listOf(
        "trek:village/plains", "trek:village/desert", "trek:village/savanna", "trek:village/snowy",
        "trek:village/taiga", "trek:village/cherry", "trek:village/swamp_vanilla", "trek:village/mushroom"
    )

    fun idOf(name: String): Identifier = if (':' in name) Identifier.parse(name) else Identifier.withDefaultNamespace(name)

    /** Boards per village for `samples` starts spread over the world; null when the structure is not registered. */
    fun count(level: ServerLevel, structureId: String, samples: Int, seedOffset: Int = 0): List<Int>? {
        val key = ResourceKey.create(Registries.STRUCTURE, idOf(structureId))
        val holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key).orElse(null) ?: return null
        val generator = level.chunkSource.generator
        val randomState = level.chunkSource.randomState()
        val counts = mutableListOf<Int>()
        val pieceTotals = mutableListOf<Int>()
        zombieVillages = 0
        for (i in 0 until samples) {
            val chunk = ChunkPos((i + seedOffset) * 37 - 600, (i + seedOffset) * 53 - 900)
            val start: StructureStart = holder.value().generate(
                holder, level.dimension(), level.registryAccess(), generator, generator.biomeSource,
                randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED), randomState,
                level.structureTemplateManager, level.seed, chunk, 0, level
            ) { true }
            if (!start.isValid) continue
            pieceTotals += start.pieces.size
            // Zombie-village variants use separate `.../zombie/houses` pools that do not contain the gazebo
            if (start.pieces.any { it is PoolElementStructurePiece && "/zombie/" in it.element.toString() }) {
                zombieVillages++
                continue
            }
            val boards = start.pieces.count { it is PoolElementStructurePiece && JigsawBountyHelper.isBountyBoardPiece(it) }
            counts += boards
        }
        lastPieceTotals = pieceTotals
        return counts
    }

    /** Total piece counts of the villages measured by the last [count] call (to tell a boardless village from a degenerate one). */
    /** Zombie-variant villages skipped by the last [count] call (they have no bounty board by design). */
    var zombieVillages = 0
        private set

    var lastPieceTotals: List<Int> = emptyList()
        private set

    fun summary(counts: List<Int>): String =
        "n=${counts.size} min=${counts.minOrNull()} max=${counts.maxOrNull()} avg=${"%.2f".format(counts.average())} hist=${counts.groupingBy { it }.eachCount().toSortedMap()}"
}
