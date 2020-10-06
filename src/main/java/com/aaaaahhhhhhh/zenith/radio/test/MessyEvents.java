package com.aaaaahhhhhhh.zenith.radio.test;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_add_option;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_get_mrl;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_add_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_lock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_play;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_list;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_player;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_playback_mode;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_retain;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_unlock;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_location;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_new_path;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_event_manager;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_event_attach;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_event_detach;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_count;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_get_media;

import java.io.File;
import java.util.regex.Pattern;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

import uk.co.caprica.vlcj.binding.NativeString;
import uk.co.caprica.vlcj.binding.internal.libvlc_callback_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_e;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_manager_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.list.PlaybackMode;
import uk.co.caprica.vlcj.support.eventmanager.TaskExecutor;

public class MessyEvents {
	private static File BASE = new File( System.getProperty( "user.dir" ) );

	private static final String[] FACTORY_OPTIONS = {
			// No elementary streams
			"--no-sout-all",
			// Use mp3 for the shout, rather than ogg
			"--sout-shout-mp3",
			// Keep the stream open after playing something
			"--sout-keep",
			// Play each track for only 15 seconds
			"--run-time=15"
			// Verbose verbose verbose
//			"-vvv"
	};

	private static final String SERVER_ADDRESS = "127.0.0.1";
	private static final int SERVER_PORT = 8000;

	// Icecast
	private static final String MEDIA_OPTION = String.format( ":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=shout{name='Icecast Test'},mux=mp3,dst=//source:hackme@localhost:8000/radio}", SERVER_ADDRESS, SERVER_PORT );
	// HTTP
//	private static final String MEDIA_OPTION = String.format( ":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=http,mux=ts,dst=%s:%d}", SERVER_ADDRESS, SERVER_PORT );

	private static final TaskExecutor executor = new TaskExecutor();
	
	public static void main( String[] args ) {
		// A directory containing 4 audio tracks of at least 15 seconds in length
		File file = new File( BASE, "playlist" );
		String path = file.getAbsolutePath();

		// This means the media player has stopped?
		System.out.println( "libvlc_MediaPlayerMediaChanged = " + libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue() );
		// This means it's started?
		System.out.println( "libvlc_MediaPlayerPlaying = " + libvlc_event_e.libvlc_MediaPlayerPlaying.intValue() );
		
		// New VLC instance
		libvlc_instance_t libvlcInstance = libvlc_new( FACTORY_OPTIONS.length, new StringArray( FACTORY_OPTIONS ) );
		// Create a new MediaListPlayer
		libvlc_media_list_player_t player = libvlc_media_list_player_new( libvlcInstance );
		// Create a separate media player and add it?
		// Is part really necessary, since the media list player comes with its own?
		libvlc_media_player_t mediaPlayer = libvlc_media_player_new( libvlcInstance );
		libvlc_media_list_player_set_media_player( player, mediaPlayer );
		// Set the playback mode
		libvlc_media_list_player_set_playback_mode( player, PlaybackMode.LOOP.intValue());
		
		libvlc_callback_t eventCallback = new libvlc_callback_t() {
			@Override
			public void callback( libvlc_event_t event, Pointer userData ) {
				System.out.println( "Event was called for " + event.type );
				if ( event.type == libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue() ) {
					executor.submit( () -> {
						libvlc_media_t playing = libvlc_media_player_get_media( mediaPlayer );
						System.out.println( "Currently playing " + NativeString.copyNativeString( libvlc_media_get_mrl( playing ) ) );
						libvlc_media_release( playing );
						
						/*
						 * At this point in time, we know what is being played next
						 * The question is, just where is this in the playlist?
						 * Assuming that we have a playlist, and we populate it with N items,
						 * and this event gets called each time it plays a new track
						 * then the first time this gets called, it's playing track 1
						 * so, we can also play the next, previous, and any specific track
						 * meaning, we can force it to play whichever index we want
						 * not too sure how good that is, but by determining when it ends or changes media
						 * then we just change it to another one...
						 */
					} );
				}
			}
		};
		
		// Get the event manager
		libvlc_event_manager_t manager = libvlc_media_player_event_manager( mediaPlayer );
		// Listen for some events
		libvlc_event_attach( manager, libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue(), eventCallback, null );
		libvlc_event_attach( manager, libvlc_event_e.libvlc_MediaPlayerPlaying.intValue(), eventCallback, null );
		// Create a new MediaList
		libvlc_media_list_t playlist = libvlc_media_list_new( libvlcInstance );
		// No clue what this does, it just looks important
		libvlc_media_list_retain( playlist );
		// Set the playlist for the player
		libvlc_media_list_player_set_media_list( player, playlist );

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
		
		// Get the size of the media list
		libvlc_media_list_lock( playlist );
		System.out.println( "Current size: " + libvlc_media_list_count( playlist ) );
		libvlc_media_list_unlock( playlist );

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

		// Get the size of the media list again
		libvlc_media_list_lock( playlist );
		System.out.println( "Current size: " + libvlc_media_list_count( playlist ) );
		libvlc_media_list_unlock( playlist );
		
		System.out.println( "Sleeping for 65 seconds" );
		try {
			Thread.sleep( 65_000 );
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}

		System.out.println( "Done playing!" );

		// Release these from memory
		libvlc_event_detach( manager, libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue(), eventCallback, null );
		libvlc_event_detach( manager, libvlc_event_e.libvlc_MediaPlayerPlaying.intValue(), eventCallback, null );
		libvlc_media_player_release( mediaPlayer );
		libvlc_media_list_release( playlist );
		libvlc_media_list_player_release( player );
		libvlc_release( libvlcInstance );
		
		executor.release();
	}
}
