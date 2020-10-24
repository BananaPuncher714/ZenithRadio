package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Addall extends Command {
    private static final String COMMAND_MATCH = "^(addall)$";
    Search search;

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
        if ( search.getSearchList().isEmpty() ) {
				channel.sendMessage( "There isn't anything to add!" ).queue();
			} else {
				StringBuilder builder = new StringBuilder();
				for ( int i = 0; i < search.getSearchList().size(); i++ ) {
					AudioRecord record = search.getSearchList().get( i );
					String toAppend = "Added **" + record.getTitle() + "** to the playlist.";

					radio.getMediaApi().enqueue( record, false );
					builder.append( toAppend );
					if ( i < search.getSearchList().size() - 1 ) {
						builder.append( "\n" );
					}
				}
				client.sendMessage(builder.toString(),channel);
			}
    }

    public Addall(DiscordClient discordClient, Search search) {
        super(discordClient);
        this.search =  search;
    }
}
