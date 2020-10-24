package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Time extends Command{
        private static final String COMMAND_MATCH = "^(time)$";

        public boolean   isValidCommand(String command) {
            //Get string, match to command, run x
            Pattern pattern = Pattern.compile(COMMAND_MATCH);
            Matcher matcher = pattern.matcher(command);
            return (matcher.matches());
        }

        public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
            long time = radio.getControlsApi().getCurrentTime();
			long length = radio.getControlsApi().getCurrentLength();
			channel.sendMessage( "**__Current time:__** " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( length ) ).queue();
        }

        public Time(DiscordClient discordClient) {
            super(discordClient);
        }
    }

