package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Playlist extends Command{
    private static final String COMMAND_MATCH = "^(playlist)$";

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){

        StringBuilder builder = new StringBuilder( "**__Current playlist:__**\n" );
			List<AudioRecord> playlist = radio.getMediaApi().getPlaylist();
			for ( int i = 0; i < playlist.size(); i++ ) {
				AudioRecord record = playlist.get( i );
				String toAppend = "**" + ( i + 1 ) + ". ** " + record.getTitle();
				builder.append( toAppend );
				if ( i < playlist.size() - 1 ) {
					builder.append( "\n" );
				}
			}

			client.sendMessage(builder.toString(), channel);
    }

	public Playlist(DiscordClient discordClient) {
		super(discordClient);
	}
}
