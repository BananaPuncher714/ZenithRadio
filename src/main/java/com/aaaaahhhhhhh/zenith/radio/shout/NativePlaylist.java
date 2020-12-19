package com.aaaaahhhhhhh.zenith.radio.shout;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_add_option;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_get_mrl;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_add_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_count;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_insert_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_item_at_index;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_lock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_remove_index;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_retain;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_unlock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_location;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_path;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_release;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.Playlist;

import uk.co.caprica.vlcj.binding.NativeString;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;

public class NativePlaylist implements Playlist {
	private final List< AudioRecord > queue = new ArrayList< AudioRecord >();
	private final libvlc_instance_t libvlcInstance;
	private final libvlc_media_list_t playlist;
	private final String mediaOption;
	
	// Lock for the queue
	private final ReentrantLock lock = new ReentrantLock();
	
	public NativePlaylist( libvlc_instance_t libvlcInstance, String option ) {
		this.mediaOption = option;
		this.libvlcInstance = libvlcInstance;
		
		// Create a new MediaList
		playlist = libvlc_media_list_new( libvlcInstance );
		// No clue what this does, it just looks important
		libvlc_media_list_retain( playlist );
	}
	
	@Override
	public boolean remove( int index ) {
		lock.lock();
		queue.remove( index );
		
		libvlc_media_list_lock( playlist );
		int nativeSize = libvlc_media_list_count( playlist );
		boolean successful = false;
		if ( nativeSize > 0 ) {
			successful = libvlc_media_list_remove_index( playlist, index ) == 0;
		}
		libvlc_media_list_unlock( playlist );
		
		lock.unlock();

		if ( nativeSize == 0 ) {
			throw new RuntimeException( "Tried to remove from an empty playlist!" );
		}
		
		return successful;
	}
	
	@Override
	public boolean insert( int index, AudioRecord file ) {
		lock.lock();
		String path = file.getFile().getAbsolutePath();
		
		queue.add( index, file );
		libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );

		libvlc_media_add_option( media, mediaOption );

		// Add the media
		libvlc_media_list_lock( playlist );
		boolean successful = libvlc_media_list_insert_media( playlist, media, index ) == 0;
		libvlc_media_list_unlock( playlist );
		
		// Release it
		libvlc_media_release( media );
		
		lock.unlock();
		
		return successful;
	}
	
	@Override
	public boolean add( AudioRecord file ) {
		lock.lock();
		String path = file.getFile().getAbsolutePath();
		
		queue.add( file );
		libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );

		libvlc_media_add_option( media, mediaOption );

		// Add the media
		libvlc_media_list_lock( playlist );
		boolean successful = libvlc_media_list_add_media( playlist, media ) == 0;
		libvlc_media_list_unlock( playlist );
		
		// Release it
		libvlc_media_release( media );
	
		lock.unlock();
		
		return successful;
	}
	
	@Override
	public AudioRecord get( int index ) {
		lock.lock();
		AudioRecord file = queue.get( index );
		lock.unlock();
		
		return file;
	}
	
	@Override
	public String getMrl( int index ) {
		lock.lock();
		libvlc_media_list_lock( playlist );
		libvlc_media_t item = libvlc_media_list_item_at_index( playlist, index );
		String mrl = NativeString.copyNativeString( libvlc_media_get_mrl( item ) );
		libvlc_media_list_unlock( playlist );
		
		// Release it
		libvlc_media_release( item );
		
		lock.unlock();
		
		return mrl;
	}
	
	@Override
	public int getSize() {
		lock.lock();
		libvlc_media_list_lock( playlist );
		int nativeSize = libvlc_media_list_count( playlist );
		libvlc_media_list_unlock( playlist );
		int size = queue.size();
		lock.unlock();
		
		if ( nativeSize != size ) {
			throw new RuntimeException( String.format( "Sizes no longer in sync! Expected %u, got %u", size, nativeSize ) );
		}
		
		return size;
	}
	
	@Override
	public List< AudioRecord > getQueue() {
		lock.lock();
		List< AudioRecord > copy = Collections.unmodifiableList( queue );
		lock.unlock();
		
		return copy;
	}
	
	private void debugOut() {
		libvlc_media_list_lock( playlist );
		int size = libvlc_media_list_count( playlist );
		System.out.println( "Current size: " + size + " vs queue " + queue.size() );
        for (int i = 0; i < size; i++) {
            libvlc_media_t item = libvlc_media_list_item_at_index( playlist, i );
            System.out.println( "Item is " + NativeString.copyNativeString( libvlc_media_get_mrl( item ) ) );
            libvlc_media_release(item);
        }
        libvlc_media_list_unlock( playlist );
	}
	
	public libvlc_media_list_t getNativeMediaList() {
		return playlist;
	}
	
	@Override
	public void release() {
		libvlc_media_list_release( playlist );
	}
}
