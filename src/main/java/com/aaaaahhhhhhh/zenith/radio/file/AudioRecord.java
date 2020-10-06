package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

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
				AudioFile audioFile = AudioFileIO.read( file );
				Tag tag = audioFile.getTag();
				
				if ( tag != null ) {
					title = getTag( tag, FieldKey.TITLE );
					if ( title.isEmpty() ) {
						title = file.getName();
					}
					album = getTag( tag, FieldKey.ALBUM );
					disc =  getTag( tag, FieldKey.DISC_NO );
					track = getTag( tag, FieldKey.TRACK );
					artist = getTag( tag, FieldKey.ARTIST );
					if ( artist.isEmpty() ) {
						artist = getTag( tag, FieldKey.ALBUM_ARTIST );
					}
				}
				
				lastModified = newMod;
				return true;
			} catch ( CannotReadException | IOException | TagException | InvalidAudioFrameException | ReadOnlyFileException e ) {
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
	
	private static String getTag( Tag tag, FieldKey key ) {
		try {
			return tag.getFirst( key );
		} catch ( UnsupportedOperationException exception ) {
			return "";
		}
	}
}
