package gg.surge.legacy.mixin;

import gg.surge.legacy.Keys;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
	@Shadow public KeyBinding[] allKeys;

	@Inject(method = "load", at = @At("HEAD"))
	private void surge$keys(CallbackInfo ci) {
		if (allKeys == null) return;
		List<KeyBinding> list = new ArrayList<KeyBinding>(Arrays.asList(allKeys));
		boolean changed = false;
		for (KeyBinding k : Keys.ALL) if (!list.contains(k)) { list.add(k); changed = true; }
		if (changed) allKeys = list.toArray(new KeyBinding[0]);
	}
}
