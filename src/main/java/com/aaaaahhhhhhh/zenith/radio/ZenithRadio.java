package com.aaaaahhhhhhh.zenith.radio;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.aaaaahhhhhhh.zenith.radio.http.MiniHttpd;
import com.aaaaahhhhhhh.zenith.radio.media.DefaultMetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.media.ImageMetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.shout.IceMount;
import com.aaaaahhhhhhh.zenith.radio.shout.MusicRotatingPlayer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.support.eventmanager.TaskExecutor;

public class ZenithRadio {
	private static File BASE = new File( System.getProperty( "user.dir" ) );
	private static final boolean VERBOSE = true;
	private static Gson GSON = new Gson();
	
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
	private File coverDirectory;
	private IceMount mount;
	private MusicRotatingPlayer player;
	private MusicCache cache;
	private ZenithRadioProperties properties;
	private ImageServerProperties imageProperties;
	private ExternalUpdateProperties externalProperties;
	private PlaylistManager manager;
	
	private RadioMedia mediaManager;
	private RadioControls controls;
	
	private MiniHttpd httpd;
	
	private final TaskExecutor executor = new TaskExecutor();
	
	private boolean isActive = false;
	private final ReentrantLock lock = new ReentrantLock();
	private final List< RadioClient > clients = new ArrayList< RadioClient >();
	
	public ZenithRadio( File homeDir ) {
		homeDirectory = homeDir;
		homeDirectory.mkdirs();
		
		if ( !load() ) {
			throw new IllegalArgumentException( "Failed to load properly!" );
		}
	}
	
	public File getMusicDirectory() {
		return musicDirectory;
	}
	
	public File getHomeDirectory() {
		return homeDirectory;
	}
	
	public File getCoverArtDirectory() {
		return coverDirectory;
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

	public String getUrl() {
		return properties.getProperties().getProperty( "icecast-url", "https://radio.aaaaahhhhhhh.com" );
	}
	
	public ImageServerProperties getImageServerProperties() {
		// TODO Manage this properly
		return imageProperties;
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
	
	private boolean load() {
		lock.lock();
		File configFile = new File( homeDirectory, "config.properties" );
		this.properties = new ZenithRadioProperties( configFile );
		
		if ( properties.isMusicDirectorySet() ) {
			debug( "Music directory detected" );
			musicDirectory = properties.getMusicDirectory();
			
			if ( !musicDirectory.exists() ) {
				debug( "The file "+ musicDirectory + " does not exist!" );
				return false;
			}
			
			if ( !musicDirectory.isDirectory() ) {
				debug( "The file " + musicDirectory + " is not a directory!" );
				return false;
			}
			
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
		
		imageProperties = new ImageServerProperties()
				.setEnabled( "true".equalsIgnoreCase( properties.getProperties().getProperty( "image-server-enabled" ) ) )
				.setBaseUrl( properties.getProperties().getProperty( "image-server-url", "http://localhost" ) )
				.setServerPath( properties.getProperties().getProperty( "image-server-path", "" ) )
				.setSavePath( properties.getProperties().getProperty( "image-save-path", "" ) )
				.setInternalPort( Integer.valueOf( properties.getProperties().getProperty( "image-server-internal-port", "0" ) ) )
				.setExternalPort( Integer.valueOf( properties.getProperties().getProperty( "image-server-external-port", "0" ) ) );
		
		externalProperties = new ExternalUpdateProperties()
				.setEnabled( "true".equalsIgnoreCase( properties.getProperties().getProperty( "external-update-enabled" ) ) )
				.setUrl( properties.getProperties().getProperty( "external-update-url", "http://localhost" ) )
				.setUserpass( properties.getProperties().getProperty( "external-update-keypass", "admin:hackme" ) );
		
		String imagePath = imageProperties.getSavePath();
		if ( imagePath == null || imagePath.isEmpty() ) {
			coverDirectory = new File( homeDirectory, "covers" );
		} else {
			coverDirectory = new File( imagePath );
		}
		coverDirectory.mkdirs();
		
		if ( imageProperties.isEnabled() ) {
			try {
				int port = imageProperties.getInternalPort();
				if ( port > 0 ) {
					try {
						debug( "Starting the image server on port " + port );
						httpd = new MiniHttpd( port, this::serveCoverArt );
						new Thread( httpd::run ).start();
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			} catch  ( Exception e ) {
			}
		}
		
		lock.unlock();
		
		return true;
	}
	
	private void init() {
		lock.lock();
		cache.init();
		
		int size = cache.getRecords().size();
		if ( size == 0 ) {
			throw new IllegalArgumentException( "No tracks have been detected!" );
		} else {
			debug( size + " tracks found!" );
		}
		
		CacheProvider provider = new CacheProvider( cache );
		
		debug( "Setting the IceMount's genre " );
		mount = properties.getIceMount().setGenre( randomSplash() );
		debug( "Creating a new music player" );
		player = new MusicRotatingPlayer( mount, provider ).setCallback( this::playerCallback );
		
		String baseUrl = String.format( "%s:%s/%s/",
				imageProperties.getBaseUrl(),
				imageProperties.getExternalPort(),
				imageProperties.getServerPath()
		);
		player.setMetadata( new ImageMetadataGenerator( new DefaultMetadataGenerator(), baseUrl, coverDirectory ) );
		
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
		// Update the things
		if ( externalProperties.isEnabled() ) {
			JsonObject json = new JsonObject();
			String album = file.getAlbum();
			json.addProperty( "mountpoint", "/" + mount.getMount() );
			json.addProperty( "title", file.getTitle() );
			json.addProperty( "album", album );
			json.addProperty( "artist", file.getArtist() );
			json.addProperty( "disc", file.getDisc() );
			json.addProperty( "track", file.getTrack() );
			if ( album != null && !album.equalsIgnoreCase( "unknown" ) && !album.isEmpty() ) {
				String url = String.format( "%s:%s/%s/%d.png",
						imageProperties.getBaseUrl(),
						imageProperties.getExternalPort(),
						imageProperties.getServerPath(),
						album.hashCode()
				);
				json.addProperty( "cover_art", url );
			}
			json.addProperty( "start_time", System.currentTimeMillis() );
			json.addProperty( "track_length", "/" + getControlsApi().getCurrentLength() );
			
			String jsonStr = GSON.toJson( json );
			
			try {
				URL url = new URL( externalProperties.getUrl() );
				URLConnection con = url.openConnection();

				byte[] bytes = jsonStr.getBytes( StandardCharsets.UTF_8 );
				
				String encoded = Base64.getEncoder().encodeToString( externalProperties.getUserpass().getBytes( "UTF-8" ) );
				con.setRequestProperty( "Authorization", "Basic " + encoded );
				con.setRequestProperty( "Content-Type", "application/json; charset=utf-8" );
				con.setRequestProperty( "Content-Length", String.valueOf( bytes.length ) );
				
				OutputStream stream = con.getOutputStream();
				stream.write( bytes );
				stream.flush();
				stream.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			
		}
		
		for ( RadioClient client : clients ) {
			client.onMediaChange( this, file );
		}
		lock.unlock();
	}
	
	private File serveCoverArt( String path, InetAddress address ) {
		Pattern pattern = Pattern.compile( String.format( "^%s\\/(-?[0-9]+\\.png)$", imageProperties.getServerPath() ) );
		Matcher matcher = pattern.matcher( path );
		if ( matcher.find() ) {
			String file = matcher.group( 1 );
			return new File( coverDirectory, file );
		}
		return null;
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
		
		if ( httpd != null ) {
			debug( "Stopping the image server" );
			httpd.stop();
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
