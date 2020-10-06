package com.aaaaahhhhhhh.zenith.radio.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class DiscordProperties {
	private final File file;
	private final Properties properties = new Properties();
	
	public DiscordProperties( File file ) {
		this.file = file;
		
		if ( file.exists() ) {
			load();
		} else {
			loadDefaults();
			save();
		}
	}
	
	protected Properties getProperties() {
		return properties;
	}
	
	protected String getToken() {
		return properties.getProperty( "token" );
	}
	
	protected File getWebmFolder() {
		String folder = properties.getProperty( "webms" );
		if ( !folder.isEmpty() ) {
			return new File( folder );
		}
		return null;
	}
	
	protected boolean isAdmin( String id ) {
		String[] arr = properties.getProperty( "admin-ids" ).split( "," );
		for ( String a : arr ) {
			if ( a.equals( id ) ) {
				return true;
			}
		}
		return false;
	}
	
	protected MessageChannel getChannel( JDA api ) {
		if ( api == null ) {
			return null;
		}
		
		String guildId = properties.getProperty( "guild-id" );
		String channel = properties.getProperty( "channel-id" );
		
		if ( guildId.isEmpty() || channel.isEmpty() ) {
			return null;
		}
		
		Guild guild = api.getGuildById( guildId );

		if ( guild != null ) {
			return api.getGuildById( guildId ).getTextChannelById( channel );
		}
		return null;
	}
	
	private void loadDefaults() {
		properties.setProperty( "token", "" );
		properties.setProperty( "webms", "" );
		properties.setProperty( "guild-id", "" );
		properties.setProperty( "channel-id", "" );
		properties.setProperty( "admin-ids", "" );
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
