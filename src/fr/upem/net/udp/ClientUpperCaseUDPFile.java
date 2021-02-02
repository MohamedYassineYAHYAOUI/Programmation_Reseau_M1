package fr.upem.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientUpperCaseUDPFile {

	private static final Charset UTF8 = Charset.forName("UTF8");
	private static final int BUFFER_SIZE = 1024;

	private static void usage() {
		System.out.println("Usage : ClientUpperCaseUDPFile in-filename out-filename timeout host port ");
	}

	public static Thread threadListner(DatagramChannel dc, BlockingQueue<String> blockingQueue, Logger logger) {
		Objects.requireNonNull(dc);
		return new Thread(() -> {
			ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
			try {
				while (!Thread.interrupted()) {
					buff.clear();
					dc.receive(buff);
					buff.flip();
					var msg = UTF8.decode(buff).toString();
					blockingQueue.put(msg);
				}
				return;
			} catch (AsynchronousCloseException e) {
				logger.log(Level.WARNING, "Datagram Channel was closed by another thread", e);
				return;
			} catch (ClosedChannelException e) {
				logger.log(Level.WARNING, "Datagram Channel closed in thread Listner", e);
				return;
			} catch (InterruptedException | IOException e) {
				logger.log(Level.SEVERE, "error in thread Listner", e);
				return;
			}
		});
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 5) {
			usage();
			return;
		}

		String inFilename = args[0];
		String outFilename = args[1];
		int timeout = Integer.valueOf(args[2]);
		String host = args[3];
		int port = Integer.valueOf(args[4]);
		SocketAddress dest = new InetSocketAddress(host, port);

		// Read all lines of inFilename opened in UTF-8
		List<String> lines = Files.readAllLines(Paths.get(inFilename), UTF8);
		ArrayList<String> upperCaseLines = new ArrayList<>();

		// consumer
		var bloquingQueue = new ArrayBlockingQueue<String>(10);
		var logger = Logger.getLogger(ClientUpperCaseUDPFile.class.getName());

		try (DatagramChannel dc = DatagramChannel.open()) {
			dc.bind(null);
			threadListner(dc, bloquingQueue, logger).start();
			for (var line : lines) {
				while (true) {
					ByteBuffer buff = UTF8.encode(line);
					dc.send(buff, dest);

					String msg = bloquingQueue.poll(timeout, TimeUnit.MILLISECONDS);
					if (msg != null) {
						upperCaseLines.add(msg);
						break;
					}
				}
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "error in thread sender(main)", e);
			return;
		}

		// Write upperCaseLines to outFilename in UTF-8
		Files.write(Paths.get(outFilename), upperCaseLines, UTF8, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}
}
