package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.io.IOException;

import ealvatag.audio.AudioFile;
import ealvatag.audio.AudioFileIO;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.audio.exceptions.InvalidAudioFrameException;
import ealvatag.tag.FieldKey;
import ealvatag.tag.Tag;
import ealvatag.tag.TagException;

public class AudioRecord extends FileRecord {
	protected String album = "";
	protected String artist = "";
	protected String title = "";
	protected String disc = "";
	protected String track = "";
	
	public AudioRecord( File file ) {
		super( file );
	}

	@Override
	public boolean update() {
		long newMod = file.lastModified();
		
		if ( newMod > lastModified ) {
			try {
				AudioFile audioFile = AudioFileIO.readIgnoreArtwork( file );
				Tag tag = audioFile.getTag().orNull();
				
				if ( tag != null ) {
					title = tag.getValue( FieldKey.TITLE ).or( file.getName() );
					album = tag.getValue( FieldKey.ALBUM ).or( "Unknown" );
					disc = tag.getValue( FieldKey.DISC_NO ).or( "" );
					track = tag.getValue( FieldKey.TRACK ).or( "" );
					artist = tag.getValue( FieldKey.ARTIST ).or( tag.getValue( FieldKey.ALBUM_ARTIST ).or( "Unknown" ) );
				} else {
					title = file.getName();
					album = "Unknown";
					disc = "";
					track = "";
					artist = "Unknown";
				}
				
				lastModified = newMod;
				return true;
			} catch ( IOException | CannotReadException | TagException | InvalidAudioFrameException e ) {
				// Kind of buggy, but hope that it doesn't break here
				e.printStackTrace();
			}
		}
		return false;
	}

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public String getTitle() {
		return title;
	}

	public String getDisc() {
		return disc;
	}

	public String getTrack() {
		return track;
	}
}
