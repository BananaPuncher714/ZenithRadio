package com.aaaaahhhhhhh.zenith.radio.shout;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_add_option;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_get_mrl;
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
	
	private int head;
	private int tail;
	private int size;
	
	public NativeRotatingPlaylist( libvlc_instance_t libvlcInstance, int size, String option ) {
		this.mediaOption = option;
		this.libvlcInstance = libvlcInstance;
		this.size = size;
		head = -1;
		tail = -1;
		
		// Create a new MediaList
		playlist = libvlc_media_list_new( libvlcInstance );
		// No clue what this does, it just looks important
		libvlc_media_list_retain( playlist );
	}
	
	protected void increment() {
		// Increment the current player's head
		head = ( head + 1 ) % size;
	}
	
	protected int getHead() {
		return head;
	}
	
	public void resetPosition() {
		lock.lock();
		head = -1;
		tail = -1;
		lock.unlock();
	}
	
	protected void fill() {
		// Get the size of the native object
		int nativeSize = libvlc_media_list_count( playlist );
		// Get how many need to be filled
		// If it's empty and the head == tail, then size
		// Otherwise, get the head - ( tail + 1 ) mod size
		int difference = ( head == -1 && tail == -1 ) ? size : ( size + ( head - ( tail + 1 ) ) ) % size;
		int queued = size - difference;
		
		// Get how much we need to fill at most
		int fillAmount = Math.min( queue.size() - queued, difference );
		
		// Get the amount of filler we need
		int playlistSize = size - nativeSize;
		
		for ( int i = 0; i < fillAmount; i++ ) {
			// Increase the tail count
			tail = ( tail + 1 ) % size;

			// We don't have any more space, so remove what's at the current position
			if ( playlistSize-- <= 0 ) {
				libvlc_media_list_remove_index( playlist, tail );
			}
			
			// Add the record
			AudioRecord record = queue.get( queued + i );
			String path = record.getFile().getAbsolutePath();
			
			libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );
			libvlc_media_add_option( media, mediaOption );
			libvlc_media_list_insert_media( playlist, media, tail );
		}
	}

	@Override
	public boolean remove( int index ) {
		queue.remove( index );
		
		return true;
	}
	
	@Override
	public boolean insert( int index, AudioRecord file ) {
		queue.add( index, file );
		
		// TODO What? Does this even work properly?
		if ( index < size && head != -1 ) {
			// Check if the index or tail is smaller
			int difference = ( size + ( tail - head ) ) % size;
			if ( index <= difference ) {
				// If so, then reset the tail
				tail = ( index + head - 1 ) % size;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean add( AudioRecord file ) {
		queue.add( file );

		return true;
	}
	
	@Override
	public AudioRecord get( int index ) {
		AudioRecord file = queue.get( index );
		
		return file;
	}
	
	@Override
	public String getMrl( int index ) {
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
//		libvlc_media_list_lock( playlist );
//		int nativeSize = libvlc_media_list_count( playlist );
//		libvlc_media_list_unlock( playlist );
		int size = queue.size();
		
		return size;
	}
	
	@Override
	public List< AudioRecord > getQueue() {
		List< AudioRecord > copy = Collections.unmodifiableList( queue );
		
		return copy;
	}
	
	void debugOut() {
		int size = libvlc_media_list_count( playlist );
		System.out.println( "Current size: " + size + " vs queue " + queue.size() );
        for (int i = 0; i < size; i++) {
            libvlc_media_t item = libvlc_media_list_item_at_index( playlist, i );
            System.out.println( "Item is " + NativeString.copyNativeString( libvlc_media_get_mrl( item ) ) );
            libvlc_media_release(item);
        }
	}
	
	public libvlc_media_list_t getNativeMediaList() {
		return playlist;
	}
	
	@Override
	public void release() {
		libvlc_media_list_release( playlist );
	}
	
	protected void lock() {
		lock.lock();
		libvlc_media_list_lock( playlist );
		
	}
	
	protected void unlock() {
		libvlc_media_list_unlock( playlist );
		lock.unlock();
	}
}
