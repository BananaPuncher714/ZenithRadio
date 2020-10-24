package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Next extends Command {
    private static final String COMMAND_MATCH = "^(next|skip)$";

    public boolean isValidCommand(String command){
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return(matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
        channel.sendMessage( "Skipping..." ).queue();
        radio.getControlsApi().playNext();
    }

    public Next(DiscordClient discordClient) {
        super(discordClient);
    }
}
