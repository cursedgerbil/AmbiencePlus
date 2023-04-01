package com.cursedgerbil.ambience;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.Tags.Biomes;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.tags.ITagManager;

public final class SongPicker {

	public static final String EVENT_MAIN_MENU = "mainMenu";
	public static final String EVENT_BOSS = "boss";
	public static final String EVENT_BOSS_WITHER = "bossWither";
	public static final String EVENT_BOSS_DRAGON = "bossDragon";
	public static final String EVENT_IN_NETHER = "nether";
	public static final String EVENT_IN_END = "end";
	public static final String EVENT_HORDE = "horde";
	public static final String EVENT_NIGHT = "night";
	public static final String EVENT_RAIN = "rain";
	public static final String EVENT_UNDERWATER = "underwater";
	public static final String EVENT_UNDERGROUND = "underground";
	public static final String EVENT_DEEP_UNDEGROUND = "deepUnderground";
	public static final String EVENT_HIGH_UP = "highUp";
	public static final String EVENT_VILLAGE = "village";
	public static final String EVENT_VILLAGE_NIGHT = "villageNight";
	public static final String EVENT_MINECART = "minecart";
	public static final String EVENT_BOAT = "boat";
	public static final String EVENT_HORSE = "horse";
	public static final String EVENT_PIG = "pig";
	public static final String EVENT_FISHING = "fishing";
	public static final String EVENT_DYING = "dying";
	public static final String EVENT_PUMPKIN_HEAD = "pumpkinHead";
	public static final String EVENT_CREDITS = "credits";
	public static final String EVENT_GENERIC = "generic";

	public static final Map<String, String[]> eventMap = new HashMap();
	public static final Map<ResourceLocation, String[]> biomeMap = new HashMap();
	public static final Map<ResourceLocation, String[]> primaryTagMap = new HashMap();
	public static final Map<ResourceLocation, String[]> secondaryTagMap = new HashMap();
	public static final Map<ResourceLocation, List<String>> biomePriorityMap = new HashMap();
	static ResourceLocation biome;
	static ResourceLocation biomereturn = null;
	static boolean secondaryreturn = false;

	public static final Random rand = new Random();

	public static void reset() {
		eventMap.clear();
		biomeMap.clear();
		primaryTagMap.clear();
		secondaryTagMap.clear();
	}

	public static String[] getSongs() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Level world = mc.level;

		if (player == null) {
			return getSongsForEvent(EVENT_MAIN_MENU);
		}

		if (mc.screen instanceof WinScreen) {
			return getSongsForEvent(EVENT_CREDITS);
		}

		BlockPos pos = new BlockPos(player.getX(), player.getY(), player.getZ());
		ResourceLocation biomeid = world.getBiomeManager().getBiome(pos).unwrapKey().get().location();

		AmbienceEventEvent event = new AmbienceEventEvent.Pre(world, pos);
		MinecraftForge.EVENT_BUS.post(event);
		String[] eventr = getSongsForEvent(event.event);
		if (eventr != null)
			return eventr;

		BossHealthOverlay bossOverlay = new BossHealthOverlay(mc);
		if (AmbienceMod.bossPresent != null) {
			
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideBoss")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}

			String[] songs = getSongsForEvent(EVENT_BOSS);
			if (songs != null)
				return songs;
		}

		float hp = player.getHealth();
		if (hp < 7) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideDying")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_DYING);
			if (songs != null)
				return songs;
		}

		int monsterCount = world.getEntitiesOfClass(Monster.class, new AABB(player.getX() - 16, player.getY() - 8,
				player.getZ() - 16, player.getX() + 16, player.getY() + 8, player.getZ() + 16)).size();
		if (monsterCount > 5) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideHorde")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_HORDE);
			if (songs != null)
				return songs;
		}

		if (player.fishing != null) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideFishing")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_FISHING);
			if (songs != null)
				return songs;
		}

		ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
		if (headItem != null && headItem.getItem() == Item.byBlock(Blocks.CARVED_PUMPKIN)) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overridePumpkin")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_PUMPKIN_HEAD);
			if (songs != null)
				return songs;
		}

		if (world.dimension() == Level.NETHER) {
			String[] songs = getSongsForEvent(EVENT_IN_NETHER);
			if (songs != null)
				return songs;
		} else if (world.dimension() == Level.END) {
			String[] songs = getSongsForEvent(EVENT_IN_END);
			if (songs != null)
				return songs;
		}

		Entity riding = player.getVehicle();
		if (riding != null) {
			if (riding instanceof Minecart) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideMinecart")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_MINECART);
				if (songs != null)
					return songs;
			}
			if (riding instanceof Boat) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideBoat")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_BOAT);
				if (songs != null)
					return songs;
			}
			if (riding instanceof Horse) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideHorse")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_HORSE);
				if (songs != null)
					return songs;
			}
			if (riding instanceof Pig) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overridePig")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_PIG);
				if (songs != null)
					return songs;
			}
		}

		if (player.isUnderWater()) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideUnderwater")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_UNDERWATER);
			if (songs != null)
				return songs;
		}

		long time = world.getDayTime() % 24000;
		boolean night = time > 13300 && time < 23200;

		boolean underground = !world.canSeeSky(pos);

		if (underground) {
			ResourceLocation biomeoverride = world.getBiomeManager().getBiome(pos).unwrapKey().get().location();
			ResourceLocation deepdark = new ResourceLocation("deep_dark");
			if (pos.getY() < -15) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideDeepUnderground")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_DEEP_UNDEGROUND);
				if (songs != null)
					return songs;
			}
			if (pos.getY() < 55) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideUnderground")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_UNDERGROUND);
				if (songs != null)
					return songs;
			}
		} else if (world.isRaining()) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideRain")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_RAIN);
			if (songs != null)
				return songs;
		}

		if (pos.getY() > 128) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideHighUp")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_HIGH_UP);
			if (songs != null)
				return songs;
		}

		if (night) {
			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideNight")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_NIGHT);
			if (songs != null)
				return songs;

		}

		int villagerCount = world.getEntitiesOfClass(Villager.class, new AABB(player.getX() - 30, player.getY() - 8,
				player.getZ() - 30, player.getX() + 30, player.getY() + 8, player.getZ() + 30)).size();
		if (villagerCount > 3) {
			if (night) {
				if (biomePriorityMap.containsKey(biomeid)) {
					List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
					if (currentbiomePriorities.contains("overrideVillageNight")) {
						if (biomeMap.containsKey(biome))
							return biomeMap.get(biome);
					}
				}
				String[] songs = getSongsForEvent(EVENT_VILLAGE_NIGHT);
				if (songs != null)
					return songs;
			}

			if (biomePriorityMap.containsKey(biomeid)) {
				List<String> currentbiomePriorities = biomePriorityMap.get(biomeid);
				if (currentbiomePriorities.contains("overrideVillage")) {
					if (biomeMap.containsKey(biome))
						return biomeMap.get(biome);
				}
			}
			String[] songs = getSongsForEvent(EVENT_VILLAGE);
			if (songs != null)
				return songs;
		}

		event = new AmbienceEventEvent.Post(world, pos);
		MinecraftForge.EVENT_BUS.post(event);
		eventr = getSongsForEvent(event.event);
		if (eventr != null)
			return eventr;

		if (world != null) {
			String[] biomecheck = checkBiomeSong(biomeMap, primaryTagMap, secondaryTagMap);
			if (biomecheck != null) {return biomecheck;}
		}

		return getSongsForEvent(EVENT_GENERIC);
	}

	public static String getSongsString() {
		return StringUtils.join(getSongs(), ",");
	}

	public static String getRandomSong() {
		String[] songChoices = getSongs();

		return songChoices[rand.nextInt(songChoices.length)];
	}

	public static String[] getSongsForEvent(String event) {
		if (eventMap.containsKey(event))
			return eventMap.get(event);

		return null;
	}

	public static String getSongName(String song) {
		return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
	}
	
	public static String[] checkBiomeSong(Map<ResourceLocation, String[]> biomeMap, Map<ResourceLocation, String[]> primaryMap, Map<ResourceLocation, String[]> secondaryMap) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Level world = mc.level;

		BlockPos pos = new BlockPos(player.getX(), player.getY(), player.getZ());
		biome = world.getBiomeManager().getBiome(pos).unwrapKey().get().location();
		if (biomeMap.containsKey(biome))
			return biomeMap.get(biome);

		biomereturn = null;
		secondaryreturn = false;
		if (AmbienceMod.biomes != null) {
			
			for (int f = 0; f < AmbienceMod.biomes.size(); f++) {
				TagKey<Biome> currentbiometag = AmbienceMod.biomes.get(f);
				if (world.getBiomeManager().getBiome(pos).is(currentbiometag)) {
					if (biomereturn == null) {
						if (primaryTagMap.containsKey(currentbiometag.location())) {
							biomereturn = currentbiometag.location();
							secondaryreturn = false;
						}
						else if (secondaryTagMap.containsKey(currentbiometag.location())) {
							biomereturn = currentbiometag.location();
							secondaryreturn = true;
						}
					}
				}
			}

		if (biomereturn != null) {
			//i spent 2 hours here trying to fix this part not working
			//and then realised i had put a = instead of an ==
			//if you want some gasoline and a match sorry i took them
			if (secondaryreturn == true) {
				return secondaryTagMap.get(biomereturn);
			} else if (secondaryreturn == false) {
				return primaryTagMap.get(biomereturn);
			}
	}
		}
			return null;
	}
}