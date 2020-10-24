package com.aaaaahhhhhhh.zenith.radio.Commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Info extends Command {
    private static final String COMMAND_MATCH = "^(info)$";

    public boolean   isValidCommand(String command) {
        //Get string, match to command, run x
        Pattern pattern = Pattern.compile(COMMAND_MATCH);
        Matcher matcher = pattern.matcher(command);
        return (matcher.matches());
    }

    public void runCommand(MessageChannel channel, boolean admin, String command, ZenithRadio radio){
            AudioRecord record = radio.getMediaApi().getPlaying();
			MessageBuilder builder = new MessageBuilder();
			builder.append( "**__Now playing__**\n" );
			builder.append( "Title: " + record.getTitle() + "\n" );
			builder.append( "Album: " + ( record.getAlbum().isEmpty() ? "Unknown" : record.getAlbum() ) + "\n" );
			builder.append( "Artist: " + ( record.getArtist().isEmpty() ? "Unknown" : record.getArtist() ) + "\n" );
			builder.append( "Track: " );
			if ( !record.getDisc().isEmpty() ) {
				builder.append( record.getDisc() );
				builder.append( "." );
			}
			builder.append( record.getTrack() + "\n" );

			String prefix = radio.getMusicDirectory().getAbsolutePath();
			String recordFile = record.getFile().getAbsolutePath();
			builder.append( "File: `" );
			if ( recordFile.startsWith( prefix ) ) {
				builder.append( recordFile.substring( prefix.length() ) );
			} else {
				builder.append( recordFile );
			}
			builder.append( "`\n" );

			long length = record.getFile().length();

			builder.append( "Size: " + client.humanReadableSize( length ) + "\n" );

			long time = radio.getControlsApi().getCurrentTime();
			long totalTime = radio.getControlsApi().getCurrentLength();
			builder.append( "Current time: " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( totalTime ) );

			channel.sendMessage( builder.build() ).queue();
    }

    public Info(DiscordClient discordClient) {
        super(discordClient);
    }
}
