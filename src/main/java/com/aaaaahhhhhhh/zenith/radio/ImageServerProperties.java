package com.aaaaahhhhhhh.zenith.radio;

public class ImageServerProperties {
	boolean enabled;
	String savePath;
	String serverPath;
	String baseUrl;
	int externalPort;
	int internalPort;
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public ImageServerProperties setEnabled( boolean enabled ) {
		this.enabled = enabled;
		return this;
	}
	
	public String getSavePath() {
		return savePath;
	}
	
	public ImageServerProperties setSavePath( String savePath ) {
		this.savePath = savePath;
		return this;
	}
	
	public String getServerPath() {
		return serverPath;
	}
	
	public ImageServerProperties setServerPath( String serverPath ) {
		this.serverPath = serverPath;
		return this;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	
	public ImageServerProperties setBaseUrl( String baseUrl ) {
		this.baseUrl = baseUrl;
		return this;
	}
	
	public int getExternalPort() {
		return externalPort;
	}
	
	public ImageServerProperties setExternalPort( int externalPort ) {
		this.externalPort = externalPort;
		return this;
	}
	
	public int getInternalPort() {
		return internalPort;
	}
	
	public ImageServerProperties setInternalPort( int internalPort ) {
		this.internalPort = internalPort;
		return this;
	}
}
