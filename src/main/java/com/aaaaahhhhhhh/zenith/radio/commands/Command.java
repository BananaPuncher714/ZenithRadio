package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import net.dv8tion.jda.api.entities.MessageChannel;

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
