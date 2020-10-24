package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import net.dv8tion.jda.api.entities.MessageChannel;
import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Refresh extends Command {
    private static final String COMMAND_MATCH = "^(refresh)$";

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
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
    }

    public Refresh(DiscordClient discordClient) {
        super(discordClient);
    }
}
