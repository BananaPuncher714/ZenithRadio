package com.aaaaahhhhhhh.zenith.radio.media;

public interface MediaPlayer {
	void play();
	void pause();
	void stop();
	boolean playNext();
	void play( int index );
	boolean seek( long ms );
	long currentTime();
	long currentLength();
	boolean isPaused();
	boolean isPlaying();
	boolean isActive();
	void setCallback( MediaEventCallback callback );
	Playlist getPlaylist();
	void release();
}
