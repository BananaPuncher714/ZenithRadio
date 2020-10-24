package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search extends Command {
    private static final String COMMAND_MATCH = "^search (.+)$";
	public List<AudioRecord> searchList = new ArrayList< AudioRecord >();

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
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
						builder.append( "**" + ( i + 1 ) + "**" + " - " + record.getTrack() + ". " + record.getTitle() + " - **" + record.getAlbum() + "** // " + record.getArtist() );
						if ( i < searchList.size() - 1 ) {
							builder.append( "\n" );
						}
					}
					client.sendMessage(builder.toString(), channel);
				}
			} else {
				channel.sendMessage( "You're going to have to do better than that." ).queue();
			}
    }

	public List<AudioRecord> getSearchList() {
		return searchList;
	}

	public Search(DiscordClient discordClient) {
        super(discordClient);
    }
}
