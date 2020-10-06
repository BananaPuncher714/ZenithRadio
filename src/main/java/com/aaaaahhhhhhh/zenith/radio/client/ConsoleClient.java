package com.aaaaahhhhhhh.zenith.radio.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public class ConsoleClient extends RadioClient {
	ZenithRadio radio;
	InputStream in;
	Thread thread;
	
	boolean running = true;
	
	public ConsoleClient( InputStream stream ) {
		this.in = stream;
		
		thread = new Thread( this::activate );
		thread.start();
	}
	
	@Override
	public void onAttach( ZenithRadio radio ) {
		this.radio = radio;
		ZenithRadio.debug( "Attached to radio!" );
	}

	@Override
	public void onDetach( ZenithRadio radio ) {
		radio = null;
		ZenithRadio.debug( "Detached from radio!" );
	}
	
	private void activate() {
		BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
		
		List< AudioRecord > searchList = new ArrayList< AudioRecord >();
		
		try ( Scanner scanner = new Scanner( in ) ) {
			String output = "";
			while ( running ) {
				System.out.print( "> " );

				while ( !reader.ready() && running ) {
					Thread.sleep( 200 );
				}
				
				if ( running ) {
					output = reader.readLine();
					ZenithRadio local = radio;
					if ( local != null ) {
						if ( output.equalsIgnoreCase( "next" ) || output.equalsIgnoreCase( "skip" ) ) {
							System.out.println( "Skipping..." );
							local.getControlsApi().playNext();
						} else if ( output.equalsIgnoreCase( "shuffle" ) ) {
							System.out.println( "Refilling the playlist" );
							local.getMediaApi().refill();
						} else if ( output.equalsIgnoreCase( "playlist" ) ) {
							System.out.println( "Displaying current playlist:" );
							local.debugPrintPlaylist();
						} else if ( output.equalsIgnoreCase( "stop" ) ) {
							local.stop();
						} else if ( output.equalsIgnoreCase( "drama" ) ) {
							System.out.println( "Adding to blacklist!" );
							AudioRecord playing = local.getMediaApi().getPlaying();
							local.getPlaylistManager().getDefaultProvider().addToBlacklist( playing );
							local.getControlsApi().playNext();
						} else if ( output.equalsIgnoreCase( "time" ) ) {
							long time = local.getControlsApi().getCurrentTime();
							long length = local.getControlsApi().getCurrentLength();
							System.out.println( "Time: " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( length ) );
						} else if ( output.startsWith( "search " ) ) {
							String key = output.substring( 7 ).toLowerCase().replaceAll( "\\s+", "" );
							if ( key.length() > 2 ) {
								searchList.clear();
								for ( AudioRecord record : radio.getMediaApi().getRecords() ) {
									if ( record.getTitle().toLowerCase().contains( key ) ||
											record.getAlbum().toLowerCase().contains( key ) ||
											record.getArtist().toLowerCase().contains( key ) ) {
										searchList.add( record );
									}
								}
								
								if ( searchList.isEmpty() ) {
									System.out.println( "No matches!" );
								} else {
									System.out.println( "Matches(" + searchList.size() + "):" );
									for ( int i = 0; i < searchList.size(); i++ ) {
										AudioRecord record = searchList.get( i );
										System.out.println( ( i + 1 ) + ":\t" + record.getAlbum() + " - " + record.getTitle() + " // " + record.getArtist() );
									}
								}
							} else {
								System.out.println( "Your query must be longer than 2 characters!" );
							}
						} else if ( output.matches( "play \\d+" ) ) {
							int index = Integer.valueOf( output.split( "\\s+" )[ 1 ] ) - 1;
							
							if ( index < 0 || index >= searchList.size() ) {
								System.out.println( "Selection out of bounds!" );
							} else {
								AudioRecord record = searchList.get( index );
								System.out.println( "Enqueuing " + record.getTitle() );
								radio.getMediaApi().enqueue( record, false );
							}
						}
					} else {
						System.out.println( "Not attached to any radio!" );
					}
				}
			}
		} catch ( IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMediaChange( ZenithRadio radio, AudioRecord record ) {
		System.out.println( "Now playing " + record.getTitle() );
	}

	@Override
	public void stop() {
		running = false;
	}
}
