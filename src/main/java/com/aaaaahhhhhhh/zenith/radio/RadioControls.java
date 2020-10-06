package com.aaaaahhhhhhh.zenith.radio;

import java.util.concurrent.locks.ReentrantLock;

public class RadioControls {
	private final ZenithRadio radio;
	private final ReentrantLock lock;
	
	protected RadioControls( ZenithRadio radio ) {
		this.radio = radio;
		lock = radio.getLock();
	}

	public boolean playNext() {
		lock.lock();
		boolean success = false;
		if ( radio.isActive() ) {
			success = radio.getPlayer().playNext();
		}
		lock.unlock();
		
		return success;
	}
	
	public boolean isPlaying() {
		lock.lock();
		boolean success = false;
		if ( radio.isActive() ) {
			success = radio.getPlayer().isPlaying();
		}
		lock.unlock();
		
		return success;
	}
	
	public boolean seek( long ms ) {
		lock.lock();
		boolean success = false;
		if ( radio.isActive() ) {
			success = radio.getPlayer().seek( ms );
		}
		lock.unlock();
		
		return success;
	}
	
	public long getCurrentTime() {
		lock.lock();
		long ms = -1;
		if ( radio.isActive() ) {
			ms = radio.getPlayer().getCurrentTime();
		}
		lock.unlock();
		
		return ms;
		
	}
	
	public long getCurrentLength() {
		lock.lock();
		long ms = -1;
		if ( radio.isActive() ) {
			ms = radio.getPlayer().getCurrentLength();
		}
		lock.unlock();
		
		return ms;
	}
}
