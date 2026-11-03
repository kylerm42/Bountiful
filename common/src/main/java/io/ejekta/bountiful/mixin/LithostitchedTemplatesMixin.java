package io.ejekta.bountiful.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.ejekta.bountiful.content.JigsawBountyHelper;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Lithostitched replaces vanilla's jigsaw placement for every jigsaw structure (including all villages, vanilla and
 * Trek) with its own generator, whose candidate lists come from {@code LithostitchedTemplates.shuffle} rather than
 * {@code StructureTemplatePool.getShuffledTemplates}. This applies the same board quota there. Skipped when
 * Lithostitched is not installed. {@code require = 0}: if a Lithostitched update renames this internal method the
 * hook silently stops applying; {@code villageBoardsHonorGenFrequency} (run with Lithostitched, see its docs) detects that.
 */
@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.structure.LithostitchedTemplates")
public class LithostitchedTemplatesMixin {

    @ModifyReturnValue(method = "shuffle", at = @At("RETURN"), require = 0)
    private List<StructurePoolElement> bountiful_applyBoardQuota(List<StructurePoolElement> candidates) {
        return JigsawBountyHelper.INSTANCE.filterCandidates(candidates);
    }
}
