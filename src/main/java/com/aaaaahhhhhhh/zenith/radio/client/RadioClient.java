package com.aaaaahhhhhhh.zenith.radio.client;

import com.aaaaahhhhhhh.zenith.radio.ZenithRadio;
import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public abstract class RadioClient {
	public abstract void onAttach( ZenithRadio radio );
	public abstract void onDetach( ZenithRadio radio );
	public void onMediaChange( ZenithRadio radio, AudioRecord record ) {}
	
	public void stop() {}
}
