package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;

public abstract class FileRecord {
	protected final File file;
	protected long lastModified;
	
	public FileRecord( File file ) {
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}

	public long getLastModified() {
		return lastModified;
	}

	public FileRecord setLastModified( long value ) {
		this.lastModified = value;
		return this;
	}
	
	public abstract boolean update();

	@Override
	public int hashCode() {
		return file.getAbsolutePath().hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileRecord other = (FileRecord) obj;
		return file.getAbsolutePath().equals( other.file.getAbsolutePath() );
	}
}
