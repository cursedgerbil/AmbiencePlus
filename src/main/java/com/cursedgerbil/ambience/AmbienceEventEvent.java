package com.cursedgerbil.ambience;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

// top lel name
// works as an api, feel free to include in your mods to add custom events
public class AmbienceEventEvent extends Event {
	
	// Set this string to something as the answer
	public String event = "";
	
	public Level world;
	public BlockPos pos;
	
	AmbienceEventEvent(Level world, BlockPos pos) {
		this.world = world;
		this.pos = pos;
	}
	
	public static class Pre extends AmbienceEventEvent {

		public Pre(Level world, BlockPos pos) {
			super(world, pos);
		} 
	}
	
	
	public static class Post extends AmbienceEventEvent {

		public Post(Level world, BlockPos pos) {
			super(world, pos);
		}
		
	}
}