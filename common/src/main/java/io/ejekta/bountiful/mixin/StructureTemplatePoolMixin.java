package io.ejekta.bountiful.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.ejekta.bountiful.content.JigsawBountyHelper;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Candidate list for every jigsaw slot, in vanilla's and in Lithostitched's jigsaw implementation. */
@Mixin(StructureTemplatePool.class)
public class StructureTemplatePoolMixin {

    @ModifyReturnValue(method = "getShuffledTemplates", at = @At("RETURN"))
    private List<StructurePoolElement> bountiful_applyBoardQuota(List<StructurePoolElement> candidates) {
        return JigsawBountyHelper.INSTANCE.filterCandidates(candidates);
    }
}
