package io.ejekta.bountiful.content

import io.ejekta.bountiful.config.BountifulIO
import io.ejekta.bountiful.mixin.SinglePoolElementAccessor
import net.minecraft.resources.Identifier
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import java.util.Random

/**
 * Enforces `board.villageGenFrequency` (boards per village) for jigsaw villages, whichever jigsaw implementation
 * generates them (vanilla `JigsawPlacement`, or Lithostitched's replacement, which Trek villages go through).
 *
 * Mixins feed three events: a structure generation begins/ends ([onStructureGenerateStart]/[onStructureGenerateEnd]),
 * a candidate list is produced for a jigsaw slot ([filterCandidates]), and a piece is created for an accepted
 * candidate ([onPieceCreated]). Outside a structure generation nothing is filtered.
 */
object JigsawBountyHelper {

    /**
     * The bounty gazebo template and its variants. A variant exists for each jigsaw connector name a village mod
     * expects: `bounty_gazebo` connects with `minecraft:building_entrance`, `bounty_gazebo_floor` (a vertical
     * connector on the gazebo's floor) with `minecraft:building_floor` (Towns and Towers' nilotic village).
     */
    private val BOUNTY_GAZEBO_IDS = setOf(
        Identifier.fromNamespaceAndPath("bountiful", "village/common/bounty_gazebo"),
        Identifier.fromNamespaceAndPath("bountiful", "village/common/bounty_gazebo_floor")
    )

    private class Quota(val allowed: Int) {
        var placed = 0
    }

    // null = not inside a structure generation call on this thread
    private val quota: ThreadLocal<Quota?> = ThreadLocal.withInitial { null }

    /** Number of boards to generate in a village with the given frequency. [roll] is a uniform value in [0, 1). */
    fun boardsFor(frequency: Float, roll: Float): Int {
        if (frequency <= 0f) return 0
        val guaranteed = frequency.toInt()
        val extraChance = frequency - guaranteed
        return guaranteed + if (roll < extraChance) 1 else 0
    }

    fun onStructureGenerateStart(worldSeed: Long, chunkX: Int, chunkZ: Int) {
        // Deterministic per world + start chunk, independent of the world generation RNG
        val seed = worldSeed xor (chunkX.toLong() * 341873128712L + chunkZ.toLong() * 132897987541L) xor 0xB04B044BL
        val allowed = boardsFor(BountifulIO.configData.board.villageGenFrequency, Random(seed).nextFloat())
        quota.set(Quota(allowed))
    }

    fun onStructureGenerateEnd() {
        quota.remove()
    }

    fun isBountyBoardElement(element: StructurePoolElement): Boolean {
        if (element !is SinglePoolElement) return false
        val accessor = element as SinglePoolElementAccessor
        val id: Identifier = accessor.bountiful_getTemplate().left().orElse(null) ?: return false
        return id in BOUNTY_GAZEBO_IDS
    }

    fun isBountyBoardPiece(piece: PoolElementStructurePiece): Boolean = isBountyBoardElement(piece.element)

    fun onPieceCreated(element: StructurePoolElement) {
        val q = quota.get() ?: return
        if (isBountyBoardElement(element)) q.placed++
    }

    /**
     * Removes the board from candidate lists once the quota is full; until then moves it to the front so it is
     * tried before any competing house piece (otherwise a weight-1 entry would often never be picked).
     */
    fun filterCandidates(candidates: List<StructurePoolElement>): List<StructurePoolElement> {
        val q = quota.get() ?: return candidates
        val boards = candidates.filter { isBountyBoardElement(it) }
        if (boards.isEmpty()) return candidates
        val others = candidates.filter { !isBountyBoardElement(it) }
        return if (q.placed >= q.allowed) others else boards + others
    }
}
