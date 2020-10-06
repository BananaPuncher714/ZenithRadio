package com.aaaaahhhhhhh.zenith.radio.test;

import java.io.File;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioListPlayerComponent;

public class SoutKeepTest {
	private static File BASE = new File( System.getProperty( "user.dir" ) );

	private static final String[] FACTORY_OPTIONS = {
//			"--sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=shout{name='Zenith Radio'},mux=mp3,dst=//source:hackme@localhost:8000/radio}",
			"--no-sout-all",
			"--sout-shout-mp3",
			"--sout-keep"
//			"-vvv"
	};
	private static final String[] OPTIONS = {
			":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=shout,mux=mp3,dst=//source:hackme@localhost:8000/radio}",
//			":sout=#transcode{vcodec=none,acodec=vorb,ab=320,channels=2,samplerate=44100,scodec=none}:gather:std{access=shout{name='Icecast Test'},mux=ogg,dst=//source:hackme@localhost:8000/radio}",
//			":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:gather:std{access=file,dst=" + new File( BASE, "fmttm.mp3" ) + "}",
//			":sout=#transcode{vcodec=none,acodec=mp3,ab=320,channels=2,samplerate=44100,scodec=none}:std{access=http,mux=ts,dst=localhost:8080}",
//			":sout-keep"
	};
	
	public static void main( String[] args ) {
		MediaPlayerFactory factory = new MediaPlayerFactory( FACTORY_OPTIONS );
		AudioListPlayerComponent component = new AudioListPlayerComponent( factory );
		component.mediaPlayer().events().addMediaPlayerEventListener( new MediaPlayerEventAdapter() {
		    @Override
		    public void playing( MediaPlayer mediaPlayer ) {
		    	System.out.println( "Started playing mediaPlayer" );
		    }

		    @Override
		    public void finished( MediaPlayer mediaPlayer ) {
		    	System.out.println( "Finished playing" );
		    }

		    @Override
		    public void error( MediaPlayer mediaPlayer ) {
		    	System.out.println( "Stopped playing forcefully" );
		    }
		} );

		for ( File file : new File( BASE, "playlist" ).listFiles() ) {
//			if ( component.mediaListPlayer().list().media().add( file.getAbsolutePath() ) ) {
			if ( component.mediaListPlayer().list().media().add( file.getAbsolutePath(), OPTIONS ) ) {
				System.out.println( "Added file " + file + " to the playlist successfully" );
			} else {
				System.out.println( "Unable to add file " + file + " to the playlist" );
			}
		}
//		if ( component.mediaListPlayer().list().media().add( new File( BASE, "playlist" ).getAbsolutePath(), OPTIONS ) ) {
//			System.out.println( "Added the playlist directory successfully" );
//		} else {
//			System.out.println( "Failed to add the playlist directory" );
//		}

		System.out.println( "Starting..." );
		component.mediaListPlayer().controls().play();
		
		while ( true ) {
			try {
				Thread.sleep( 1000 );
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}
}
