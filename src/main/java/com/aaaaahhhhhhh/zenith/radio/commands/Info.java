package com.aaaaahhhhhhh.zenith.radio.commands;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.client.DiscordClient;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

import net.dv8tion.jda.api.EmbedBuilder;
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
            
            EmbedBuilder embed = new EmbedBuilder();
			embed.setTitle( "Info", radio.getUrl() );
			embed.setColor( 0xce32ff );
            
			String album = record.getAlbum();
			if ( !album.isEmpty() && !album.equalsIgnoreCase( "unknown" ) ) {
				String baseUrl = String.format( "%s:%s/%s/",
						radio.getImageServerProperties().getBaseUrl(),
						radio.getImageServerProperties().getExternalPort(),
						radio.getImageServerProperties().getServerPath()
				);
				embed.setImage( baseUrl + album.hashCode() + ".png" );
			}
			
			StringBuilder descBuilder = new StringBuilder();
			descBuilder.append( "**Title**: " + record.getTitle() + "\n" );
			descBuilder.append( "**Album**: " + ( record.getAlbum().isEmpty() ? "Unknown" : record.getAlbum() ) + "\n" );
			descBuilder.append( "**Artist**: " + ( record.getArtist().isEmpty() ? "Unknown" : record.getArtist() ) + "\n" );
			descBuilder.append( "**Track**: " );
			if ( !record.getDisc().isEmpty() ) {
				descBuilder.append( record.getDisc() );
				descBuilder.append( "." );
			}
			descBuilder.append( record.getTrack() + "\n" );
			descBuilder.append( "**Length**: " + ZenithRadio.timeFormat( radio.getControlsApi().getCurrentLength() ) + "\n" );

			String prefix = radio.getMusicDirectory().getAbsolutePath();
			String recordFile = record.getFile().getAbsolutePath();
			descBuilder.append( "**File**: `" );
			if ( recordFile.startsWith( prefix ) ) {
				descBuilder.append( recordFile.substring( prefix.length() ) );
			} else {
				descBuilder.append( recordFile );
			}
			descBuilder.append( "`\n" );

			long length = record.getFile().length();

			descBuilder.append( "**Size**: " + client.humanReadableSize( length ) + "\n" );

			long time = radio.getControlsApi().getCurrentTime();
			long totalTime = radio.getControlsApi().getCurrentLength();
			descBuilder.append( "**Current time**: " + ZenithRadio.timeFormat( time ) + "/" + ZenithRadio.timeFormat( totalTime ) );

			embed.setDescription( descBuilder.toString() );
			
			channel.sendMessage( embed.build() ).queue();
    }

    public Info(DiscordClient discordClient) {
        super(discordClient);
    }
}
