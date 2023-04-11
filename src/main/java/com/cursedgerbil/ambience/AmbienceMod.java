
package com.cursedgerbil.ambience;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;


@Mod("ambience")
public class AmbienceMod {
	public static final String MOD_ID = "ambience";
	public static final String MOD_NAME = MOD_ID;
	public static final String BUILD = "GRADLE:BUILD";
	public static final String VERSION = "GRADLE:VERSION-" + BUILD;
	public static final String DEPENDENCIES = "";

	private static final int WAIT_DURATION = 40;
	public static final int FADE_DURATION = 40;
	public static final int SILENCE_DURATION = 20;


	public static PlayerThread thread;
	
	String currentSong;
	String nextSong;
	int waitTick = WAIT_DURATION;
	int fadeOutTicks = FADE_DURATION;
	int fadeInTicks = 0;
	int silenceTicks = 0;
	static int bossDelay = 0;
	static String bossPresent = null;
	static List<TagKey<Biome>> biomes = null;
	static Registry<Biome> biomeregistry = null;

	public AmbienceMod() {
		
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {return;}
		MinecraftForge.EVENT_BUS.register(this);
		
		File ambienceDir = new File(Minecraft.getInstance().gameDirectory, "/ambience_music");
		if(!ambienceDir.exists())
			ambienceDir.mkdir();
		
		SongLoader.loadFrom(ambienceDir);
		
		if(SongLoader.enabled)
			thread = new PlayerThread();
	}
	
	@SubscribeEvent
	public void listBiomes(TagsUpdatedEvent event) {
		biomeregistry = event.getRegistryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
		biomes = biomeregistry.getTagNames().toList();
	}
	

	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		bossDelay += 1;
		if (bossDelay >= 30) {bossPresent = null;}
		if(thread == null)
			return;
		
		if(event.phase == Phase.END) {
			String songs = SongPicker.getSongsString();
			String song = null;
			
			if(songs != null) {
				if(nextSong == null || !songs.contains(nextSong)) {
					do {
						song = SongPicker.getRandomSong();
					} while(song.equals(currentSong) && songs.contains(","));
				} else
					song = nextSong;
			}
			
			if(songs != null && (!songs.equals(PlayerThread.currentSongChoices) || (song == null && PlayerThread.currentSong != null) || !thread.playing)) {
				if(nextSong != null && nextSong.equals(song))
					waitTick--;
				
				if (!song.equals(currentSong)) {
					if (currentSong != null && PlayerThread.currentSong != null && !PlayerThread.currentSong.equals(song) && songs.equals(PlayerThread.currentSongChoices))
						currentSong = PlayerThread.currentSong;
					else
						nextSong = song;
				} else if (nextSong != null && !songs.contains(nextSong))
					nextSong = null;
				
				if(waitTick <= 0) {
					if(PlayerThread.currentSong == null) {
						currentSong = nextSong;
						nextSong = null;
						PlayerThread.currentSongChoices = songs;
						changeSongTo(song);
						fadeOutTicks = 0;
						fadeInTicks = 0;
						waitTick = WAIT_DURATION;
					} else if(fadeOutTicks < FADE_DURATION) {
						thread.setGain(PlayerThread.fadeGains[fadeOutTicks]);
						fadeOutTicks++;
						silenceTicks = 0;
					} else {
						if(silenceTicks < SILENCE_DURATION) {
							silenceTicks++;
						} else {
							nextSong = null;
							PlayerThread.currentSongChoices = songs;
							changeSongTo(song);
							fadeOutTicks = 0;
							fadeInTicks = 0;
							waitTick = WAIT_DURATION;
						}
					}
				}
			} else {
				nextSong = null;
				thread.setGain(PlayerThread.fadeGains[0]);
				silenceTicks = 0;
				fadeOutTicks = 0;
				fadeInTicks = 0;
				waitTick = WAIT_DURATION;
			}
			
			if(thread != null) {
				if (fadeOutTicks > 0) {
					thread.setFadeMult(fadeOutTicks);
				}
				else {
					if (fadeInTicks < 40) {fadeInTicks++;}
					thread.setFadeMult(40 - fadeInTicks);
				}
				thread.setRealGain();
			}
		}
	}
	
	@SubscribeEvent
	public void onRenderOverlay(CustomizeGuiOverlayEvent.DebugText event) {
		if(!Minecraft.getInstance().options.renderDebug)
			return;
		
		event.getRight().add(null);
		if(PlayerThread.currentSong != null) {
			String name = "Now Playing: " + SongPicker.getSongName(PlayerThread.currentSong);
			event.getRight().add(name);
		}
		if(nextSong != null) {
			String name = "Next Song: " + SongPicker.getSongName(nextSong);
			event.getRight().add(name);
		}
	}
	
	@SubscribeEvent
	public void onBackgroundMusic(PlaySoundEvent event) {
		if(SongLoader.enabled && event.getSound().getSource() == SoundSource.MUSIC) {
			if(event.isCancelable())
				event.setCanceled(true);
			
			event.setSound(null);
		}
	}
	
	@SubscribeEvent
	public void CheckForBoss(CustomizeGuiOverlayEvent.BossEventProgress event) {
		String[] bossChars = event.getBossEvent().getName().getContents().toString().split("");
		int idstart = 0;
		int idend = 0;
		String bossName = "";
		for (int f = 0; f < bossChars.length; f++) {
			if (idend == 0) {
			if (bossChars[f].equals("'")) {
				if (idstart == 0) {idstart = f;} else {idend = f;}
			}
			}
		}
		for (int g = idstart + 1; g < idend; g++) {
			bossName = bossName.concat(bossChars[g]);
		}
		bossPresent = bossName;
		bossDelay = 0;
	}
	
	public void changeSongTo(String song) {
		currentSong = song;
		thread.play(song);
	}
	
}

