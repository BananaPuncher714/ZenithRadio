package com.aaaaahhhhhhh.zenith.radio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;
import com.aaaaahhhhhhh.zenith.radio.file.MusicCache;

public class PlaylistManager {
	private final ZenithRadio radio;
	private final Map< String, PlaylistProvider > providers = new ConcurrentHashMap< String, PlaylistProvider >();
	private final CacheProvider defaultProvider;
	private final File directory;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	public PlaylistManager( ZenithRadio radio, CacheProvider defaultProvider ) {
		this.radio = radio;
		this.defaultProvider = defaultProvider;
		
		directory = new File( radio.getHomeDirectory(), "playlists" );
		directory.mkdirs();
		
		lock.lock();
		load();
		lock.unlock();
	}
	
	public CacheProvider getDefaultProvider() {
		return defaultProvider;
	}
	
	protected void onMusicCacheUpdate( MusicCache cache, UpdateCache update ) {
		lock.lock();
		
		// Refresh the default provider
		defaultProvider.refresh();
		
		// For each playlist provider, check if anything was removed
		for ( Iterator< Entry< String, PlaylistProvider > > iterator = providers.entrySet().iterator(); iterator.hasNext(); ) {
			Entry< String, PlaylistProvider > entry = iterator.next();
			PlaylistProvider provider = entry.getValue();
			
			provider.records.removeAll( update.getRemoved() );
			
			// Assume that the playlist provider is empty, or unable to do anything
			if ( !provider.available() ) {
				if ( radio.getPlayer().getProvider() == provider ) {
					radio.getPlayer().setProvider( defaultProvider );
				}
				iterator.remove();
				
				// Delete the file
				File file = new File( directory, entry.getKey() + ".m3u8" );
				file.delete();
			}
		}
		
		if ( !defaultProvider.available() && !radio.getPlayer().getProvider().available() ) {
			System.out.println( "Both providers are unavailable!" );
			radio.getPlayer().stop();
		} else {
			// So, one of the providers is available
			if ( !radio.getPlayer().isPlaying() ) {
				// Start the radio
				System.out.println( "Restarted the radio!" );
				radio.getPlayer().play();
			}
		}
		
		lock.unlock();
		
		/*
		 * So, first things first
		 * When we detect a change in the cache
		 * Refresh the playlists
		 * For the cache provider, simply refresh it.
		 * For other playlist files, check if any of the records were removed
		 * If a playlist is empty, then delete it
		 * If the music player's provider is the same, set it to use the default provider
		 * Check if the default provider is empty, and if so then stop the player and tell the console or someone about it
		 * Not much else then
		 * 
		 * 
		 */
	}
	
	public PlaylistProvider get( String id ) {
		return providers.get( id );
	}
	
	public boolean contains( String id ) {
		return providers.containsKey( id );
	}
	
	public void register( String id, PlaylistProvider playlist ) {
		lock.lock();
		if ( providers.containsKey( id ) ) {
			lock.unlock();
			throw new IllegalArgumentException( "A playlist with the same id already exists!" );
		}

		providers.put( id, playlist );
		
		// TODO save to file
		save( id, playlist );
	
		// TODO call register event
		lock.unlock();
	}
	
	public void unregister( String id ) {
		lock.lock();
		
		// TODO delete it from file
		File file = new File( directory, id + ".m3u8" );
		file.delete();

		// TODO call unregister event
		providers.remove( id );
		
		lock.unlock();
	}
	
	public Set< String > getPlaylistIds() {
		return Collections.unmodifiableSet( providers.keySet() );
	}
	
	private void load() {
		if ( directory.exists() ) {
			Path dirPath = directory.toPath();
			try {
				DirectoryStream< Path > dirStream = Files.newDirectoryStream( dirPath );
				for ( Path subPath : dirStream ) {
					File file = subPath.toFile();
					if ( !file.isDirectory() && file.getName().endsWith( "m3u8" ) ) {
						// Read the file and load them in
						PlaylistProvider provider = new PlaylistProvider();

						try ( Scanner scanner = new Scanner( file ) ){
							while ( scanner.hasNextLine() ) {
								String data = scanner.nextLine();
								if ( !data.startsWith( "#" ) ) {
									File audioFile = new File( data );

									AudioRecord record = radio.getCache().getRecord( audioFile );
									if ( record == null ) {
										record = new AudioRecord( audioFile );
										record.update();
									}

									provider.records.add( record );
								}
							}
						} catch ( FileNotFoundException e ) {
							e.printStackTrace();
						}

						if ( provider.available() ) {
							String id = file.getName().replaceFirst( "\\.m3u8$", "" );
							ZenithRadio.debug( "Registering playlist " + id );
							register( id, provider );
						}
					}
				}
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			
			PlaylistProvider provider = get( "blacklist" );
			if ( provider != null ) {
				defaultProvider.setBlacklist( provider.records );
			}
		}
	}
	
	protected void save() {
		ZenithRadio.debug( "Saving all playlists " );
		
		// Update the default blacklist
		if ( !defaultProvider.isBlacklistEmpty() ) {
			PlaylistProvider blacklist = get( "blacklist" );
			if ( blacklist == null ) {
				blacklist = new PlaylistProvider();
				providers.put( "blacklist", blacklist );
			}
			blacklist.records.clear();
			blacklist.records.addAll( defaultProvider.getBlacklist() );
		}
		
		// Update the rest of the playlists
		for ( Entry< String, PlaylistProvider > entry : providers.entrySet() ) {
			save( entry.getKey(), entry.getValue() );
			ZenithRadio.debug( "Saved playlist " + entry.getKey() );
		}
	}
	
	private void save( String id, PlaylistProvider provider ) {
		File file = new File( directory, id + ".m3u8" );

		try ( FileWriter writer = new FileWriter( file ) ) {
			for ( AudioRecord r : provider.records ) {
				writer.write( r.getFile().getAbsolutePath() + "\n" );
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}
