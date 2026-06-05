package org.blocovermelho.ae2emi.platform.facade;

import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Ae2EmiMixinPlugin implements IMixinConfigPlugin {
    private static String MIXIN_PACKAGE_ROOT = "org.blocovermelho.ae2emi.mixin.";
    private static String MIXIN_COMPAT_ROOT = "org.blocovermelho.ae2emi.mixin.compat.";
    private static HashMap<String, ImmutableSet<String>> MOD_COMPAT_APPLY = new HashMap<>();
    private static HashSet<String> TO_APPLY;

    static {
        MOD_COMPAT_APPLY.put("extra-mod-integrations", ImmutableSet.of("ExmiDisableMixin"));
        TO_APPLY = getMixinsToEnable();
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE_ROOT)) {
            Ae2EmiMod.LOGGER.error("[Mixin] Foreign Mixin Detected and Skipped.");
            Ae2EmiMod.LOGGER.error("[Mixin] {} [APPLY -> {}].", mixinClassName, targetClassName);
            return false;
        }

        if (mixinClassName.startsWith(MIXIN_COMPAT_ROOT)) {
            var className = mixinClassName.replace(MIXIN_COMPAT_ROOT, "");

            if (TO_APPLY.contains(className)) {
                Ae2EmiMod.LOGGER.info("[Mod Compatibility] Applying mixin: {}.", className);
                return true;
            }
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private static HashSet<String> getMixinsToEnable() {
        var set = new HashSet<String>();
        for (var entry : MOD_COMPAT_APPLY.entrySet()) {
            if (ModLoadedUtil.ae2emi_isModLoaded(entry.getKey())) {
                set.addAll(entry.getValue());
            }
        }

        return set;
    }
}
