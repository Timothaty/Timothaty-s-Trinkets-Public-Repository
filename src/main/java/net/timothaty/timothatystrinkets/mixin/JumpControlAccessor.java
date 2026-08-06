package net.timothaty.timothatystrinkets.mixin;

import net.minecraft.world.entity.ai.control.JumpControl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(JumpControl.class)
public interface JumpControlAccessor {
	@Accessor("jump")
	void timothatys_trinkets$setJumpRequested(boolean value);
}
