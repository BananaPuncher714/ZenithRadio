package com.aaaaahhhhhhh.zenith.radio;

public class ExternalUpdateProperties {
	boolean enabled = false;
	String url = "";
	String userpass = "";
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public ExternalUpdateProperties setEnabled( boolean enabled ) {
		this.enabled = enabled;
		return this;
	}
	
	public String getUrl() {
		return url;
	}
	
	public ExternalUpdateProperties setUrl( String url ) {
		this.url = url;
		return this;
	}
	
	public String getUserpass() {
		return userpass;
	}
	
	public ExternalUpdateProperties setUserpass( String userpass ) {
		this.userpass = userpass;
		return this;
	}
}
