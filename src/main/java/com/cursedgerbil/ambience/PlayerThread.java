package com.cursedgerbil.ambience;

import java.io.InputStream;

import com.cursedgerbil.ambience.thirdparty.javazoom.jl.player.AudioDevice;
import com.cursedgerbil.ambience.thirdparty.javazoom.jl.player.JavaSoundAudioDevice;
import com.cursedgerbil.ambience.thirdparty.javazoom.jl.player.advanced.AdvancedPlayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;

public class PlayerThread extends Thread {

	public static final float MIN_GAIN = -50F;
	public static final float MAX_GAIN = 0F;
	
	public static float fadeGains[];
	
	static {
		fadeGains = new float[AmbienceMod.FADE_DURATION];
		float totaldiff = MIN_GAIN - MAX_GAIN;
		float diff = totaldiff / fadeGains.length;
		for(int i = 0; i < fadeGains.length; i++) {
			fadeGains[i] = MAX_GAIN + diff * i;
		}
	}
	
	public volatile static float gain = MAX_GAIN;
	public volatile static float realGain = 0;
	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;
	
	AdvancedPlayer player;
	
	volatile boolean queued = false;
	volatile boolean kill = false;
	volatile boolean playing = false;
	
	public PlayerThread() {
		setDaemon(true);
		setName("Ambience Player Thread");
		start();
	}
	
	@Override
	public void run() {
		try {
			while(!kill) {
				if (queued && currentSong != null) {
					if(player != null) {
						resetPlayer();
					}
					InputStream stream = SongLoader.getStream();
					if (stream == null) {continue;}
					player = new AdvancedPlayer(stream);
					queued = false;
				}
				
				boolean played = false;
				if (player != null && player.getAudioDevice() != null && realGain > MIN_GAIN) {
					setGain(fadeGains[0]);
					player.play();
					playing = true;
					played = true;
				}
				
				if (played && !queued) {next();}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void next() {
		if (!currentSongChoices.contains(",") ) {
			play(currentSong);
		}
		else {
			if (SongPicker.getSongsString().equals(currentSongChoices)) {
				String newSong;
				do {
					newSong = SongPicker.getRandomSong();
				} while (newSong.equals(currentSong));
				play(newSong);
			} else {
				play(null);
		}
	}
	}
	
	public void resetPlayer() {
		playing = false;
		if(player != null) {
			player.close();
	}
		currentSong = null;
		player = null;
	}
	
	public void play(String song) {
		resetPlayer();
		
		currentSong = song;
		queued = true;
	}
	
	public float getGain() {
		if(player == null) {
			return gain;
	}
		AudioDevice device = player.getAudioDevice();
		if (device != null && device instanceof JavaSoundAudioDevice) {
			return ((JavaSoundAudioDevice) device).getGain();
		}
		return gain;
	}
	
	public void addGain(float gain) {
		setGain(getGain() + gain);
	}
	
	public void setGain(float gain) {
		this.gain = Math.min(MAX_GAIN, Math.max(MIN_GAIN, gain));
		
		if(player == null) {
			return;
	}
	}
	
	public void setRealGain() {
		Options settings = Minecraft.getInstance().options;
		float musicGain = settings.getSoundSourceVolume(SoundSource.MUSIC) * settings.getSoundSourceVolume(SoundSource.MASTER);
		float realGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * musicGain;
		
		this.realGain = realGain;
		if (player != null) {
			AudioDevice device = player.getAudioDevice();
			if (device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(realGain);
				} catch (IllegalArgumentException e) { }
		}
	}
		if (musicGain == 0) {
			play(null);
		}
		}
		public float getRelativeVolume() {
			return getRelativeVolume(getGain());
		}
		
		public float getRelativeVolume(float gain) {
			float width = MAX_GAIN - MIN_GAIN;
			float rel = Math.abs(gain - MIN_GAIN);
			return rel / Math.abs(width);
		}
		
		public int getFramesPlayed() {
			return player == null ? 0 : player.getFrames();
		}
		
		
		public void forceKill() {
			try {
				resetPlayer();
				interrupt();
				
				finalize();
				kill = true;
			} catch(Throwable e) {
				e.printStackTrace();
		}
	}
	}