package fr.upem.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClientIdUpperCaseUDPBurst {

	private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPBurst.class.getName());
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int BUFFER_SIZE = 1024;
	private final List<String> lines;
	private final int nbLines;
	private final String[] upperCaseLines; //
	private final int timeout;
	private final String outFilename;
	private final InetSocketAddress serverAddress;
	private final DatagramChannel dc;
	private final ReceptionLog receptionLog; // BitSet marking received requests

	public static void usage() {
		System.out.println("Usage : ClientIdUpperCaseUDPBurst in-filename out-filename timeout host port ");
	}

	public ClientIdUpperCaseUDPBurst(List<String> lines, int timeout, InetSocketAddress serverAddress,
			String outFilename) throws IOException {
		this.lines = lines;
		this.nbLines = lines.size();
		this.timeout = timeout;
		this.outFilename = outFilename;
		this.serverAddress = serverAddress;
		this.dc = DatagramChannel.open();
		dc.bind(null);
		this.receptionLog = new ReceptionLog(nbLines);
		this.upperCaseLines = new String[nbLines];
	}

	private void senderThreadRun(){
		var sendBuff = ByteBuffer.allocateDirect(BUFFER_SIZE);
		long sendTime = 0;

		try {
			while (!Thread.interrupted()) {
				var current = System.currentTimeMillis();
				var arrayIndex = receptionLog.indexOflinesToSend();
				if (arrayIndex.isEmpty()) {
					logger.info("finished sending all lines");
					break;
				}

				if ((current - sendTime) >= timeout) {
					logger.info("sending packects "+arrayIndex);
					for (var index : arrayIndex) {
						sendBuff.clear();
						sendBuff.putLong(index);
						sendBuff.put(UTF8.encode(lines.get(index)));
						sendBuff.flip();
						dc.send(sendBuff, serverAddress);
					}
					sendTime = System.currentTimeMillis();
				}
			}
		} catch (AsynchronousCloseException e) {
			logger.info("datagramme channel was closed");
		} catch ( IOException e) {
			logger.log(Level.SEVERE, "error in thread sender");
		} finally {
			logger.info("closing thread sender");
		}
	}

	public void launch() throws IOException {
		Thread senderThread = new Thread(this::senderThreadRun);
		senderThread.start();
		var receiveBuff = ByteBuffer.allocateDirect(BUFFER_SIZE);

		try {
			while (!receptionLog.receivedAllPackets()) {
				receiveBuff.clear();
				dc.receive(receiveBuff);
				receiveBuff.flip();
				if (receiveBuff.remaining() < Long.BYTES) {
					continue;
				}
				var id = (int) receiveBuff.getLong();
				if (!receptionLog.packetWasReceived(id)) {
					upperCaseLines[id] = UTF8.decode(receiveBuff).toString();
					receptionLog.receivedPacket(id);
				}
			}
		// no AsynchronousCloseException 
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error in thread Listner(main)", e);
			return;
		}
		senderThread.interrupt();

		Files.write(Paths.get(outFilename), Arrays.asList(upperCaseLines), UTF8, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

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
		InetSocketAddress serverAddress = new InetSocketAddress(host, port);

		// Read all lines of inFilename opened in UTF-8
		List<String> lines = Files.readAllLines(Paths.get(inFilename), UTF8);
		// Create client with the parameters and launch it
		ClientIdUpperCaseUDPBurst client = new ClientIdUpperCaseUDPBurst(lines, timeout, serverAddress, outFilename);
		client.launch();

	}

	private static class ReceptionLog {

		private final BitSet bitSet;
		private final int nbLines;

		ReceptionLog(int nbLines) {
			if (nbLines < 0) {
				throw new IllegalArgumentException("nb lines < 0");
			}
			this.nbLines = nbLines;
			this.bitSet = new BitSet(nbLines);
		}

		List<Integer> indexOflinesToSend() {
			synchronized (bitSet) {
				var indexList = new ArrayList<Integer>();
				for (int i = bitSet.nextClearBit(0); i < nbLines; i = bitSet.nextClearBit(i + 1)) {
					indexList.add(i);
				}
				return indexList;
			}
		}

		boolean packetWasReceived(int id) {
			synchronized (bitSet) {
				return bitSet.get(id);
			}
		}

		void receivedPacket(int id) {
			synchronized (bitSet) {
				bitSet.set(id);
			}
		}

		boolean receivedAllPackets() {
			synchronized (bitSet) {
				return bitSet.cardinality() == nbLines;
			}
		}

	}
}
