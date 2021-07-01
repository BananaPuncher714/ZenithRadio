package com.aaaaahhhhhhh.zenith.radio.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;

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
					if ( ( title = tag.getFirst( FieldKey.TITLE ) ) == null ) title = file.getName();
					if ( ( album = tag.getFirst( FieldKey.ALBUM ) ) == null ) album = "Unknown";
					if ( ( disc = tag.getFirst( FieldKey.DISC_NO ) ) == null ) disc = "";
					if ( ( track = tag.getFirst( FieldKey.TRACK ) ) == null ) track = "";
					if ( ( artist = tag.getFirst( FieldKey.ARTIST ) ) == null ) if ( ( artist = tag.getFirst( FieldKey.ALBUM_ARTIST ) ) == null ) artist = "Unknown";
				} else {
					title = file.getName();
					album = "Unknown";
					disc = "";
					track = "";
					artist = "Unknown";
				}
				
				lastModified = newMod;
				return true;
			} catch ( IOException | CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException e ) {
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
	
	public BufferedImage getCoverArt() {
		try {
			AudioFile audioFile = AudioFileIO.read( file );
			Tag tag = audioFile.getTag();
			
			BufferedImage image = null;
			// Try and fetch the image from the file
			if ( tag != null ) {
				Artwork art = tag.getFirstArtwork();
				if ( art != null ) {
					Object artImage = art.getImage();
					if ( artImage instanceof BufferedImage ) {
						image = ( BufferedImage ) artImage;
					}
				}
			}
			
			// Try and get a cover image from the parent directories
			if ( image == null ) {
				File parent = file.getParentFile();
				for ( int i = 0; i < 2; i++ ) {
					for ( File f : parent.listFiles() ) {
						if ( f.getName().toLowerCase().matches( "^(folder|cover)\\.(png|jpg)$" ) ) {
							image = ImageIO.read( f );
						}
					}
					parent = parent.getParentFile();
				}
			}
			
			return image;
		} catch ( IOException | CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException e ) {
			e.printStackTrace();
			return null;
		}
	}
}
