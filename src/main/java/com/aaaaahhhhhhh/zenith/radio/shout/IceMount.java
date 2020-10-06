package com.aaaaahhhhhhh.zenith.radio.shout;

public class IceMount {
	protected final String username;
	protected final String serverAddress;
	protected final int port;
	protected final String passwd;
	protected final String mount;
	
	protected boolean useMp3 = true;
	
	protected int bitrate = 320;
	
	protected String name = "Icecast Radio";
	protected String genre = "Alternative";
	protected String description = "VLC Media Player";
	protected String url = "http://www.videolan.org/vlc";
	
	public IceMount( String username, String serverAddress, int port, String passwd, String mount ) {
		this.username = username;
		this.serverAddress = serverAddress;
		this.port = port;
		this.passwd = passwd;
		this.mount = mount;
	}

	public String getUsername() {
		return username;
	}
	
	public String getServerAddress() {
		return serverAddress;
	}

	public int getPort() {
		return port;
	}

	public String getPasswd() {
		return passwd;
	}

	public String getMount() {
		return mount;
	}

	public int getBitrate() {
		return bitrate;
	}

	public IceMount setBitrate( int bitrate ) {
		this.bitrate = bitrate;
		return this;
	}

	public boolean isUseMp3() {
		return useMp3;
	}

	public IceMount setUseMp3( boolean useMp3 ) {
		this.useMp3 = useMp3;
		return this;
	}

	public String getName() {
		return name;
	}

	public IceMount setName( String name ) {
		this.name = name;
		return this;
	}

	public String getGenre() {
		return genre;
	}

	public IceMount setGenre( String genre ) {
		this.genre = genre;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public IceMount setDescription( String description ) {
		this.description = description;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public IceMount setUrl( String url ) {
		this.url = url;
		return this;
	}
}
