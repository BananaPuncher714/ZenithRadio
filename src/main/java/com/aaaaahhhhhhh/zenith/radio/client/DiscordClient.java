package com.aaaaahhhhhhh.zenith.radio.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.security.auth.login.LoginException;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

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
				for ( File file : folder.listFiles() ) {
					if ( !file.isDirectory() ) {
						webms.add( file );
					}
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
				onCommand( channel, content.substring( 1 ), isAdmin );
			}
		}
	}
	
	private void onCommand( MessageChannel channel, String command, boolean admin ) {
		ZenithRadio radio = this.radio;
		if ( command.equalsIgnoreCase( "next" ) || command.equalsIgnoreCase( "skip" ) ) {
			channel.sendMessage( "Skipping..." ).queue();
			radio.getControlsApi().playNext();
		} else if ( command.equalsIgnoreCase( "shuffle" ) ) {
			channel.sendMessage( "Shuffling the current playlist..." ).queue();
			radio.getMediaApi().refill();
		} else if ( command.equalsIgnoreCase( "playlist" ) ) {
			StringBuilder builder = new StringBuilder( "**__Current playlist:__**\n" );
			List< AudioRecord > playlist = radio.getMediaApi().getPlaylist();
			for ( int i = 0; i < playlist.size(); i++ ) {
				AudioRecord record = playlist.get( i );
				builder.append( "**" + ( i + 1 ) + ". ** " + record.getTitle() );
				if ( i < playlist.size() - 1 ) {
					builder.append( "\n" );
				}
			}
			
			channel.sendMessage( builder.toString() ).queue();
		} else if ( command.equalsIgnoreCase( "stop" ) ) {
			if ( admin ) {
				channel.sendMessage( "*I don't feel so good...*" ).queue();
				radio.stop();
			} else {
				channel.sendMessage( "*I can't let you do that...*" ).queue();
			}
		} else if ( command.equalsIgnoreCase( "drama" ) ) {
			channel.sendMessage( "What?! You don't want to listen to this?! I'm going to sue you for liable." ).queue();
			AudioRecord playing = radio.getMediaApi().getPlaying();
			radio.getPlaylistManager().getDefaultProvider().addToBlacklist( playing );
			radio.getControlsApi().playNext();
		} else if ( command.equalsIgnoreCase( "time" ) ) {
			long time = radio.getControlsApi().getCurrentTime();
			long length = radio.getControlsApi().getCurrentLength();
			channel.sendMessage( "**__Current time:__** " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( length ) ).queue();
		} else if ( command.startsWith( "search " ) ) {
			String key = command.substring( 7 ).toLowerCase().replaceAll( "\\s+", "" );
			if ( key.length() > 2 ) {
				channel.sendMessage( "Searching..." ).queue();
				searchList.clear();
				for ( AudioRecord record : radio.getMediaApi().getRecords() ) {
					if ( record.getTitle().toLowerCase().contains( key ) ||
							record.getAlbum().toLowerCase().contains( key ) ||
							record.getArtist().toLowerCase().contains( key ) ) {
						searchList.add( record );
					}
				}
				
				if ( searchList.isEmpty() ) {
					channel.sendMessage( "No matches found!" ).queue();
				} else {
					StringBuilder builder = new StringBuilder( "**__Matches(`" + searchList.size() + "`):__**\n" );
					for ( int i = 0; i < Math.min( 100, searchList.size() ); i++ ) {
						AudioRecord record = searchList.get( i );
						builder.append( "**" + ( i + 1 ) + "**" + " - " + record.getAlbum() + " - " + record.getTitle() + " // " + record.getArtist() );
						if ( i < searchList.size() - 1 ) {
							builder.append( "\n" );
						}
					}
					channel.sendMessage( builder.toString() ).queue();
				}
			} else {
				channel.sendMessage( "You're going to have to do better than that." ).queue();
			}
		} else if ( command.matches( "play \\d+" ) ) {
			int index = Integer.valueOf( command.split( "\\s+" )[ 1 ] ) - 1;
			
			if ( index < 0 || index >= searchList.size() ) {
				channel.sendMessage( "That's *not* a valid selection." ).queue();
			} else {
				AudioRecord record = searchList.get( index );
				channel.sendMessage( "Added **" + record.getTitle() + "** to the playlist." ).queue();
				radio.getMediaApi().enqueue( record, false );
			}
		} else {
			channel.sendMessage( "What? Sorry, I don't speak liberal." ).queue();
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
