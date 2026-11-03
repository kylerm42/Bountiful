package io.ejekta.bountiful.mixin;

import io.ejekta.bountiful.content.JigsawBountyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Jigsaw generators create a piece with this constructor exactly when a candidate was accepted (not while probing),
 * so it is the point where a bounty board is counted against the village's quota.
 */
@Mixin(PoolElementStructurePiece.class)
public class PoolElementStructurePieceMixin {

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/world/level/levelgen/structure/pools/StructurePoolElement;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V",
        at = @At("TAIL")
    )
    private void bountiful_countBoard(
        StructureTemplateManager manager, StructurePoolElement element, BlockPos position, int groundLevelDelta,
        Rotation rotation, BoundingBox boundingBox, LiquidSettings liquidSettings, CallbackInfo ci
    ) {
        JigsawBountyHelper.INSTANCE.onPieceCreated(element);
    }
}
