package com.lightning.northstar.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class NorthstarConfigs {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<Integer> OXYGEN_SEALER_MAX_VOLUME;
	public static final ForgeConfigSpec.ConfigValue<Boolean> RENDER_WEATHER;
	public static final ForgeConfigSpec.ConfigValue<Boolean> RENDER_SKY;

	static {
		BUILDER.push("Northstar Config");

		OXYGEN_SEALER_MAX_VOLUME = BUILDER.comment("The maximum volume in blocks that the oxygen sealer will try to seal. This is multiplied by the RPM of the machine. So 20 * 256 = 2560 max volume")
			.define("Sealer Volume", 20);

		// TODO: Move to client (Will never happen)
		RENDER_SKY = BUILDER.comment("Allow Northstar to render the sky")
			.define("Custom Sky Rendering", true);
		RENDER_WEATHER = BUILDER.comment("Allow Northstar to render custom weather")
			.define("Custom Weather Rendering", true);

		BUILDER.pop();
		SPEC = BUILDER.build();
	}
}
