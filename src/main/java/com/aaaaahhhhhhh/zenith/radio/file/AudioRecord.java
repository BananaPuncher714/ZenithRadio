package com.aaaaahhhhhhh.zenith.radio.file;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ealvatag.audio.AudioFile;
import ealvatag.audio.AudioFileIO;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.audio.exceptions.InvalidAudioFrameException;
import ealvatag.tag.FieldKey;
import ealvatag.tag.Tag;
import ealvatag.tag.TagException;
import ealvatag.tag.images.Artwork;

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
	
	public BufferedImage getCoverArt() {
		try {
			AudioFile audioFile = AudioFileIO.readIgnoreArtwork( file );
			Tag tag = audioFile.getTag().orNull();
			
			BufferedImage image = null;
			// Try and fetch the image from the file
			if ( tag != null ) {
				if ( tag.getSupportedFields().contains( FieldKey.COVER_ART ) ) {
					Artwork art = tag.getFirstArtwork().orNull();
					if ( art != null ) {
						Object artImage = art.getImage();
						if ( artImage instanceof BufferedImage ) {
							image = ( BufferedImage ) artImage;
						}
					}
				}
			}
			
			// Try and get a cover image from the parent directories
			if ( image == null ) {
				File parent = file.getParentFile();
				for ( int i = 0; i < 2; i++ ) {
					for ( File f : parent.listFiles() ) {
						if ( f.getName().toLowerCase().matches( "^cover.(png|jpg)$" ) ) {
							image = ImageIO.read( f );
						}
					}
					parent = parent.getParentFile();
				}
			}
			
			return image;
		} catch ( IOException | CannotReadException | TagException | InvalidAudioFrameException e ) {
			e.printStackTrace();
			return null;
		}
	}
}
