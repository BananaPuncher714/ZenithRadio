package com.aaaaahhhhhhh.zenith.radio.player;

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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.Playlist;

import uk.co.caprica.vlcj.binding.NativeString;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;

public class NativeRotatingPlaylist implements Playlist {
	private final List< AudioRecord > queue = new LinkedList< AudioRecord >();
	private final libvlc_instance_t libvlcInstance;
	private final libvlc_media_list_t playlist;
	private final String mediaOption;
	
	// Lock for the queue
	private final ReentrantLock lock = new ReentrantLock();
	
	private int currentIndex;
	private int tail;
	private int size;
	
	public NativeRotatingPlaylist( libvlc_instance_t libvlcInstance, int size, String option ) {
		this.mediaOption = option;
		this.libvlcInstance = libvlcInstance;
		this.size = size;
		currentIndex = 0;
		tail = 0;
		
		// Create a new MediaList
		playlist = libvlc_media_list_new( libvlcInstance );
		// No clue what this does, it just looks important
		libvlc_media_list_retain( playlist );
	}
	
	public void increment() {
		lock.lock();
		// Increment the current player's position
		currentIndex = ( currentIndex + 1 ) % size;
		
		fill();

		lock.unlock();
	}
	
	public void resetPosition() {
		lock.lock();
		
		currentIndex = 0;
		tail = 0;
		
		fill();
		
		lock.unlock();
	}
	
	private void fill() {
		lock.lock();
		libvlc_media_list_lock( playlist );
		
		// Get the size of the native object
		int nativeSize = libvlc_media_list_count( playlist );
		int difference = ( nativeSize == 0 && currentIndex == tail ) ? size : ( size + ( currentIndex - ( tail + 1 ) ) ) % size;
		int queued = size - difference;
		
		// Get how much we need to fill at most
		int fillAmount = Math.min( queue.size() - queued, difference );
		
		// Get the amount of filler we need
		int playlistSize = size - nativeSize;
		
		for ( int i = 0; i < fillAmount; i++ ) {
			// Fill the thing
			if ( nativeSize != 0 || currentIndex != tail ) {
				tail = ( tail + 1 ) % size;
			}

			// We don't have any more space, so remove what's at the current position
			if ( playlistSize <= 0 ) {
				libvlc_media_list_remove_index( playlist, tail );
			} else {
				playlistSize--;
			}
			
			// Add the record
			AudioRecord record = queue.get( queued + i );
			String path = record.getFile().getAbsolutePath();
			
			libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );
			libvlc_media_add_option( media, mediaOption );
			libvlc_media_list_insert_media( playlist, media, tail );
		}
		
		libvlc_media_list_unlock( playlist );
		lock.unlock();
	}
	
	protected void clearAndRemove( int index ) {
		lock.lock();
		libvlc_media_list_lock( playlist );
		
		// Get the size
		int nativeSize = libvlc_media_list_count( playlist );
		// Clear the playlist
		for ( int i = 0; i < nativeSize; i++ ) {
			libvlc_media_list_remove_index( playlist, 0 );
		}
		
		for ( int i = 0; i < index; i++ ) {
			queue.remove( 0 );
		}
		
		resetPosition();
		
		libvlc_media_list_unlock( playlist );
		lock.unlock();
	}
	
	@Override
	public boolean remove( int index ) {
		lock.lock();
		libvlc_media_list_lock( playlist );
		
		int nativeSize = libvlc_media_list_count( playlist );
		int difference = ( nativeSize == 0 && currentIndex == tail ) ? size : ( size + ( currentIndex - ( tail + 1 ) ) ) % size;
		int queued = size - difference;
		
		queue.remove( index );
		if ( queued > index ) {
			tail = index + currentIndex;
		}
		
		fill();
		
		if ( nativeSize == 0 ) {
			throw new RuntimeException( "Tried to remove from an empty playlist!" );
		}
		
		libvlc_media_list_unlock( playlist );
		lock.unlock();
		
		return true;
	}
	
	@Override
	public boolean insert( int index, AudioRecord file ) {
		lock.lock();
		libvlc_media_list_lock( playlist );
		
		int nativeSize = libvlc_media_list_count( playlist );
		int difference = ( nativeSize == 0 && currentIndex == tail ) ? size : ( size + ( currentIndex - ( tail + 1 ) ) ) % size;
		int queued = size - difference;
		
		queue.add( index, file );
		if ( queued > index ) {
			tail = index + currentIndex;
		}
		
		fill();
		
		libvlc_media_list_unlock( playlist );
		lock.unlock();
		
		return true;
	}
	
	@Override
	public boolean add( AudioRecord file ) {
		lock.lock();
		
		queue.add( file );

		fill();
		
		lock.unlock();
		
		return true;
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
		String mrl = queue.get( index ).getFile().getAbsolutePath();
		
//		libvlc_media_list_lock( playlist );
//		libvlc_media_t item = libvlc_media_list_item_at_index( playlist, index );
//		String mrl = NativeString.copyNativeString( libvlc_media_get_mrl( item ) );
//		libvlc_media_list_unlock( playlist );
		
		// Release it
//		libvlc_media_release( item );
		
		lock.unlock();
		
		return mrl;
	}
	
	@Override
	public int getSize() {
		lock.lock();
//		libvlc_media_list_lock( playlist );
//		int nativeSize = libvlc_media_list_count( playlist );
//		libvlc_media_list_unlock( playlist );
		int size = queue.size();
		lock.unlock();
		
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
