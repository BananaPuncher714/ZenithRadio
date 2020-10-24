package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    DiscordClient client;

    public Command(DiscordClient discordClient) {
        client = discordClient;
    }

    public abstract boolean isValidCommand(String command);
    public abstract void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio);



    public Command() {
    }


}
