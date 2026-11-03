package io.ejekta.bountiful.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.ejekta.bountiful.content.JigsawBountyHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Predicate;

/**
 * Brackets one whole structure generation (find the start, then run the piece generator) so the bounty-board quota
 * is tracked per structure. This is deliberately independent of which jigsaw algorithm runs inside: vanilla's
 * {@code JigsawPlacement} and replacements such as Lithostitched's {@code AlternateJigsawGenerator} (which
 * Lithostitched swaps in for every jigsaw structure, and which Trek requires) are both covered, because the
 * quota is applied to the pool candidate lists and piece creation, which both use.
 */
@Mixin(Structure.class)
public class StructureGenerateMixin {

    @WrapMethod(method = "generate")
    private StructureStart bountiful_trackBoardQuota(
        Holder<Structure> selected, ResourceKey<Level> dimension, RegistryAccess registryAccess,
        ChunkGenerator chunkGenerator, BiomeSource biomeSource, Climate.Sampler climateSampler,
        RandomState randomState, StructureTemplateManager structureTemplateManager, long seed,
        ChunkPos sourceChunkPos, int references, LevelHeightAccessor heightAccessor,
        Predicate<Holder<Biome>> validBiome, Operation<StructureStart> original
    ) {
        JigsawBountyHelper.INSTANCE.onStructureGenerateStart(seed, sourceChunkPos.x(), sourceChunkPos.z());
        try {
            return original.call(
                selected, dimension, registryAccess, chunkGenerator, biomeSource, climateSampler, randomState,
                structureTemplateManager, seed, sourceChunkPos, references, heightAccessor, validBiome
            );
        } finally {
            JigsawBountyHelper.INSTANCE.onStructureGenerateEnd();
        }
    }
}
