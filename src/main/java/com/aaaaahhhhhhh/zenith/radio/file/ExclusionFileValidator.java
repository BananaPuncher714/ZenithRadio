package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.util.regex.Pattern;

public class ExclusionFileValidator implements FileValidator {
	private Pattern[] patterns;
	
	public ExclusionFileValidator( String... regex ) {
		patterns = new Pattern[ regex.length ];
		for ( int i = 0; i < regex.length; i++ ) {
			patterns[ i ] = Pattern.compile( regex[ i ] );
		}
	}
	
	@Override
	public boolean isValidFile( File file ) {
		String path = file.getAbsolutePath();
		for ( Pattern pattern : patterns ) {
			if ( pattern.matcher( path ).find() ) {
				return false;
			}
		}
		return true;
	}

}
