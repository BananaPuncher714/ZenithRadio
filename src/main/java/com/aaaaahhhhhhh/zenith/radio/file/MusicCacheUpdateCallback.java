package com.aaaaahhhhhhh.zenith.radio.file;

import com.aaaaahhhhhhh.zenith.radio.file.DirectoryRecord.UpdateCache;

public interface MusicCacheUpdateCallback {
	void onMusicCacheUpdate( MusicCache cache, UpdateCache update );
}
