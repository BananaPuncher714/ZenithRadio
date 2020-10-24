package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Drama extends Command{
        private static final String COMMAND_MATCH = "^(drama)$";

        public boolean   isValidCommand(String command) {
            //Get string, match to command, run x
            Pattern pattern = Pattern.compile(COMMAND_MATCH);
            Matcher matcher = pattern.matcher(command);
            return (matcher.matches());
        }

        public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
			channel.sendMessage( "What?! You don't want to listen to this?! I'm going to sue you for liable." ).queue();
			AudioRecord playing = radio.getMediaApi().getPlaying();
			radio.getPlaylistManager().getDefaultProvider().addToBlacklist( playing );
			radio.getControlsApi().playNext();
        }

        public Drama(DiscordClient discordClient) {
            super(discordClient);
        }
    }

