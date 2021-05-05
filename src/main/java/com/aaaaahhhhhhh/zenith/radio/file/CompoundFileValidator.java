package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;

public class CompoundFileValidator implements FileValidator {
	private AudioFileValidator audioValidator;
	private ExclusionFileValidator exclusionValidator;
	
	public CompoundFileValidator( String[] exclude ) {
		audioValidator = new AudioFileValidator();
		exclusionValidator = new ExclusionFileValidator( exclude );
	}

	@Override
	public boolean isValidFile( File file ) {
		if ( file.isFile() ) {
			return audioValidator.isValidFile( file ) && exclusionValidator.isValidFile( file );
		} else {
			return exclusionValidator.isValidFile( file );
		}
	}
}
