package fr.upem.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientUpperCaseUDPTimeout {

	public static final int BUFFER_SIZE = 1024;
	private static final int BLOCKING_QUEUE_CAPACITY = 5;

	private static void usage() {
		System.out.println("Usage : NetcatUDP host port charset");
	}

	public static Thread threadListner(DatagramChannel dc, Charset cs, BlockingQueue<String> blockingQueue,
			Logger logger) {
		Objects.requireNonNull(dc);
		return new Thread(() -> {
			ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
			try {
				while (!Thread.interrupted()) {
					buff.clear();
					dc.receive(buff);
					buff.flip();
					var msg = cs.decode(buff).toString();
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

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}

		InetSocketAddress server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		Charset cs = Charset.forName(args[2]);
		System.out.println(args[2]);

		var bloquingQueue = new ArrayBlockingQueue<String>(BLOCKING_QUEUE_CAPACITY);

		var logger = Logger.getLogger(ClientUpperCaseUDPRetry.class.getName());

		try (Scanner scan = new Scanner(System.in); DatagramChannel dc = DatagramChannel.open();) {
			dc.bind(null);
			threadListner(dc, cs, bloquingQueue, logger).start();

			while (scan.hasNextLine()) {
				String line = scan.nextLine();

				ByteBuffer buff = cs.encode(line);
				dc.send(buff, server);

				String msg = bloquingQueue.poll(1, TimeUnit.SECONDS);
				if (msg == null) {
					System.out.println("server did not respond in time");
				}else {
					System.out.println("message: "+msg);
				}
			}
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "error in thread sender(main)", e);
			return;
		}

	}
}
