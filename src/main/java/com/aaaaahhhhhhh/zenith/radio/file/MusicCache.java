package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;

public class MusicCache {
	private final File baseDir;
	
	protected final DirectoryRecord record;
	
	private Map< String, AudioRecord > files = new ConcurrentHashMap< String, AudioRecord >();
	
	private MusicCacheUpdateCallback callback;
	
	private Thread watcher;
	private WatchService service;
	private boolean closed = false;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	public MusicCache( File dir, FileValidator validator ) { 
		baseDir = dir;
		baseDir.mkdirs();

		record = new DirectoryRecord( baseDir, validator );
	}
	
	public MusicCache( DirectoryRecord record ) {
		this.record = record;
		
		baseDir = record.getFile();
		baseDir.mkdirs();
	}
	
	public void init() {
		Path path = baseDir.toPath();
		FileSystem fs = path.getFileSystem();
		
		try {
			ZenithRadio.debug( "Registering new watcher service" );
			service = fs.newWatchService();
			path.register( service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY );
		} catch ( IOException e ) {
			// I should probably do something with this...
			e.printStackTrace();
		}
		
		ZenithRadio.debug( "Updating the directory record" );
		boolean unchanged = record.update();
		if ( unchanged ) {
			ZenithRadio.debug( "No changes detected" );
		} else {
			ZenithRadio.debug( "Changes detected" );
		}
		
		ZenithRadio.debug( "Updating cached audio records" );
		updateRecords();
		
		ZenithRadio.debug( "Starting the watcher service" );
		watcher = new Thread( this::watch );
		watcher.start();
	}
	
	public File getBaseDirectory() {
		return record.getFile();
	}
	
	public MusicCache setCallback( MusicCacheUpdateCallback callback ) {
		this.callback = callback;
		return this;
	}
	
	public MusicCacheUpdateCallback getCallback() {
		return callback;
	}
	
	private void watch() {
		try {
			while ( true ) {
				WatchKey key = service.take();
				if ( !key.pollEvents().isEmpty() ) {
					ZenithRadio.debug( "Detected a change in the music cache" );
					Thread.sleep( 100 );
					
					lock.lock();
					UpdateCache cache = record.updateDirectory();
					lock.unlock();

					if ( !cache.isUnchanged() ) {
						ZenithRadio.debug( "Updating cached records" );
						
						updateRecords();
						
						if ( callback != null ) {
							callback.onMusicCacheUpdate( this, cache );
						}
					}
				}
				key.reset();
			}
		} catch ( ClosedWatchServiceException | InterruptedException e ) {
			// Shutting down the watch service... Add debug calls or something
//			e.printStackTrace();
		}
	}
	
	private void updateRecords() {
		lock.lock();
		files = new ConcurrentHashMap< String, AudioRecord >();
		for ( AudioRecord r : record.getAllAudioRecords() ) {
			files.put( r.getFile().getAbsolutePath(), r );
		}
		lock.unlock();
	}
	
	public UpdateCache forceUpdate() {
		lock.lock();
		UpdateCache cache = record.updateDirectory();
		updateRecords();
		lock.unlock();
		return cache;
	}
	
	public AudioRecord getRecord( File file ) {
		return files.get( file.getAbsolutePath() );
	}
	
	public Collection< AudioRecord > getRecords() {
		return Collections.unmodifiableCollection( files.values() );
	}
	
	public boolean isStopped() {
		return closed;
	}
	
	public void stop() {
		try {
			service.close();
			closed = true;
		} catch ( IOException e ) {
			// Should probably do something with this too...
			e.printStackTrace();
		}
	}
}
