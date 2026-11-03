package io.ejekta.bountiful.bridge;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the mixins that target optional third-party mods only when that mod's class is on the classpath, so a
 * missing mod does not log a "could not load target class" warning.
 */
public class BountifulMixinPlugin implements IMixinConfigPlugin {

    private static final String LITHOSTITCHED_MIXIN = "io.ejekta.bountiful.mixin.LithostitchedTemplatesMixin";
    private static final String LITHOSTITCHED_TARGET = "dev/worldgen/lithostitched/impl/worldgen/structure/LithostitchedTemplates.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (LITHOSTITCHED_MIXIN.equals(mixinClassName)) {
            return BountifulMixinPlugin.class.getClassLoader().getResource(LITHOSTITCHED_TARGET) != null;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
