package com.aaaaahhhhhhh.zenith.radio.shout;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_event_attach;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_event_detach;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_get_mrl;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_is_playing;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_next;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_pause;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_play;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_play_item_at_index;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_list;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_media_player;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_set_playback_mode;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_list_player_stop;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_event_manager;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_get_length;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_get_media;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_get_time;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_is_seekable;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_player_set_time;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_media_release;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_new;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_release;

import java.util.ArrayList;
import java.util.List;

import com.aaaaahhhhhhh.zenith.radio.media.MediaChangedEventCallback;
import com.aaaaahhhhhhh.zenith.radio.media.MediaPlayer;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

import uk.co.caprica.vlcj.binding.NativeString;
import uk.co.caprica.vlcj.binding.internal.libvlc_callback_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_e;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_manager_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_event_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_instance_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_list_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_player_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.list.PlaybackMode;
import uk.co.caprica.vlcj.support.eventmanager.TaskExecutor;

public class NativePlayer implements MediaPlayer {
	private static final String MEDIA_OPTION_TEMPLATE = ":sout=#transcode{vcodec=none,acodec=%s,ab=%d,channels=2,samplerate=44100,scodec=none}:std{access=shout,mux=%s,dst=//source:%s@%s:%d/%s}";
	
	private final libvlc_instance_t libvlcInstance;
	private final libvlc_media_list_player_t player;
	private final libvlc_media_player_t mediaPlayer;
	private final libvlc_callback_t eventCallback;

	private final TaskExecutor executor = new TaskExecutor();
	private final NativePlaylist playlist;
	private final String mediaOption;

	private MediaChangedEventCallback callback;
	private boolean paused = false;
	
	public NativePlayer( IceMount mount, String... options ) {
		String[] factoryOptions = getFactoryArgsFor( mount, options );
		this.libvlcInstance = libvlc_new( factoryOptions.length, new StringArray( factoryOptions ) );
		this.mediaOption = String.format( MEDIA_OPTION_TEMPLATE,
				( mount.isUseMp3() ? "mp3" : "vorb" ),
				mount.getBitrate(),
				( mount.isUseMp3() ? "mp3" : "ogg" ),
				mount.getPasswd(),
				mount.getServerAddress(),
				mount.getPort(),
				mount.getMount() );

		// Create a new MediaListPlayer
		player = libvlc_media_list_player_new( libvlcInstance );
		// Create a separate media player and add it?
		// Is part really necessary, since the media list player comes with its own?
		mediaPlayer = libvlc_media_player_new( libvlcInstance );
		libvlc_media_list_player_set_media_player( player, mediaPlayer );
		
		// Set the playback mode. If it ever hits the end, it'll loop around instead of stopping
		libvlc_media_list_player_set_playback_mode( player, PlaybackMode.LOOP.intValue() );
		
		eventCallback = new libvlc_callback_t() {
			@Override
			public void callback( libvlc_event_t event, Pointer userData ) {
				if ( event.type == libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue() ) {
					if ( callback != null ) {
						executor.submit( () -> {
							libvlc_media_t playing = libvlc_media_player_get_media( mediaPlayer );
							String mrl = NativeString.copyNativeString( libvlc_media_get_mrl( playing ) );
							libvlc_media_release( playing );
							
							callback.mediaChanged( mrl );
						} );
					}
				}
			}
		};
		
		// Get the event manager and listen for some events
		libvlc_event_manager_t manager = libvlc_media_player_event_manager( mediaPlayer );
		libvlc_event_attach( manager, libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue(), eventCallback, null );
		
		// Set the playlist for the player
		playlist = new NativePlaylist( libvlcInstance, mediaOption );
		libvlc_media_list_player_set_media_list( player, playlist.getNativeMediaList() );
	}
	
	public void release() {
		playlist.release();
		
		libvlc_event_manager_t manager = libvlc_media_player_event_manager( mediaPlayer );
		libvlc_event_detach( manager, libvlc_event_e.libvlc_MediaPlayerMediaChanged.intValue(), eventCallback, null );
		libvlc_media_player_release( mediaPlayer );
		libvlc_media_list_player_release( player );
		libvlc_release( libvlcInstance );
		executor.release();
	}
	
	@Override
	public NativePlaylist getPlaylist() {
		return playlist;
	}
	
	@Override
	public void setCallback( MediaChangedEventCallback callback ) {
		this.callback = callback;
	}
	
	@Override
	public void play() {
		paused = false;
		libvlc_media_list_player_play( player );
	}
	
	@Override
	public void stop() {
		paused = false;
		libvlc_media_list_player_stop( player );
	}
	
	@Override
	public void pause() {
		paused = true;
		libvlc_media_list_player_pause( player );
	}
	
	@Override
	public boolean playNext() {
		boolean successful = false;
		if ( successful = libvlc_media_list_player_next( player ) == 0 ) {
			paused = false;
		}
		return successful;
	}
	
	@Override
	public void play( int index ) {
		paused = false;
		libvlc_media_list_player_play_item_at_index( player, index );
	}
	
	@Override
	public boolean isPaused() {
		return paused;
	}
	
	@Override
	public boolean isPlaying() {
		return libvlc_media_list_player_is_playing( player ) == 1;
	}
	
	@Override
	public boolean isActive() {
		return paused || isPlaying();
	}
	
	private static String[] getFactoryArgsFor( IceMount mount, String... options ) {
		List< String > args = new ArrayList< String >();
		
		args.add( "--no-sout-all" );
		if ( mount.isUseMp3() ) {
			args.add( "--sout-shout-mp3" );
		}
		args.add( "--sout-keep" );
		
		args.add( "--sout-shout-name=" + mount.getName() );
		args.add( "--sout-shout-description=" + mount.getDescription() );
		args.add( "--sout-shout-genre=" + mount.getGenre() );
		args.add( "--sout-shout-url=" + mount.getUrl() );
		
		for ( String str : options ) {
			args.add( str );
		}
		
		return args.toArray( new String[ args.size() ] );
	}

	@Override
	public boolean seek( long ms ) {
		if ( libvlc_media_player_is_seekable( mediaPlayer ) == 1 ) {
			libvlc_media_player_set_time( mediaPlayer, ms );
			return true;
		}
		return false;
	}

	@Override
	public long currentTime() {
		return libvlc_media_player_get_time( mediaPlayer );
	}

	@Override
	public long currentLength() {
		return libvlc_media_player_get_length( mediaPlayer );
	}
}
