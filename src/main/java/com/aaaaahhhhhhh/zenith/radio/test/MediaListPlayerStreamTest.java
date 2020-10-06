package com.aaaaahhhhhhh.zenith.radio.test;

import java.io.File;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.medialist.MediaList;
import uk.co.caprica.vlcj.medialist.MediaListRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.list.MediaListPlayer;

public class MediaListPlayerStreamTest {
	private static File BASE = new File( System.getProperty( "user.dir" ) );

	private static final String[] FACTORY_OPTIONS = {
			// No elementary streams
			"--no-sout-all",
			// Use mp3 for the shout, rather than ogg
			"--sout-shout-mp3",
			// Keep the stream open after playing something
			"--sout-keep",
			// Play each track for only 15 seconds
			"--run-time=15",
			// Verbose verbose verbose
			"-vvv"
	};

	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int SERVER_PORT = 8000;
	
	// Icecast
	private static final String MEDIA_OPTION = String.format( ":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=shout{name='Icecast Test'},mux=mp3,dst=//source:hackme@localhost:8000/radio}", SERVER_ADDRESS, SERVER_PORT );
	// HTTP
//	private static final String MEDIA_OPTION = String.format( ":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=http,mux=ts,dst=%s:%d}", SERVER_ADDRESS, SERVER_PORT );
	
	public static void main( String[] args ) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// A directory containing 4 audio tracks of at least 15 seconds in length
		File file = new File( BASE, "playlist" );
		String path = file.getAbsolutePath();
		
		MediaPlayerFactory factory = new MediaPlayerFactory( FACTORY_OPTIONS );
		// Construct a MediaListPlayer, rather than an AudioListPlayerComponent
		MediaListPlayer listPlayer = factory.mediaPlayers().newMediaListPlayer();
		// Construct a MediaPlayer
		MediaPlayer player = factory.mediaPlayers().newMediaPlayer();

		
		// In particular it is this line which breaks it.
		// Removing it causes it to play normally
//		listPlayer.mediaPlayer().setMediaPlayer( player );
		

		
		// Construct a new media list
		MediaList list = factory.media().newMediaList();
		// Set the media list to the player's ListApi 
		MediaListRef ref = list.newMediaListRef();
		listPlayer.list().setMediaList( ref );
		ref.release();

		// Add the media items normally
		System.out.println( "Re-adding all previous tracks" );
		if ( listPlayer.list().media().add( path, MEDIA_OPTION ) ) {
			System.out.println( "Added the playlist directory successfully" );
		} else {
			System.out.println( "Failed to add the playlist directory" );
		}
		
		System.out.println( "Broadcasting to " + SERVER_ADDRESS + ":" + SERVER_PORT );
		listPlayer.controls().play();
		
		System.out.println( "Sleeping for 60 seconds" );
		try {
			// This is assuming there's 4 tracks
			Thread.sleep( 60_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		
		System.out.println( "Done playing!" );
		list.release();
		player.release();
		listPlayer.release();
		factory.release();
	}
}