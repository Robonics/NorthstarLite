package com.lightning.northstar.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class NorthstarConfigs {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<Boolean> TEMPERATURE_STUFF_ENABLED;
	public static final ForgeConfigSpec.ConfigValue<Integer> OXYGEN_SEALER_MAX_VOLUME;

	static {
		BUILDER.push("Northstar Config");

		TEMPERATURE_STUFF_ENABLED = BUILDER.comment("Should northstar handle temperature? (This will be automatically disabled if ColdSweat is installed)")
			.define("Temperature Enabled", true);
		OXYGEN_SEALER_MAX_VOLUME = BUILDER.comment("The maximum volume in blocks that the oxygen sealer will try to seal")
			.define("Sealer Volume", 1000);

		BUILDER.pop();
		SPEC = BUILDER.build();
	}
}
