package com.aaaaahhhhhhh.zenith.radio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.aaaaahhhhhhh.zenith.radio.shout.IceMount;

public class ZenithRadioProperties {
	private final File file;
	private final Properties properties = new Properties();
	
	public ZenithRadioProperties( File file ) {
		this.file = file;
		
		if ( file.exists() ) {
			load();
		} else {
			loadDefaults();
			save();
		}
	}
	
	public boolean isMusicDirectorySet() {
		String val = properties.getProperty( "music-directory" );
		return val != null && !val.isEmpty();
	}
	
	public File getMusicDirectory() {
		return new File( properties.getProperty( "music-directory" ) );
	}
	
	public IceMount getIceMount() {
		return new IceMount( properties.getProperty( "icecast-username" ),
				properties.getProperty( "icecast-address" ),
				Integer.valueOf( properties.getProperty( "icecast-port" ) ),
				properties.getProperty( "icecast-password" ),
				properties.getProperty( "icecast-mount" ) )
				.setDescription( properties.getProperty( "icecast-description" ) )
				.setName( properties.getProperty( "icecast-name" ) )
				.setBitrate( Integer.valueOf( properties.getProperty( "icecast-bitrate" ) ) )
				.setUrl( properties.getProperty( "icecast-url" ) )
				.setUseMp3( Boolean.valueOf( properties.getProperty( "icecast-use-mp3" ) ) );
	}
	
	protected Properties getProperties() {
		return properties;
	}
	
	private void loadDefaults() {
		// The main directory where music is stored
		properties.setProperty( "music-directory", "" );
		
		// The icecast connection
		properties.setProperty( "icecast-address", "localhost" );
		properties.setProperty( "icecast-port", "8000" );
		properties.setProperty( "icecast-username", "admin" );
		properties.setProperty( "icecast-password", "hackme" );
		properties.setProperty( "icecast-mount", "radio" );
		properties.setProperty( "icecast-name", "Zenith Radio" );
		properties.setProperty( "icecast-description", "Zenith Gaming House Radio Service" );
		properties.setProperty( "icecast-url", "https://www.aaaaahhhhhhh.com" );
		properties.setProperty( "icecast-bitrate", "320" );
		properties.setProperty( "icecast-use-mp3", "true" );
	}
	
	private void load() {
		try ( InputStream stream = new FileInputStream( file ) ) {
			properties.load( stream );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	protected void save() {
		file.getParentFile().mkdirs();
		try ( OutputStream stream = new FileOutputStream( file ) ) {
			properties.store( stream, null );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}
