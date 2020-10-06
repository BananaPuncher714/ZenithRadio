package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;

public class AudioFileValidator implements FileValidator {
	private static final String[] SUPPORTED_TYPES = {
			".flac",
			".mp3",
			".ogg",
			".aiff",
			".wav",
			".wma",
			".dsf",
			".aac",
			".m4a"
		};
	
	@Override
	public boolean isValidFile( File file ) {
		String name = file.getName();
		for ( String pat : SUPPORTED_TYPES ) {
			if ( name.endsWith( pat ) ) {
				return true;
			}
		}
		return false;
	}
}
