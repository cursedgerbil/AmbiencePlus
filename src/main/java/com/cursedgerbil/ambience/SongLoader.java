package com.cursedgerbil.ambience;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistries.Keys;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;


public final class SongLoader {

	public static File mainDir;
	public static boolean enabled = false;
	static boolean biomevalid = false;
	
	public static void loadFrom(File f) {
		File config = new File(f, "ambience.properties");
		if(!config.exists())
			initConfig(config); 
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(config));
			enabled = props.getProperty("enabled").equals("true");
			
			if(enabled) {
				SongPicker.reset();
				Set<Object> keys = props.keySet();
				for(Object obj : keys) {
					String s = (String) obj;
					
					String[] tokens = s.split("\\.");
					if(tokens.length < 2)
						continue;

					String keyType = tokens[0];
					if(keyType.equals("event")) {	
						String event = tokens[1];
						if (event.contains("boss") && tokens.length > 2) {
							event = "boss." + joinIdentifyingTokens(tokens);
						}
						
						SongPicker.eventMap.put(event, props.getProperty(s).split(","));
					} else if(keyType.equals("biome")) {
						String biomeName = joinIdentifyingTokens(tokens);
						ResourceLocation biomeResource = new ResourceLocation(tokens[1],biomeName);
						for (int f2 = 0; f2 < tokens.length; f2++) {
							if (tokens[f2].contains("override")) {
							List<String> biomeOverrides = new ArrayList<>();
							String[] potentialOverrides = {"overrideBoss", "overrideHorde","overrideUnderground", "overrideDeepUnderground", "overrideHighUp", "overrideVillage",
									"overrideVillageNight", "overrideUnderwater", "overrideMinecart", "overrideBoat", "overrideHorse", "overridePig", "overrideNight",
									"overrideRain", "overrideDying", "overrideFishing", "overridePumpkinHead"};
							for (int f3 = 0; f3 < potentialOverrides.length; f3++) {
							if (tokens[f2].equals(potentialOverrides[f3])) {
								biomeOverrides.add(potentialOverrides[f3]);
							}
							}
							if (!biomeOverrides.isEmpty()) {
							SongPicker.biomePriorityMap.put(biomeResource, biomeOverrides);
							}
						}
						
						if(biomeResource != null)
							SongPicker.biomeMap.put(biomeResource, props.getProperty(s).split(","));
						}
					} else if(keyType.matches("primarytag|secondarytag")) {
						boolean primary = keyType.equals("primarytag");
						String biomeNameb = joinIdentifyingTokens(tokens);
						biomevalid = false;
						ResourceLocation biomeResourceb = new ResourceLocation(tokens[1],biomeNameb);
						
						if(biomeResourceb != null) {
							if(primary) {
								SongPicker.primaryTagMap.put(biomeResourceb, props.getProperty(s).split(","));
							}
							else SongPicker.secondaryTagMap.put(biomeResourceb, props.getProperty(s).split(","));
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		File musicDir = new File(f, "music");
		if(!musicDir.exists())
			musicDir.mkdir();
			
		mainDir = musicDir;
	}
	
	public static void initConfig(File f) {
		try {
			f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write("# Ambience Config\n");
			writer.write("enabled=false");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static InputStream getStream() {
		if(PlayerThread.currentSong == null || PlayerThread.currentSong.equals("null"))
			return null;
		
		File f = new File(mainDir, PlayerThread.currentSong + ".mp3");
		if(f.getName().equals("null.mp3"))
			return null;
		
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			System.out.println( "File " + f + " not found. Fix your Ambience config!");
			e.printStackTrace();
			return null;
		}
	}
	
	private static String joinTokensExceptFirst(String[] tokens) {
		String s = "";
		int i = 0;
		for(String token : tokens) {
			i++;
			if(i == 1)
				continue;
			s += token;
		}
		return s;
	}
	private static String joinIdentifyingTokens(String[] tokens) {
		String s = "";
		int i = 0;
		for(String token : tokens) {
			i++;
			if(i == 1 || i == 2)
				continue;
			if(token.contains("override")) {
				continue;
			}
			s += token;
		}
		return s;
	}
}
