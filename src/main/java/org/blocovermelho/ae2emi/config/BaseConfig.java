package org.blocovermelho.ae2emi.config;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;

public class BaseConfig extends WrappedConfig {
    public ModCompat compat = new ModCompat();

    public class ModCompat implements Section {
        @Comment("Disables the ExMI AppEng integration.")
        @Comment("Reason: Both mods register the same recipes and would lead to duplicated recipes showing.")
        @Comment("It is recommended to keep this enabled, since it only takes effect if you have said mod loaded.")
        @Comment("Setting this to \"false\" would make the mod prefer ExMI's recipes instead of ae2emi's.")
        @Comment("Requires rejoining the world/server to take effect.")
        public boolean disableExMIAe2Integration = true;
    }
}
