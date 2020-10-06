package com.aaaaahhhhhhh.zenith.radio.media;

import java.util.List;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public interface Playlist {
	boolean remove( int index );
	boolean add( AudioRecord file );
	boolean insert( int index, AudioRecord file );
	AudioRecord get( int index );
	String getMrl( int index );
	List< AudioRecord > getQueue();
	int getSize();
	void release();
}
