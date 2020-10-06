package com.aaaaahhhhhhh.zenith.radio.shout;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.DefaultMetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.media.MediaPlayer;
import com.aaaaahhhhhhh.zenith.radio.media.MediaProvider;
import com.aaaaahhhhhhh.zenith.radio.media.MetadataGenerator;
import com.aaaaahhhhhhh.zenith.radio.media.MusicPlayerUpdateCallback;
import com.aaaaahhhhhhh.zenith.radio.media.Playlist;
import com.aaaaahhhhhhh.zenith.radio.media.ShoutMetadata;

public class MusicPlayer {
	public static final int DEFAULT_BUFFER_SIZE = 10;
	
	private final IceMount mount;
	private final MediaPlayer player;
	private final Playlist playlist;
	
	private final ReentrantLock lock = new ReentrantLock();
	private final Set< String > skipFill = new HashSet< String >();
	private final int buffer;

	private int position = -1;
	private int queuePos = -1;
	private MetadataGenerator defGenerator;
	
	private MediaProvider mediaProvider;
	private MediaProvider defaultProvider;
	private MusicPlayerUpdateCallback callback;
	private MetadataGenerator generator;
	
	public MusicPlayer( IceMount mount, MediaProvider defProvider ) {
		this( mount, defProvider, DEFAULT_BUFFER_SIZE );
	}
	
	public MusicPlayer( IceMount mount, MediaProvider defProvider, int buffer ) {
		this.mount = mount;
		this.defaultProvider = defProvider;
		
		if ( buffer < 2 ) {
			throw new IllegalArgumentException( "Buffer size must be at least 2. Recieved " + buffer + " instead!" );
		}
		this.buffer = buffer;
		
		defGenerator = new DefaultMetadataGenerator();
		
		player = new NativePlayer( mount );
		playlist = player.getPlaylist();
		
		player.setCallback( this::onMediaChanged );
	}
	
	public void refill() {
		lock.lock();
		
		// This bit is extra from the previous media provider and needs to be removed
		int extra = playlist.getSize() - ( queuePos + 1 );
		
		// But first, figure out how much actually needs to be added
		// since everything after queuePos needs to be removed,
		// but we only want to add up to the size of the buffer
		int add = Math.max( 0, buffer - ( queuePos + 1 ) );
		
		for ( int i = 0; i < add; i++ ) {
			AudioRecord record = mediaProvider.provide();
			if ( record == null || !record.getFile().exists() ) {
				record = defaultProvider.provide();
			}
			
			if ( record != null ) {
				playlist.add( mediaProvider.provide() );
			}
		}
		
		if ( player.isActive() && position > queuePos ) {
			// Remove the first items in the list
			for ( int i = 0; i < extra; i++ ) {
				playlist.remove( 0 );
			}
			
			// Only play if the playlist isn't empty
			if ( playlist.getSize() > 0 ) {
				// Now change to the new item
				skipFill.add( playlist.getMrl( 0 ) );
				player.play( 0 );
			} else {
				// The playlist shouldn't ever be empty
				// If it is, then stop the player and hope something else fixes the problem
				stop();
			}
		} else {
			// It's not currently playing anything we need to remove
			// so we can remove it safely
			for ( int i = 0; i < extra; i++ ) {
				playlist.remove( queuePos + 1 );
			}
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
		List< AudioRecord > copy = playlist.getQueue();
		lock.unlock();
		
		return copy;
	}
	
	public AudioRecord getCurrentlyPlaying() {
		lock.lock();
		AudioRecord file = playlist.get( position );
		lock.unlock();
		
		return file;
	}
	
	public boolean enqueue( AudioRecord file, boolean forceSkip ) {
		boolean successful = false;
		
		lock.lock();
		if ( position > queuePos ) {
			// The player currently isn't playing any queued items
			queuePos = position + 1;
			successful = playlist.insert( queuePos, file );
			if ( isPlaying() && forceSkip ) {
				player.playNext();
			}
		} else {
			queuePos++;
			successful = playlist.insert( queuePos, file );
		}
		lock.unlock();
		
		return successful;
	}
	
	public MusicPlayer setCallback( MusicPlayerUpdateCallback callback ) {
		// Does this actually need a lock...
		this.callback = callback; 
		return this;
	}
	
	public MusicPlayerUpdateCallback getCallback() {
		return callback;
	}
	
	public MusicPlayer setMetadata( MetadataGenerator generator ) {
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
		// Check if the remaining amount of items is less than buffer
		// playlist size - position < buffer
		// If so, then get the difference and add x amount of items
		// Then, remove up to the currently playing position
		// However, what to do if we queue a list of songs, but then change the radio?
		// Keep an index for the queued songs?
		// Use a queue index for individually added songs
		// Once position > queuePos then we know that we're done playing queued songs
		lock.lock();
		int needToAdd = Math.max( 0, ( buffer - playlist.getSize() ) + position );
		for ( int i = 0; i < needToAdd; i++ ) {
			AudioRecord record = mediaProvider.provide();
			if ( record == null || !record.getFile().exists() ) {
				record = defaultProvider.provide();
			}
			
			if ( record != null ) {
				playlist.add( mediaProvider.provide() );
			}
		}
		for ( int i = 0; i < position; i++ ) {
			// It seems like even though everything is shifted forwards, the list player has its own internal index
			// for keeping track of where it is in the playlist.
			// This means that we can't actually change the position of the element, or we can force set the index
			// But then, that causes another issue where the media position gets changed again
			// So, we need a way to determine if the currently playing one is right or not
			/*
			 * The order is as follows
			 * Play
			 * Skip
			 * CALLED
			 * Play
			 * Remove
			 * Skip
			 * CALLED
			 * Play internal <- Error here
			 * 
			 * Play
			 * Skip
			 * CALLED
			 * Play
			 * Remove
			 * Play 0
			 * CALLED
			 * 
			 * 
			 */
			playlist.remove( 0 );
		}
		queuePos = Math.max( queuePos - position, -1 );
		
		position = 0;
		// Skip this next time the media callback gets changed, and force play position 0
		skipFill.add( playlist.getMrl( 0 ) );
		player.play( 0 );
		
		lock.unlock();
	}
	
	private void onMediaChanged( String newMrl ) {
		// Now, I have no clue if at this point in time, the media player is actually playing something...
		// But so far nothing's broken...
		
		lock.lock();
		boolean requiresFill = !skipFill.remove( newMrl );
		lock.unlock();
		
		if ( requiresFill ) {
			position++;
			fill();
		} else {
			lock.lock();
			AudioRecord file = playlist.get( position );
			lock.unlock();
			
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
				playNext();
			}
		}
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
				if ( i == position && i == queuePos && i == buffer - 1 ) {
					System.out.print( " ≡" );
				} else if ( i == position && i == queuePos ) {
					System.out.print( " =" );
				} else if ( i == position && i == buffer - 1 ) {
					System.out.print( " !" );
				} else if ( i == queuePos && i == buffer - 1 ) {
					System.out.print( " %" );
				} else if ( i == position ) {
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
