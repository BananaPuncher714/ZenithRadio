package com.aaaaahhhhhhh.zenith.radio.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniHttpd {
	private volatile boolean running = false;
	
	private final int port;
	private final ServerSocket socket;
	
	private FileSupplier supplier;
	
	public MiniHttpd( int port, FileSupplier supplier ) throws IOException {
		this.port = port;
		this.supplier = supplier;
		
		socket = new ServerSocket( port );
		socket.setReuseAddress( true );
	}
	
	public int getPort() {
		return port;
	}
	
	public void run() {
		running = true;
		while ( running ) {
			try {
				new Thread( new HttpdConnection( this, socket.accept() )::run ).start();
			} catch ( IOException e ) {
			}
		}
		if ( !socket.isClosed() ) {
			try {
				socket.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		running = false;
		if ( !socket.isClosed() ) {
			try {
				socket.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	public interface FileSupplier {
		File request( String path, InetAddress address );
	}
	
	public class HttpdConnection {
		protected final MiniHttpd server;
		protected final Socket client;

		public HttpdConnection( MiniHttpd server, Socket client ) {
			this.server = server;
			this.client = client;
		}

		public void run() {
			try {
				BufferedReader in = new BufferedReader( new InputStreamReader( client.getInputStream(), StandardCharsets.UTF_8 ) );
				OutputStream out = client.getOutputStream();
				String request = in.readLine();
				Matcher get = Pattern.compile("GET /?(\\S*).*").matcher( request );
				if ( get.matches() ) {
					request = get.group( 1 );

					File result = supplier.request( request, client.getInetAddress() );

					if ( result == null ) {
						out.write( "HTTP/1.0 400 Bad Request\r\n".getBytes() );
					} else {
						try {
							// Writes zip files specifically; Designed for resource pack hosting
							FileInputStream fis = new FileInputStream ( result );
							out.write( "HTTP/1.0 200 OK\r\n".getBytes() );
							out.write( "Content-Type: image/png\r\n".getBytes() );
							out.write( ( "Content-Length: " + result.length() + "\r\n" ).getBytes() );
							out.write( ( "Date: " + new Date().toGMTString() + "\r\n" ).getBytes() );
							out.write( "Server: MiniHttpd\r\n\r\n".getBytes() );
							byte [] data = new byte [ 64*1024 ];
							for( int read; ( read = fis.read( data ) ) > -1; ) {
								out.write( data, 0, read );
							}
							out.flush();
							fis.close();
						} catch ( FileNotFoundException e ) {
							out.write( "HTTP/1.0 404 Object Not Found\r\n".getBytes() );
						}
					}
				} else {
					out.write( "HTTP/1.0 400 Bad Request\r\n".getBytes() );
				}
				out.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			
			try {
				client.close();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
}
