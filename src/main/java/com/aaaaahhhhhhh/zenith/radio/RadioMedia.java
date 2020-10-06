package com.aaaaahhhhhhh.zenith.radio;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.media.MetadataGenerator;

public class RadioMedia {
	private final ZenithRadio radio;
	private final ReentrantLock lock;
	
	protected RadioMedia( ZenithRadio radio ) {
		this.radio = radio;
		this.lock = radio.getLock();
	}
	
	public List< AudioRecord > getPlaylist() {
		lock.lock();
		List< AudioRecord > playlist = null;
		if ( radio.isActive() ) {
			playlist = radio.getPlayer().getPlaylist();
		}
		lock.unlock();
		
		return playlist;
	}
	
	public Collection< AudioRecord > getRecords() {
		return radio.getCache().getRecords();
	}
	
	public AudioRecord getRecordFor( File file ) {
		return radio.getCache().getRecord( file );
	}
	
	public AudioRecord getPlaying() {
		lock.lock();
		AudioRecord record = null;
		if ( radio.isActive() ) {
			record = radio.getPlayer().getCurrentlyPlaying();
		}
		lock.unlock();
		
		return record;
	}
	
	public void refill() {
		lock.lock();
		if ( radio.isActive() ) {
			radio.getPlayer().refill();
		}
		lock.unlock();
	}
	
	public boolean enqueue( AudioRecord record, boolean skip ) {
		lock.lock();
		boolean success = false;
		if ( radio.isActive() ) {
			success = radio.getPlayer().enqueue( record, skip );
		}
		lock.unlock();
		
		return success;
	}
	
	public boolean setPlaylist( String id ) {
		boolean success = true;
		
		lock.lock();
		if ( radio.isActive() ) {
			if ( id == null ) {
				radio.getPlayer().setProvider( radio.getPlaylistManager().getDefaultProvider() );
			}
			
			PlaylistProvider provider = radio.getPlaylistManager().get( id );
			if ( provider == null ) {
				success = false;
			} else {
				radio.getPlayer().setProvider( provider );
			}
		}
		lock.unlock();
		
		return success;
	}
	
	public void setMetadataGenerator( MetadataGenerator generator ) {
		radio.getPlayer().setMetadata( generator );
	}
	
	public MetadataGenerator getMetadataGenerator() {
		return radio.getPlayer().getMetadata();
	}
}
