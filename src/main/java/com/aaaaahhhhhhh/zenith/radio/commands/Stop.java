package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stop extends Command {

        private static final String COMMAND_MATCH = "^(stop)$";

        public boolean   isValidCommand(String command) {
            //Get string, match to command, run x
            Pattern pattern = Pattern.compile(COMMAND_MATCH);
            Matcher matcher = pattern.matcher(command);
            return (matcher.matches());
        }

        public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
            if ( admin ) {
				channel.sendMessage( "*I don't feel so good...*" ).queue();
				radio.stop();
			} else {
				channel.sendMessage( "*I can't let you do that...*" ).queue();
			}
        }

        public Stop(DiscordClient discordClient) {
            super(discordClient);
        }
    }

