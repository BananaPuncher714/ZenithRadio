package com.aaaaahhhhhhh.zenith.radio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.aaaaahhhhhhh.zenith.radio.util.BinaryReader;
import com.aaaaahhhhhhh.zenith.radio.util.BinaryWriter;

public class MusicCacheIO {
	public static MusicCache read( File file, FileValidator validator ) throws IOException {
		FileInputStream stream = new FileInputStream( file );
		byte[] arr = IOUtils.toByteArray( stream );
		BinaryReader reader = new BinaryReader( arr );
		
		String header = reader.getString( 2 );
		if ( !header.equals( "DR" ) ) {
			throw new IllegalArgumentException( "Not a directory record file!" );
		}
		
		int format = reader.getInt16Little();
		if ( format == 1 ) {
			return readFormat1( reader, validator );
		}
		
		throw new IllegalArgumentException( "Unsupported format version!" );
	}
	
	public static void write( MusicCache cache, File file ) throws IOException {
		BinaryWriter writer = new BinaryWriter( 2048 );
		// DR - 2 bytes
		// format - 2 bytes
		writer.write( "DR", 2 );
		
		writeFormat1( cache.record, writer );

		FileOutputStream fos = new FileOutputStream( file );
		fos.write( writer.toByteArray() );
		fos.close();
	}
	
	private static MusicCache readFormat1( BinaryReader reader, FileValidator validator ) {
		int recordCount = reader.getInt32Little();
		
		FileRecord[] records = new FileRecord[ recordCount ];
		for ( int i = 0; i < recordCount; i++ ) {
			int parentIndex = reader.getInt32Little();
			int type = reader.getInt8();

			String mrl = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
			long lastModified = reader.getLong64Little();
			
			File file = new File( mrl );
			
			FileRecord record;
			if ( type == 0 ) {
				record = new DirectoryRecord( file, validator );
			} else if ( type == 1 ) {
				AudioRecord aRecord = new AudioRecord( file );
				record = aRecord;
				
				aRecord.title = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
				aRecord.album = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
				aRecord.artist = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
				aRecord.track = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
				aRecord.disc = reader.getString( reader.getInt16Little(), StandardCharsets.UTF_8 );
			} else {
				throw new IllegalArgumentException( "Encountered invalid type "  + type );
			}
			
			record.setLastModified( lastModified );
			
			records[ i ] = record;
			
			if ( parentIndex != i ) {
				FileRecord parent = records[ parentIndex ];
				if ( parent instanceof DirectoryRecord ) {
					DirectoryRecord dParent = ( DirectoryRecord ) parent;
					dParent.subRecords.put( file.getAbsolutePath(), record );
				} else {
					throw new IllegalArgumentException( "Parent record is not a DirectoryRecord!" );
				}
			}
		}
		
		if ( records[ 0 ] instanceof DirectoryRecord ) {
			return new MusicCache( ( DirectoryRecord ) records[ 0 ] );
		} else {
			throw new IllegalArgumentException( "The first record must be a directory record!" );
		}
	}
	
	private static void writeFormat1( DirectoryRecord record, BinaryWriter writer ) {
		// Format 1
		writer.writeLittle( 1, 2 );
		
		// Amount of records - 4 bytes
		// Parent index - 4 bytes
		// Type -  1 byte
		// MRL length - 2 bytes
		// MRL
		// Date modified - 8 bytes
		// Extra data - ?
		Map< FileRecord, FileRecord > parent = new HashMap< FileRecord, FileRecord >();
		parent.put( record, record );
		
		List< RecordPair > pairs = new LinkedList< RecordPair >();
		Deque< DirectoryRecord > queue = new ArrayDeque< DirectoryRecord >();
		pairs.add( new RecordPair( record, record ) );
		queue.add( record );
		while ( !queue.isEmpty() ) {
			DirectoryRecord r = queue.poll();

			for ( FileRecord subItem : r.getSubrecords() ) {
				pairs.add( new RecordPair( subItem, r ) );
				parent.put( subItem, r );
				
				if ( subItem instanceof DirectoryRecord ) {
					queue.add( ( DirectoryRecord ) subItem );
				}
			}
		}
		
		// Amount of records
		writer.writeLittle( parent.size(), 4 );
		
		int currentIndex = 0;
		Map< FileRecord, Integer > indexes = new HashMap< FileRecord, Integer >();
		indexes.put( record, 0 );
		
		for ( RecordPair pair : pairs ) {
			FileRecord mainRecord = pair.key;
			FileRecord parentRecord = pair.value;
			
			// Set the parent record's index
			writer.writeLittle( indexes.get( parentRecord ), 4 );
			
			// Set the type of record
			if ( mainRecord instanceof DirectoryRecord ) {
				writer.writeLittle( 0, 1 );
			} else if ( mainRecord instanceof AudioRecord ) {
				writer.writeLittle( 1, 1 );
			}
			
			// Set the mrl
			byte[] mrl = mainRecord.getFile().getAbsolutePath().getBytes( StandardCharsets.UTF_8 );
			writer.writeLittle( mrl.length, 2 );
			writer.write( mrl );
			
			// Set the date modified
			writer.writeLittle( mainRecord.getLastModified(), 8 );
			
			if ( mainRecord instanceof DirectoryRecord ) {
				// No extra data
			} else if ( mainRecord instanceof AudioRecord ) {
				AudioRecord aRecord = ( AudioRecord ) mainRecord;
				// Yes extra data
				byte[] title = aRecord.getTitle().getBytes( StandardCharsets.UTF_8 );
				byte[] album = aRecord.getAlbum().getBytes( StandardCharsets.UTF_8 );
				byte[] artist = aRecord.getArtist().getBytes( StandardCharsets.UTF_8 );
				byte[] track = aRecord.getTrack().getBytes( StandardCharsets.UTF_8 );
				byte[] disc = aRecord.getDisc().getBytes( StandardCharsets.UTF_8 );

				writer.writeLittle( title.length, 2 );
				writer.write( title );
				writer.writeLittle( album.length, 2 );
				writer.write( album );
				writer.writeLittle( artist.length, 2 );
				writer.write( artist );
				writer.writeLittle( track.length, 2 );
				writer.write( track );
				writer.writeLittle( disc.length, 2 );
				writer.write( disc );
			}
			
			indexes.put( mainRecord, currentIndex++ );
		}
	}
	
	private static class RecordPair {
		FileRecord key;
		FileRecord value;
		
		public RecordPair( FileRecord key, FileRecord value ) {
			this.key = key;
			this.value = value;
		}
	}
}
