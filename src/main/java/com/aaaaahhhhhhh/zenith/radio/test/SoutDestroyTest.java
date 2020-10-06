package com.aaaaahhhhhhh.zenith.radio.test;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_add_option;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_add_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_lock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_unlock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_location;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_path;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_release;

import java.io.File;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.medialist.MediaList;
import uk.co.caprica.vlcj.player.component.AudioListPlayerComponent;

public class SoutDestroyTest {
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
		AudioListPlayerComponent component = new AudioListPlayerComponent( factory );
		
		// Add the media directly, without going through MediaRef or something
		{
			Field mediaListField = AudioListPlayerComponent.class.getDeclaredField( "mediaList" );
			mediaListField.setAccessible( true );
			MediaList mediaList = ( MediaList ) mediaListField.get( component );
			Field libvlcInstanceField = MediaPlayerFactory.class.getDeclaredField( "libvlcInstance" );
			libvlcInstanceField.setAccessible( true );
			libvlc_instance_t libvlcInstance = ( libvlc_instance_t ) libvlcInstanceField.get( factory );

			libvlc_media_list_t playlist = mediaList.mediaListInstance();
			
			// Stole this from MediaResourceLocator
			libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );
			libvlc_media_add_option( media, MEDIA_OPTION );
			
			libvlc_media_list_lock( playlist );
			if ( libvlc_media_list_add_media( playlist, media ) == 0 ) {
				System.out.println( "Added the playlist directory successfully" );
			} else {
				System.out.println( "Failed to add the playlist directory" );
			}
			libvlc_media_list_unlock( playlist );
			
			libvlc_media_release( media );
		}
		
		System.out.println( "Broadcasting to " + SERVER_ADDRESS + ":" + SERVER_PORT );
		component.mediaListPlayer().controls().play();
		
		System.out.println( "Sleeping for 55 seconds" );
		try {
			// This is assuming there's 4 tracks
			Thread.sleep( 55_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		
		System.out.println( "Re-adding all previous tracks" );
		if ( component.mediaListPlayer().list().media().add( path, MEDIA_OPTION ) ) {
			System.out.println( "Added the playlist directory successfully" );
		} else {
			System.out.println( "Failed to add the playlist directory" );
		}
		
		System.out.println( "Sleeping for 65 seconds" );
		try {
			Thread.sleep( 65_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		
		System.out.println( "Done playing!" );
		
		component.release();
	}
}