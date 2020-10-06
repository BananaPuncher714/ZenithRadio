package com.aaaaahhhhhhh.zenith.radio.media;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public class DefaultMetadataGenerator implements MetadataGenerator {
	@Override
	public ShoutMetadata getMetadataFor( AudioRecord file ) {
		final String title = file.getTitle();
		final String album = file.getAlbum();
		final String disc =  file.getDisc();
		final String track = file.getTrack();
		final String artist = file.getArtist();

		StringBuilder mainBuilder = new StringBuilder();
		if ( !track.isEmpty() ) {
			if ( !disc.isEmpty() ) {
				mainBuilder.append( disc );
				mainBuilder.append( "." );
			}
			mainBuilder.append( track );
			mainBuilder.append( " – " );
		}

		if ( title.isEmpty() ) {
			mainBuilder.append( "Unknown" );
		} else {
			mainBuilder.append( title );
		}

		if ( !artist.isEmpty() ) {
			mainBuilder.append( " // " );
			mainBuilder.append( artist );
		}

		return new ShoutMetadata( mainBuilder.toString(), album.isEmpty() ? "Unknown" : album );
	}
}
