package org.blocovermelho.ae2emi.mixin;

import appeng.util.ExternalSearch;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.emi.emi.api.EmiApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExternalSearch.class, remap = false)
public class SearchSyncMixin {
    @ModifyReturnValue(method = "isExternalSearchAvailable", at = @At("TAIL"))
    private static boolean ae2emi$setSearchAvailable(boolean original) {
        return true;
    }

    @ModifyReturnValue(method = "getExternalSearchText", at  = @At("TAIL"))
    private static String ae2emi$syncAe2Search(String original) {
        return EmiApi.getSearchText();
    }

    @ModifyReturnValue(method = "isExternalSearchFocused", at = @At("TAIL"))
    private static boolean ae2emi$syncEmiFocus(boolean original) {
        return EmiApi.isSearchFocused();
    }

    @Inject(method = "setExternalSearchText", at = @At("HEAD"))
    private static void ae2emi$syncEmiSearch(String text, CallbackInfo ci) {
        EmiApi.setSearchText(text);
    }

    @Inject(method = "clearExternalSearchText", at = @At("HEAD"))
    private static void ae2emi$clearEmiSearch(CallbackInfo ci) {
        EmiApi.setSearchText("");
    }
}
