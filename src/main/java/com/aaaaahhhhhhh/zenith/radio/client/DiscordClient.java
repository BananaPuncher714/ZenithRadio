package com.aaaaahhhhhhh.zenith.radio.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.security.auth.login.LoginException;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import com.aaaaahhhhhhh.zenith.radio.Commands.*;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordClient extends RadioClient {
	private final File homeDirectory;
	private ZenithRadio radio;
	private JDA api;
	private final DiscordProperties properties;
	Command[] commands;


	private List< AudioRecord > searchList = new ArrayList< AudioRecord >();
	
	public DiscordClient( File baseDir ) {
		homeDirectory = baseDir;
		homeDirectory.mkdirs();
		File configFile = new File( homeDirectory, "config.properties" );
		this.properties = new DiscordProperties( configFile );


		String token = properties.getToken();
		if ( !token.isEmpty() ) {
			ZenithRadio.debug( "Starting discord bot..." );
			try {
				api = JDABuilder.createDefault( token ).addEventListeners( new ListenerAdapter() {
					public void onMessageReceived( MessageReceivedEvent event ) {
						onMessage( event );
					}
				} ).build();
			} catch ( LoginException e ) {
				e.printStackTrace();
			}
		} else {
			ZenithRadio.debug( "Unable to find a bot token!" );
		}

		initCommands();
	}

	private void initCommands(){
		Search search = new Search(this);
		commands = new Command[]{
				new Next(this),
				new Drama(this),
				new Info(this),
				new Play(this, search),
				new Playlist(this),
				new Refresh(this),
				search,
				new Shuffle(this),
				new Stop(this),
				new Time(this),
				new Addall(this, search)
		};


	}
	
	@Override
	public void onAttach( ZenithRadio radio ) {
		this.radio = radio;
		
		ZenithRadio.debug( "Attached discord client to Zenith Radio" );
	}

	@Override
	public void onDetach( ZenithRadio radio ) {
		ZenithRadio.debug( "Detached discord client from Zenith Radio" );
		
		radio = null;
	}
	
	private void onMessage( MessageReceivedEvent event ) {
		User author = event.getAuthor();
		
		if ( author.isBot() ) return;

		Message message = event.getMessage();
		String content = message.getContentRaw();
		
		if ( !event.isFromGuild() ) {
			File folder = properties.getWebmFolder();
			if ( folder != null ) {
				List< File > webms = new ArrayList< File >();
				
				Path filePath = folder.toPath();
				try {
					DirectoryStream< Path > dirStream = Files.newDirectoryStream( filePath );
					for ( Path subPath: dirStream ) {
						File file = subPath.toFile();
						if ( !file.isDirectory() ) {
							webms.add( file );
						}
					}
				} catch ( IOException e ) {
					e.printStackTrace();
				}
				
				
				if ( !webms.isEmpty() ) {
					File file = webms.get( ThreadLocalRandom.current().nextInt( webms.size() ) );
					event.getChannel().sendFile( file ).queue();
				} else {
					event.getChannel().sendMessage( "DOOM" ).queue();
				}
			} else {
				event.getChannel().sendMessage( "DOOM" ).queue();
			}
			return;
		}
		
		boolean isAdmin = properties.isAdmin( author.getId() );
		if ( content.equals( "Onestcast is a liberal" ) ) {
			if ( isAdmin ) {
				message.getChannel().sendMessage( "I couldn't agree more" ).queue();
				properties.getProperties().setProperty( "guild-id", event.getGuild().getId() );
				properties.getProperties().setProperty( "channel-id", event.getChannel().getId() );
				properties.save();
			} else {
				message.getChannel().sendMessage( "**DOOOOM**" ).queue();
			}
		} else if ( content.startsWith( "$" ) ) {
			MessageChannel channel = properties.getChannel( api );
			if ( channel == null ) {
				message.getChannel().sendMessage( "SUFFERING" ).queue();;
			} else if ( channel.getIdLong() == event.getChannel().getIdLong() ) {
				onCommand( channel, content.substring( 1 ).toLowerCase(), isAdmin );
			}
		}
	}
	
	private void onCommand( MessageChannel channel, String command, boolean admin ) {
		ZenithRadio radio = this.radio;
		boolean success = false;
		for (Command com : commands) {
			if (com.isValidCommand(command)) {
				success = true;
				com.runCommand(channel, admin, command, radio);
			}

		}
		if(!success){
				channel.sendMessage("What? Sorry, I don't speak liberal.").queue();
		}
	}

	public void sendMessage(String string, MessageChannel channel){
		string = string + "\n";
		int times = string.length()/2000;
		int start = 0;
		for (int i = 1; i<=times+1; i++){
				int end = string.lastIndexOf("\n", start+2000);
				channel.sendMessage(string.substring(start, end)).queue();
				start =  end;
		}
	}

	public String humanReadableSize( long size ) {
		if ( size >= 1_000_000_000 ) {
			return ( ( size / 1_000_000_00 ) / 10.0 ) + "GB";
		} else if ( size >= 1_000_000 ) {
			return ( ( size / 1_000_00 ) / 10.0 ) + "MB";
		} else if ( size >= 1_000 ) {
			return ( ( size / 1_00 ) / 10.0 ) + "KB";
		} else {
			return size + "B";
		}
	}
	
	@Override
	public void onMediaChange( ZenithRadio radio, AudioRecord record ) {
		if ( api != null ) {
			MessageChannel channel = properties.getChannel( api );
			if ( channel != null ) {
				radio.submit( () -> {
					try {
						Thread.sleep( 200 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
					
					MessageBuilder builder = new MessageBuilder();
					builder.append( "**__Now playing__**\n" );
					builder.append( "Title: " + record.getTitle() + "\n" );
					builder.append( "Album: " + ( record.getAlbum().isEmpty() ? "Unknown" : record.getAlbum() ) + "\n" );
					builder.append( "Artist: " + ( record.getArtist().isEmpty() ? "Unknown" : record.getArtist() ) + "\n" );
					builder.append( "Track: " );
					if ( !record.getDisc().isEmpty() ) {
						builder.append( record.getDisc() );
						builder.append( "." );
					}
					builder.append( record.getTrack() + "\n" );
					builder.append( "Length: " + ZenithRadio.timeFormat( radio.getControlsApi().getCurrentLength() ) );
					
					channel.sendMessage( builder.build() ).queue();
				} );
			}
		}
	}
	
	@Override
	public void stop() {
		if ( api != null ) {
			api.shutdown();
		}
		properties.save();
	}
}
