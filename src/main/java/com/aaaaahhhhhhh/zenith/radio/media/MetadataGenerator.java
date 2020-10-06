package com.aaaaahhhhhhh.zenith.radio.media;

import com.aaaaahhhhhhh.zenith.radio.file.AudioRecord;

public interface MetadataGenerator {
	ShoutMetadata getMetadataFor( AudioRecord record );
}
