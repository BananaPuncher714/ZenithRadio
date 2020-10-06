package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class DirectoryRecord extends FileRecord {
	// I *think* file absolute paths should stay the same...
	// Right...? RIGHT?!
	protected final Map< String, FileRecord > subRecords = new ConcurrentHashMap< String, FileRecord >();
	private final FileValidator validator;
	
	public DirectoryRecord( File file, FileValidator validator ) {
		super( file );
		this.validator = validator;
	}
	
	public UpdateCache updateDirectory() {
		lastModified = file.lastModified();
		
		UpdateCache cache = new UpdateCache();
		
		for ( Iterator< Entry< String, FileRecord > > iterator = subRecords.entrySet().iterator(); iterator.hasNext(); ) {
			Entry< String, FileRecord > entry = iterator.next();
			FileRecord record = entry.getValue();

			if ( record.file.exists() ) {
				if ( record instanceof AudioRecord ) {
					AudioRecord aRecord = ( AudioRecord ) record;
					if ( aRecord.update() ) {
						cache.changed.add( aRecord );
					}
				} else if ( record instanceof DirectoryRecord ) {
					DirectoryRecord dRecord = ( DirectoryRecord ) record;
					cache.merge( dRecord.updateDirectory() );
				}
			} else {
				iterator.remove();
				if ( record instanceof DirectoryRecord ) {
					cache.removed.addAll( ( ( DirectoryRecord ) record ).getAllAudioRecords() );
				} else if ( record instanceof AudioRecord ) {
					cache.removed.add( ( AudioRecord ) record );
				}
			}
		}

		Path filePath = file.toPath();
		try {
			DirectoryStream< Path > dirStream = Files.newDirectoryStream( filePath );
		
			for ( Path subPath: dirStream ) {
				File subItem = subPath.toFile();
				String path = subItem.getAbsolutePath();
	
				// Ignore hidden files
				if ( subItem.getName().startsWith( "." ) ) {
					continue;
				}
	
				if ( !subRecords.containsKey( path ) ) {
					if ( subItem.isDirectory() ) {
						DirectoryRecord newRecord = new DirectoryRecord( subItem, validator );
	
						cache.merge( newRecord.updateDirectory() );
						
						subRecords.put( path, newRecord );
					} else if ( validator.isValidFile( subItem ) ) {
						AudioRecord newRecord = new AudioRecord( subItem );
						newRecord.update();
	
						cache.added.add( newRecord );
	
						subRecords.put( path, newRecord );
					}
				}
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		
		return cache;
	}
	
	@Override
	public boolean update() {
		return updateDirectory().isUnchanged();
	}
	
	public Collection< FileRecord > getSubrecords() {
		return subRecords.values();
	}
	
	public List< AudioRecord > getAllAudioRecords() {
		List< AudioRecord > records = new ArrayList< AudioRecord >();
		
		for ( FileRecord record : subRecords.values() ) {
			if ( record instanceof DirectoryRecord ) {
				records.addAll( ( ( DirectoryRecord ) record ).getAllAudioRecords() );
			} else if ( record instanceof AudioRecord ) {
				records.add( ( AudioRecord ) record );
			}
		}
		
		return records;
	}
	
	public class UpdateCache {
		private List< AudioRecord > removed = new ArrayList< AudioRecord >();
		private List< AudioRecord > changed = new ArrayList< AudioRecord >();
		private List< AudioRecord > added = new ArrayList< AudioRecord >();
		
		public List< AudioRecord > getRemoved() {
			return Collections.unmodifiableList( removed );
		}
		
		public List< AudioRecord > getChanged() {
			return Collections.unmodifiableList( changed );
		}
		
		public List< AudioRecord > getAdded() {
			return Collections.unmodifiableList( added );
		}
		
		protected void merge( UpdateCache other ) {
			removed.addAll( other.removed );
			changed.addAll( other.changed );
			added.addAll( other.added );
		}
		
		public boolean isUnchanged() {
			return removed.isEmpty() && changed.isEmpty() && added.isEmpty();
		}
	}
}
