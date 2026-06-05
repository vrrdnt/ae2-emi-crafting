package org.blocovermelho.ae2emi.platform.facade.crafting.recipe.category;

import appeng.core.localization.LocalizationEnum;

import java.util.Locale;

public enum TranslationKeys implements LocalizationEnum {
    CHARGER("Charger"),
    CONDENSER("Condenser"),
    ENTROPY_MANIPULATOR("Entropy Manipulator"),
    INSCRIBER("Inscriber"),
    ATTUNEMENT("P2P Tunnel Attunement"),
    TRANSFORM("In-World Transformation"),
    ;

    private final String englishText;

    TranslationKeys(String englishText) {
        this.englishText = englishText;
    }

    @Override
    public String getEnglishText() {
        return englishText;
    }

    @Override
    public String getTranslationKey() {
        return "ae2emi.category." + name().toLowerCase(Locale.ROOT);
    }
}
