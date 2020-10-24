package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Play extends Command {
    private static final String COMMAND_MATCH = "^(play|add) (.+)$";
    Search search;

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
        String[] data = command.split( "\\D+" );
			StringBuilder builder = new StringBuilder();
			for ( String v : data ) {
				if ( !v.isEmpty() ) {
					int index = Integer.valueOf( v ) - 1;

					if ( index < 0 || index >= search.getSearchList().size() ) {
						builder.append( "**" + index + "** is *not* a valid selection.\n" );
					} else {
						AudioRecord record = search.getSearchList().get( index );
						String toAppend = "Added **" + record.getTitle() + "** to the playlist.\n";

						builder.append( toAppend );
						radio.getMediaApi().enqueue( record, false );
					}
				}
			}
			if ( builder.length() == 0 ) {
				channel.sendMessage( "That's *not* a valid selection." ).queue();
			} else {
				client.sendMessage(builder.toString(), channel);
			}
    }

    public Play(DiscordClient discordClient, Search search) {
        super(discordClient);
        this.search = search;
    }
}
