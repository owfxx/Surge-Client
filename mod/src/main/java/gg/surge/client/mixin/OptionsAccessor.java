package gg.surge.client.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Options.class)
public interface OptionsAccessor {
	@Accessor("glintSpeed")
	OptionInstance<Double> surge$glintSpeed();

	@Accessor("glintStrength")
	OptionInstance<Double> surge$glintStrength();
}
