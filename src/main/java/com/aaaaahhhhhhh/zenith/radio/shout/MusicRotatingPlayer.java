package com.aaaaahhhhhhh.zenith.radio.shout;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.DefaultMetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.media.MediaProvider;
import com.aaaaahhhhhhh.zenith.radio.media.MetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.media.MusicPlayerUpdateCallback;
import com.aaaaahhhhhhh.zenith.radio.media.ShoutMetadata;

public class MusicRotatingPlayer {
	public static final int DEFAULT_BUFFER_SIZE = 10;
	
	private final IceMount mount;
	private final NativeRotatingPlayer player;
	private final NativeRotatingPlaylist playlist;
	
	private final ReentrantLock lock = new ReentrantLock();
	private final int buffer;

	private int queuePos = -1;
	private MetadataGenerator defGenerator;
	
	private MediaProvider mediaProvider;
	private MediaProvider defaultProvider;
	private MusicPlayerUpdateCallback callback;
	private MetadataGenerator generator;
	
	public MusicRotatingPlayer( IceMount mount, MediaProvider defProvider ) {
		this( mount, defProvider, DEFAULT_BUFFER_SIZE );
	}
	
	public MusicRotatingPlayer( IceMount mount, MediaProvider defProvider, int buffer ) {
		this.mount = mount;
		this.defaultProvider = defProvider;
		
		if ( buffer < 2 ) {
			throw new IllegalArgumentException( "Buffer size must be at least 2. Recieved " + buffer + " instead!" );
		}
		this.buffer = buffer;
		
		defGenerator = new DefaultMetadataGenerator();
		
		player = new NativeRotatingPlayer( mount );
		playlist = player.getPlaylist();
		
		player.setCallback( this::onMediaChanged );
	}
	
	public void refill() {
		lock.lock();
		
		// Check if there is are any enqueued tracks
		// Do not want to remove those
		// Otherwise, refill and clear
		playlist.lock();
		// Get the size
		int sizeBefore = playlist.getSize();
		int toRemove = sizeBefore - ( queuePos + 1 );
		int toAdd = buffer - ( queuePos + 1 );
		
		for ( int i = 0; i < toAdd; i++ ) {
			AudioRecord record = mediaProvider.provide();
			if ( record == null || !record.getFile().exists() ) {
				record = defaultProvider.provide();
			}
			
			if ( record != null ) {
				playlist.add( record );
			}
		}
		
		for ( int i = 0; i < toRemove; i++ ) {
			playlist.remove( queuePos + 1 );
		}
		
		playlist.unlock();
		
		// If there's nothing queued
		if ( player.isPlaying() && queuePos == -1 ) {
			player.play( 0 );
		}
		
		lock.unlock();
	}
	
	public void setProvider( MediaProvider mediaProvider ) {
		if ( this.mediaProvider != mediaProvider ) {
			this.mediaProvider = mediaProvider;
			
			refill();
		}
	}
	
	public MediaProvider getProvider() {
		return mediaProvider;
	}
	
	public boolean play() {
		boolean successful = false;
		lock.lock();
		if ( playlist.getSize() > 1 ) {
			if ( !player.isPlaying() ) {
				successful = true;
				
				player.play();
			}
		}
		lock.unlock();
		
		return successful;
	}
	
	public boolean stop() {
		boolean successful = false;
		lock.lock();
		if ( player.isActive() ) {
			successful = true;
			player.stop();
		}
		lock.unlock();
		
		return successful;
	}
	
	public boolean isPlaying() {
		lock.lock();
		boolean successful = player.isPlaying();
		lock.unlock();
		
		return successful;
	}
	
	public boolean seek( long ms ) {
		boolean successful = false;
		
		lock.lock();
		successful = player.seek( ms );
		lock.unlock();
		
		return successful;
	}
	
	public long getCurrentTime() {
		long time = -1;
		lock.lock();
		if ( player.isActive() ) {
			time = player.currentTime();
		}
		lock.unlock();
		
		return time;
	}
	
	public long getCurrentLength() {
		long time = -1;
		lock.lock();
		if ( player.isActive() ) {
			time = player.currentLength();
		}
		lock.unlock();
		
		return time;
	}
	
	public int getBufferSize() {
		return buffer;
	}
	
	public boolean playNext() {
		boolean successful = false;
		
		lock.lock();
		successful = player.playNext();
		lock.unlock();
		
		return successful;
	}
	
	public List< AudioRecord > getPlaylist() {
		lock.lock();
		playlist.lock();
		List< AudioRecord > copy = playlist.getQueue();
		playlist.unlock();
		lock.unlock();
		
		return copy;
	}
	
	public AudioRecord getCurrentlyPlaying() {
		lock.lock();
		playlist.lock();
		AudioRecord file = playlist.get( 0 );
		playlist.unlock();
		lock.unlock();
		
		return file;
	}
	
	public boolean enqueue( AudioRecord file, boolean forceSkip ) {
		boolean successful = false;
		
		lock.lock();
		// If there's nothing in the queue and the player is currently active,
		// then we need to place the item after the 0th index to make
		// sure that it gets played next.
		if ( queuePos == -1 && player.isActive() ) {
			queuePos = 0;
		}
		queuePos++;
		
		// Insert the item
		playlist.lock();
		successful = playlist.insert( queuePos, file );
		playlist.fill();
		playlist.unlock();
		
		if ( isPlaying() && forceSkip ) {
			player.playNext();
		}
		lock.unlock();
		
		return successful;
	}
	
	public MusicRotatingPlayer setCallback( MusicPlayerUpdateCallback callback ) {
		// Does this actually need a lock...
		this.callback = callback; 
		return this;
	}
	
	public MusicPlayerUpdateCallback getCallback() {
		return callback;
	}
	
	public MusicRotatingPlayer setMetadata( MetadataGenerator generator ) {
		this.generator = generator;
		return this;
	}
	
	public MetadataGenerator getMetadata() {
		return generator;
	}
	
	public void release() {
		player.release();
	}
	
	private void fill() {
		lock.lock();
		
		playlist.lock();
		int needToAdd = Math.max( 0, ( buffer - playlist.getSize() ) );
		for ( int i = 0; i < needToAdd; i++ ) {
			AudioRecord record = mediaProvider.provide();
			if ( record == null || !record.getFile().exists() ) {
				record = defaultProvider.provide();
			}

			if ( record != null ) {
				playlist.add( record );
			}
		}
		playlist.unlock();
		
		queuePos = Math.max( queuePos - 1, -1 );
			
		lock.unlock();
	}
	
	private void onMediaChanged( String newMrl ) {
		lock.lock();
		
		fill();
		
		playlist.lock();
		AudioRecord file = playlist.get( 0 );
		playlist.unlock();

		if ( file.getFile().exists() ) {
			// Callback before updating the metadata
			if ( callback != null ) {
				callback.onMusicPlayerUpdateEvent( file );
			}

			ShoutMetadata metadata;
			if ( generator != null ) {
				metadata = generator.getMetadataFor( file );
			} else {
				metadata = defGenerator.getMetadataFor( file );
			}
			updateMetadata( mount, metadata );
		} else {
			// The file doesn't exist, so skip it
			System.out.println( "Missing file!! " + file.getFile().getAbsolutePath() );
			playNext();
		}
		lock.unlock();
	}
	
	private static boolean updateMetadata( IceMount mount, ShoutMetadata metadata ) {
		return updateMetadata( mount, metadata.getArtist(), metadata.getTitle() );
	}
	
	private static boolean updateMetadata( IceMount mount, String artist, String title ) {
		String strUrl = "http://%s:%d/admin/metadata?mode=updinfo&mount=/%s&song=%s";
		try {
			strUrl = String.format( strUrl,
					mount.getServerAddress(),
					mount.getPort(),
					mount.getMount(),
					URLEncoder.encode( artist.replace( " - ", " – " ) + " - " + title, "UTF-8" ) );
			
			URL url = new URL( strUrl );
			URLConnection con = url.openConnection();
			
			String encode = mount.getUsername() + ":" + mount.getPasswd();
			String encoded = Base64.getEncoder().encodeToString( encode.getBytes( "UTF-8" ) );
			con.setRequestProperty( "Authorization", "Basic " + encoded );
			
			con.getInputStream();
			
			return true;
		} catch ( IOException exception ) {
			exception.printStackTrace();
			return false;
		}
	}
	
	// Debug
	public void printPlaylist() {
		if ( playlist == null ) {
			System.out.println( "No playlist!" );
		} else {
			for ( int i = 0; i < playlist.getSize(); i++ ) {
				if ( i == 0 && i == queuePos && i == buffer - 1 ) {
					System.out.print( " ≡" );
				} else if ( i == 0 && i == queuePos ) {
					System.out.print( " =" );
				} else if ( i == 0 && i == buffer - 1 ) {
					System.out.print( " !" );
				} else if ( i == queuePos && i == buffer - 1 ) {
					System.out.print( " %" );
				} else if ( i == 0 ) {
					System.out.print( " -" );
				} else if ( i == queuePos ) {
					System.out.print( " >" );
				} else if ( i == buffer - 1 ) {
					System.out.print( " ^" );
				}
				System.out.println( "\t" + i + "\t" + playlist.get( i ).getFile().getName() );
			}
		}
	}
}
