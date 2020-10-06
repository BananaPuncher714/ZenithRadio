package com.aaaaahhhhhhh.zenith.radio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.file.MusicCache;
import com.aaaaahhhhhhh.zenith.radio.media.MediaProvider;

public class CacheProvider implements MediaProvider {
	private final MusicCache cache;
	
	private int index = 0;
	private List< AudioRecord > tracks = new ArrayList< AudioRecord >();
	private Set< AudioRecord > blacklist = new HashSet< AudioRecord >();
	
	private final ReentrantLock lock = new ReentrantLock();
	
	public CacheProvider( MusicCache cache ) {
		this.cache = cache;
		
		refresh();
	}
	
	public void refresh() {
		Set< AudioRecord > records = new HashSet< AudioRecord >( cache.getRecords() );
		lock.lock();
		blacklist.retainAll( records );
		records.removeAll( blacklist );
		lock.unlock();
		tracks = new ArrayList< AudioRecord >( records );
		
		Collections.shuffle( tracks );
		index = 0;
	}
	
	public void addToBlacklist( AudioRecord record ) {
		record.update();
		lock.lock();
		blacklist.add( record );
		tracks.remove( record );
		lock.unlock();
	}
	
	public void setBlacklist( Collection< AudioRecord > records ) {
		for ( AudioRecord record : records ) {
			record.update();
		}
		lock.lock();
		blacklist.addAll( records );
		lock.unlock();
	}
	
	public Set< AudioRecord > getBlacklist() {
		lock.lock();
		Set< AudioRecord > newSet = new HashSet< AudioRecord >( blacklist );
		lock.unlock();
		
		return newSet;
	}
	
	public boolean isBlacklistEmpty() {
		lock.lock();
		boolean success = blacklist.isEmpty();
		lock.unlock();
		
		return success;
	}
	
	@Override
	public AudioRecord provide() {
		AudioRecord record = tracks.get( index );
		index = ( index + 1 ) % tracks.size();
		
		// Shuffle when it finishes playing each file
		if ( index == 0 ) {
			Collections.shuffle( tracks );
		}
		
		return record;
	}
	
	@Override
	public boolean available() {
		return !tracks.isEmpty() || !blacklist.isEmpty();
	}
}
