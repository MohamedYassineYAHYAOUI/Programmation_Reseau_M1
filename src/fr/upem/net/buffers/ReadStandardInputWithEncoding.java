package fr.upem.net.buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ReadStandardInputWithEncoding {

	private static final int BUFFER_SIZE = 1024;

	private static void usage() {
		System.out.println("Usage: ReadStandardInputWithEncoding charset");
	}

	private static String stringFromStandardInput(Charset cs) throws IOException {
		try (ReadableByteChannel in = Channels.newChannel(System.in)) {
			var buffer = ByteBuffer.allocate(BUFFER_SIZE);
			
			while (in.read(buffer) != -1) {
				if (!buffer.hasRemaining()) {
					var tmp = ByteBuffer.allocate(buffer.capacity() * 2);
					buffer.flip();
					tmp.put(buffer);
					buffer = tmp;
				}
			}
			buffer.flip();
			//var str = new String(buffer, cs);  //10000   40000 4194304 pk ??
			var str = cs.decode(buffer).toString(); //10000   40000 2208890

			buffer.clear(); // comme on passe pas par JVM, on est obligé de faire le clear / compact ?
			return str;
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		Charset cs = Charset.forName(args[0]);
		System.out.print(stringFromStandardInput(cs));
		
	}

}
