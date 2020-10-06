package com.aaaaahhhhhhh.zenith.radio.test;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_add_option;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_add_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_lock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_play;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_list;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_player;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_retain;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_unlock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_location;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_path;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_release;

import java.io.File;
import java.util.regex.Pattern;

import com.sun.jna.StringArray;

import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;

public class Messy {
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

	public static void main( String[] args ) {
		// A directory containing 4 audio tracks of at least 15 seconds in length
		File file = new File( BASE, "playlist" );
		String path = file.getAbsolutePath();

		// New VLC instance
		libvlc_instance_t libvlcInstance = libvlc_new( FACTORY_OPTIONS.length, new StringArray( FACTORY_OPTIONS ) );
		// Create a new MediaListPlayer
		libvlc_media_list_player_t player = libvlc_media_list_player_new( libvlcInstance );
		// Create a new MediaList
		libvlc_media_list_t playlist = libvlc_media_list_new( libvlcInstance );
		// No clue what this does, it just looks important
		libvlc_media_list_retain( playlist );
		// Set the playlist for the player
		libvlc_media_list_player_set_media_list( player, playlist );

		// Create a separate media player and add it?
		// Is part really necessary, since the media list player comes with its own?
		libvlc_media_player_t mediaPlayer = libvlc_media_player_new( libvlcInstance );
		libvlc_media_list_player_set_media_player( player, mediaPlayer );

		// Stole this from MediaResourceLocator
		libvlc_media_t media = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );

		// Icecast test
		libvlc_media_add_option( media, MEDIA_OPTION );

		// Add the media
		libvlc_media_list_lock( playlist );
		if ( libvlc_media_list_add_media( playlist, media ) == 0 ) {
			System.out.println( "Added the playlist directory successfully" );
		} else {
			System.out.println( "Failed to add the playlist directory" );
		}
		libvlc_media_list_unlock( playlist );

		// Release the media from memory
		libvlc_media_release( media );

		System.out.println( "Broadcasting to " + SERVER_ADDRESS + ":" + SERVER_PORT );
		// Start the playlist
		libvlc_media_list_player_play( player );

		System.out.println( "Sleeping for 55 seconds" );
		try {
			// This is assuming there's 4 tracks
			Thread.sleep( 55_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}

		System.out.println( "Re-adding all previous tracks" );
		// This is just to see if adding another media entry to the list will cause sout to be destroyed,
		// since changing a setting, like transcoding bitrate will create a new one
		// and cause the client(Receiving the stream) to stop.
		// But, if it's the same options, then it will continue.
		// Get the media again
		libvlc_media_t moreMedia = Pattern.compile(".+://.*").matcher( path ).matches() ? libvlc_media_new_location( libvlcInstance, path ) : libvlc_media_new_path( libvlcInstance, path );

		// Add the same options from before
		libvlc_media_add_option( moreMedia, MEDIA_OPTION );

		// Add the media again
		libvlc_media_list_lock( playlist );
		if ( libvlc_media_list_add_media( playlist, moreMedia ) == 0 ) {
			System.out.println( "Added the playlist directory successfully" );
		} else {
			System.out.println( "Failed to add the playlist directory" );
		}
		libvlc_media_list_unlock( playlist );

		// Release the media from memory again
		libvlc_media_release( moreMedia );

		System.out.println( "Sleeping for 65 seconds" );
		try {
			Thread.sleep( 65_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}

		System.out.println( "Done playing!" );

		// Release these from memory
		libvlc_media_player_release( mediaPlayer );
		libvlc_media_list_release( playlist );
		libvlc_media_list_player_release( player );
		libvlc_release( libvlcInstance );
	}
}
