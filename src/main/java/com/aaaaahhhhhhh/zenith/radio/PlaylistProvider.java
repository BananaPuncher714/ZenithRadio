package com.aaaaahhhhhhh.zenith.radio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.MediaProvider;

public class PlaylistProvider implements MediaProvider {
	List< AudioRecord > records = new ArrayList< AudioRecord >();
	int index = 0;
	
	public void reset() {
		index = 0;
		Collections.shuffle( records );
	}
	
	@Override
	public AudioRecord provide() {
		if ( records.isEmpty() ) {
			return null;
		}
		
		AudioRecord record = records.get( index );
		index = ( index + 1 ) % records.size();
		
		// Shuffle when it finishes playing each file
		if ( index == 0 ) {
			Collections.shuffle( records );
		}
		
		return record;
	}
	
	@Override
	public boolean available() {
		return !records.isEmpty();
	}
}
