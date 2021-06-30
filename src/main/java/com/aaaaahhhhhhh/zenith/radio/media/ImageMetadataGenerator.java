package com.aaaaahhhhhhh.zenith.radio.media;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public class ImageMetadataGenerator implements MetadataGenerator {
	protected MetadataGenerator generator;
	protected String baseUrl;
	protected File imageCache;
	
	public ImageMetadataGenerator( MetadataGenerator generator, String baseUrl, File imageCache ) {
		if ( generator == null ) {
			throw new IllegalArgumentException( "The metadata generator provided cannot be null!" );
		}
		this.generator = generator;
		this.baseUrl = baseUrl;
		this.imageCache = imageCache;
	}

	@Override
	public ShoutMetadata getMetadataFor( AudioRecord record ) {
		ShoutMetadata metadata = generator.getMetadataFor( record );
		
		String album = record.getAlbum();
		if ( album != null && !album.equalsIgnoreCase( "unknown" ) && !album.isEmpty() ) {
			BufferedImage image = record.getCoverArt();
			if ( image != null ) {
				String name = album.hashCode() + ".png";
				File coverFile = new File( imageCache, name );
				if ( !coverFile.exists() ) {
					try {
						ImageIO.write( image, "png", coverFile );
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
				
				if ( coverFile.exists() ) {
					metadata.setUrl( baseUrl + name );
				}
			}
		}
		
		return metadata;
	}
}
