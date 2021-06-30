package com.aaaaahhhhhhh.zenith.radio.media;

public class ShoutMetadata {
	private String title;
	private String artist;
	private String url;
	
	public ShoutMetadata( String title, String artist ) {
		this.title = title;
		this.artist = artist;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String setTitle( String title ) {
		String oldTitle = this.title;
		this.title = title;
		return oldTitle;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public String setArtist( String artist ) {
		String oldArtist = this.artist;
		this.artist = artist;
		return oldArtist;
	}

	public String getUrl() {
		return url;
	}

	public ShoutMetadata setUrl( String url ) {
		this.url = url;
		return this;
	}
}
