package com.aaaaahhhhhhh.zenith.radio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.client.ConsoleClient;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.client.RadioClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioFileValidator;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.file.CompoundFileValidator;
import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;
import com.aaaaahhhhhhh.zenith.radio.file.FileValidator;
import com.aaaaahhhhhhh.zenith.radio.file.MusicCache;
import com.aaaaahhhhhhh.zenith.radio.file.MusicCacheIO;
import com.aaaaahhhhhhh.zenith.radio.shout.IceMount;
import com.aaaaahhhhhhh.zenith.radio.shout.MusicRotatingPlayer;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.support.eventmanager.TaskExecutor;

public class ZenithRadio {
	private static File BASE = new File( System.getProperty( "user.dir" ) );
	private static final boolean VERBOSE = true;
	
	static {
		System.setProperty( "jna.encoding", "UTF8" );
	}
	
	public static void main( String[] args ) {
		ZenithRadio radio = new ZenithRadio( BASE );
		if ( radio.getMusicDirectory() == null ) {
			System.out.println( "The music directory is not set!" );
			System.exit( 1 );
		} else if ( !radio.getMusicDirectory().exists() ) {
			System.out.println( "The music directory does not exist!" );
			System.exit( 2 );
		}

		System.out.println( "Looking for a VLC installation..." );
		MediaPlayerFactory factory = new MediaPlayerFactory();
		factory.release();
		
		System.out.println( "Starting Zenith Radio..." );
		radio.init();
		
		ConsoleClient client = new ConsoleClient( System.in );
		radio.attach( client );
		DiscordClient discordClient = new DiscordClient( new File( BASE, "clients/discord" ) );
		radio.attach( discordClient );
		
		radio.start();
		
		while ( radio.isActive() ) {
			try {
				Thread.sleep( 1000 );
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
		
		debug( "Stopping Zenith Radio" );
		
		client.stop();
		discordClient.stop();
	}
	
	public static String timeFormat( long time ) {
		return String.format( "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes( time ), TimeUnit.MILLISECONDS.toSeconds( time ) % 60 );
	}
	
	// TODO Use a java logger
	
	private final File homeDirectory;
	private File musicDirectory;
	private IceMount mount;
	private MusicRotatingPlayer player;
	private MusicCache cache;
	private ZenithRadioProperties properties;
	private PlaylistManager manager;
	
	private RadioMedia mediaManager;
	private RadioControls controls;
	
	private final TaskExecutor executor = new TaskExecutor();
	
	private boolean isActive = false;
	private final ReentrantLock lock = new ReentrantLock();
	private final List< RadioClient > clients = new ArrayList< RadioClient >();
	
	public ZenithRadio( File homeDir ) {
		homeDirectory = homeDir;
		homeDirectory.mkdirs();
		
		load();
	}
	
	public File getMusicDirectory() {
		return musicDirectory;
	}
	
	public File getHomeDirectory() {
		return homeDirectory;
	}
	
	public RadioMedia getMediaApi() {
		return mediaManager;
	}
	
	public RadioControls getControlsApi() {
		return controls;
	}
	
	public PlaylistManager getPlaylistManager() {
		return manager;
	}
	
	protected MusicRotatingPlayer getPlayer() {
		return player;
	}
	
	protected MusicCache getCache() {
		return cache;
	}
	
	protected ReentrantLock getLock() {
		return lock;
	}
	
	private void load() {
		lock.lock();
		File configFile = new File( homeDirectory, "config.properties" );
		this.properties = new ZenithRadioProperties( configFile );
		
		if ( properties.isMusicDirectorySet() ) {
			debug( "Music directory detected" );
			musicDirectory = properties.getMusicDirectory();
			
			FileValidator validator;
			String exclude = properties.getProperties().getProperty( "exclusion-filters" );
			if ( exclude == null || exclude.isEmpty() ) {
				debug( "Not using any exclusion rule" );
				validator = new AudioFileValidator();
			} else {
				debug( "Using exclusion rule '" + exclude + "'" );
				validator = new CompoundFileValidator( exclude.split( "," ) );
			}
			
			File cachedData = new File( homeDirectory, "cache.dat" );
			if ( cachedData.exists() ) {
				debug( "Music cache file found" );
				try {
					cache = MusicCacheIO.read( cachedData, validator );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
				
				if ( !cache.getBaseDirectory().getAbsolutePath().equals( musicDirectory.getAbsolutePath() ) ) {
					debug( "Directory mismatch! Creating a new music cache" );
					cache = new MusicCache( musicDirectory, validator );
				}
			} else {
				debug( "No music cache file found" );
				debug( "Creating a new music cache" );
				cache = new MusicCache( musicDirectory, validator );
			}
		} else {
			debug( "No music directory set!" );
		}
		
		controls = new RadioControls( this );
		mediaManager = new RadioMedia( this );
		
		lock.unlock();
	}
	
	private void init() {
		lock.lock();
		cache.init();
		CacheProvider provider = new CacheProvider( cache );
		
		debug( "Setting the IceMount's genre " );
		mount = properties.getIceMount().setGenre( randomSplash() );
		debug( "Creating a new music player" );
		player = new MusicRotatingPlayer( mount, provider ).setCallback( this::playerCallback );
		
		// Set the current provider for the radio
		player.setProvider( provider );
		
		manager = new PlaylistManager( this, provider );
		
		// Hopefully the music directory doesn't somehow get modified between the creation of the cache and registering the callback
		cache.setCallback( this::cacheCallback );
		
		isActive = true;
		lock.unlock();
	}
	
	private void start() {
		// Start the player
		// This should go after all the clients get attached
		debug( "Starting the music player" );
		lock.lock();
		player.play();
		lock.unlock();
	}
	
	private void cacheCallback( MusicCache cache, UpdateCache update ) {
		// Update playlists here
		manager.onMusicCacheUpdate( cache, update );
	}
	
	private void playerCallback( AudioRecord file ) {
		lock.lock();
		for ( RadioClient client : clients ) {
			client.onMediaChange( this, file );
		}
		lock.unlock();
	}
	
	public void submit( Runnable runnable ) {
		executor.submit( runnable );
	}
	
	public void attach( RadioClient client ) {
		client.onAttach( this );
		lock.lock();
		clients.add( client );
		lock.unlock();
	}
	
	public void detach( RadioClient client ) {
		client.onDetach( this );
		lock.lock();
		clients.remove( client );
		lock.unlock();
	}
	
	public boolean stop() {
		boolean success = false;
		lock.lock();
		if ( isActive ) {
			for ( RadioClient client : clients ) {
				client.onDetach( this );
			}
			clients.clear();
			
			isActive = false;
			
			executor.release();
			
			if ( player != null ) {
				debug( "Stopping the music player" );
				player.stop();
				debug( "Releasing the music player" );
				player.release();
			}
			if ( cache != null ) {
				debug( "Stopping the cache" );
				cache.stop();
				try {
					debug( "Saving music cache to file" );
					MusicCacheIO.write( cache, new File( homeDirectory, "cache.dat" ) );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
			if ( manager != null ) {
				debug( "Saving playlists" );
				manager.save();
			}
	
			debug( "Saving properties" );
			properties.save();
			success = true;
		}
		lock.unlock();
		
		return success;
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void debugPrintPlaylist() {
		player.printPlaylist();
	}
	
	public static void debug( String message ) {
		if ( VERBOSE ) {
			System.out.println( "[" + Thread.currentThread().getName() + "] " + message );
		}
	}
	
	private static final String[] SPLASH = {
			"Now with 90% more Drama CDs!",
			"FOOL! You thought this was a REAL description!?",
			"Want to become a Zenith Radio Public Trading Company Shareholder? TOO BARD!",
			"We've been trying to reach you regarding your car's extended warranty...",
			"Download Raid: Shadow Legends, for free on PC!",
			"Something something something..."
		};
	
	private static String randomSplash() {
		return SPLASH[ ThreadLocalRandom.current().nextInt( SPLASH.length ) ];
	}
}
