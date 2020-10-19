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
import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;

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
				
				String toAppend = "**" + ( i + 1 ) + ". ** " + record.getTitle();
				
				if ( builder.length() + toAppend.length() > 2000 ) {
					channel.sendMessage( builder.toString() ).queue();
					builder = new StringBuilder();
				}
				
				builder.append( toAppend );
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
					if ( record.getTitle().replaceAll( "\\s+", "" ).toLowerCase().contains( key ) ||
							record.getAlbum().replaceAll( "\\s+", "" ).toLowerCase().contains( key ) ||
							record.getArtist().replaceAll( "\\s+", "" ).toLowerCase().contains( key ) ) {
						searchList.add( record );
					}
				}
				
				// TODO Use a treeset and make AudioRecords comparable or something
				searchList.sort( ( a1, a2 ) -> {
					String[] a1s = {
							a1.getAlbum(),
							a1.getDisc(),
							a1.getTrack(),
							a1.getTitle(),
							a1.getArtist(),
							a1.getFile().getAbsolutePath()
					};
					String[] a2s = {
							a2.getAlbum(),
							a2.getDisc(),
							a2.getTrack(),
							a2.getTitle(),
							a2.getArtist(),
							a2.getFile().getAbsolutePath()
					};
					
					for ( int i = 0; i < a1s.length; i++ ) {
						String a1str = a1s[ i ];
						String a2str = a2s[ i ];
						
						try {
							int int1 = Integer.parseInt( a1str );
							int int2 = Integer.parseInt( a2str );
							
							if ( int1 > int2 ) {
								return 1;
							} else if ( int1 < int2 ) {
								return -1;
							}
						} catch ( NumberFormatException e ) {
							int res = a1str.compareTo( a2str );
							if ( res != 0 ) {
								return res;
							}
						}
					}
					
					return 0;
				} );
				
				if ( searchList.isEmpty() ) {
					channel.sendMessage( "No matches found!" ).queue();
				} else {
					StringBuilder builder = new StringBuilder( "**__Matches(" + searchList.size() + "):__**\n" );
					for ( int i = 0; i < Math.min( 100, searchList.size() ); i++ ) {
						AudioRecord record = searchList.get( i );
						String toAppend = "**" + ( i + 1 ) + "**" + " - " + record.getTrack() + ". " + record.getTitle() + " - **" + record.getAlbum() + "** // " + record.getArtist();
						
						// Split it into multiple messages
						if ( builder.length() + toAppend.length() > 2000 ) {
							channel.sendMessage( builder.toString() ).queue();
							builder = new StringBuilder();
						}
						
						builder.append( "**" + ( i + 1 ) + "**" + " - " + record.getTrack() + ". " + record.getTitle() + " - **" + record.getAlbum() + "** // " + record.getArtist() );
						if ( i < searchList.size() - 1 ) {
							builder.append( "\n" );
						}
					}
					channel.sendMessage( builder.toString() ).queue();
				}
			} else {
				channel.sendMessage( "You're going to have to do better than that." ).queue();
			}
		} else if ( command.startsWith( "play " ) || command.startsWith( "add " ) ) {
			String[] data = command.split( "\\D+" );
			StringBuilder builder = new StringBuilder();
			for ( String v : data ) {
				if ( !v.isEmpty() ) {
					int index = Integer.valueOf( v ) - 1;
					
					if ( index < 0 || index >= searchList.size() ) {
						builder.append( "**" + index + "** is *not* a valid selection.\n" );
					} else {
						AudioRecord record = searchList.get( index );
						// TODO Refactor this so it doesn't break or look so bad
						String toAppend = "Added **" + record.getTitle() + "** to the playlist.\n";
						if ( builder.length() + toAppend.length() > 2000 ) {
							channel.sendMessage( builder.toString() ).queue();
							builder = new StringBuilder();
						}
						
						builder.append( toAppend );
						radio.getMediaApi().enqueue( record, false );
					}
				}
			}
			if ( builder.length() == 0 ) {
				channel.sendMessage( "That's *not* a valid selection." ).queue();
			} else {
				channel.sendMessage( builder.toString() ).queue();
			}
		} else if ( command.matches( "addall" ) ) {
			if ( searchList.isEmpty() ) {
				channel.sendMessage( "There isn't anything to add!" ).queue();
			} else {
				StringBuilder builder = new StringBuilder();
				for ( int i = 0; i < searchList.size(); i++ ) {
					AudioRecord record = searchList.get( i );
					String toAppend = "Added **" + record.getTitle() + "** to the playlist.";

					if ( builder.length() + toAppend.length() > 2000 ) {
						channel.sendMessage( builder.toString() ).queue();
						builder = new StringBuilder();
					}
					
					radio.getMediaApi().enqueue( record, false );
					builder.append( toAppend );
					if ( i < searchList.size() - 1 ) {
						builder.append( "\n" );
					}
				}
				channel.sendMessage( builder.toString() ).queue();
			}
		} else if ( command.matches( "refresh" ) ) {
			channel.sendMessage( "Refreshing the music cache..." ).queue();
			UpdateCache cache = radio.getMediaApi().refresh();
			if ( cache.isUnchanged() ) {
				channel.sendMessage( "No changes detected!" ).queue();
			} else {
				StringBuilder builder = new StringBuilder();
				if ( !cache.getAdded().isEmpty() ) {
					builder.append( "Added **" + cache.getAdded().size() + "** tracks\n" );
				}
				if ( !cache.getChanged().isEmpty() ) {
					builder.append( "Updated **" + cache.getChanged().size() + "** tracks\n" );
				}
				if ( !cache.getRemoved().isEmpty() ) {
					builder.append( "Removed **" + cache.getRemoved().size() + "** tracks\n" );
				}
				channel.sendMessage( builder.toString() ).queue();
			}
		} else if ( command.matches( "info" ) ) {
			AudioRecord record = radio.getMediaApi().getPlaying();
			
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
			
			String prefix = radio.getMusicDirectory().getAbsolutePath();
			String recordFile = record.getFile().getAbsolutePath();
			builder.append( "File: `" );
			if ( recordFile.startsWith( prefix ) ) {
				builder.append( recordFile.substring( prefix.length() ) );
			} else {
				builder.append( recordFile );
			}
			builder.append( "`\n" );
			
			long length = record.getFile().length();
			
			builder.append( "Size: " + humanReadableSize( length ) + "\n" );
			
			long time = radio.getControlsApi().getCurrentTime();
			long totalTime = radio.getControlsApi().getCurrentLength();
			builder.append( "Current time: " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( totalTime ) );
			
			channel.sendMessage( builder.build() ).queue();
		} else {
			channel.sendMessage( "What? Sorry, I don't speak liberal." ).queue();
		}
	}
	
	private String humanReadableSize( long size ) {
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
