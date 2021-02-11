package fr.upem.net.udp;

import java.util.BitSet;
import java.util.HashMap;
import java.util.logging.Logger;


import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


public class ServerLongSumRev {

	private static final Logger logger = Logger.getLogger(ServerLongSumRev.class.getName());
	private static final int BUFFER_SIZE = 1024;
	private static final int OP_PACKET_SIZE = (Long.BYTES * 4) +Byte.BYTES;
	
	private final DatagramChannel dc;
	private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);

	private final HashMap<InetSocketAddress, HashMap<Long, Context>> packethistory;

	public ServerLongSumRev(int port) throws IOException {
		dc = DatagramChannel.open();
		dc.bind(new InetSocketAddress(port));

		this.packethistory = new HashMap<>();
		logger.info("ServerLongSumRev started on port " + port);
	}
	
	
	private long sessionID(ByteBuffer bb) {
		if(bb.remaining() <OP_PACKET_SIZE ) {
			throw new IllegalStateException("req ");
		}
		if(bb.get() != 1) {
			throw new IllegalStateException("req OP ");
		}
		return bb.getLong();
	}

	public void serve() throws IOException {
		while (!Thread.interrupted()) {
			buff.clear();
			var exp = (InetSocketAddress) dc.receive(buff);
			buff.flip();
			var sessionId = sessionID(buff);
			var idPosOper = (int) buff.getLong();
			var totalOper = (int) buff.getLong();
			var opValue = buff.getLong();
			
			
			var client = packethistory.get(exp);
			if( client == null) { // new Client
				var clientData = new HashMap<Long, Context>();
				var context = new Context(totalOper);
				context.receivedOper(idPosOper, opValue);
				clientData.put(sessionId,context );
				packethistory.put(exp, clientData);
			}else {
				var session =client.get(sessionId);
				if( session == null) { // new session
					var context = new Context(totalOper);
					context.receivedOper(idPosOper, opValue);
					client.put(sessionId,context );
				}else {
					session.receivedOper(idPosOper, opValue);
				}
			}
			
			buff.clear();
			var client2 = packethistory.get(exp).get(sessionId);
			if(client2.receivedAllOper()) { // res
				buff.put(Integer.valueOf(3).byteValue());
				buff.putLong(sessionId);
				buff.putLong(client2.sum);
			}else { // Ack
				buff.put(Integer.valueOf(2).byteValue());
				buff.putLong(sessionId);
				buff.putLong(idPosOper);
			}
			buff.flip();
			dc.send(buff, exp);
		}

	}

	public static void usage() {
		System.out.println("Usage : ServerLongSumRev port");
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		ServerLongSumRev server;
		int port = Integer.valueOf(args[0]);
		if (!(port >= 1024) & port <= 65535) {
			logger.severe("The port number must be between 1024 and 65535");
			return;
		}
		try {
			server = new ServerLongSumRev(port);
		} catch (BindException e) {
			logger.severe("Server could not bind on " + port + "\nAnother server is probably running on this port.");
			return;
		}
		server.serve();
	}

	class Context {
		private long sum;
		private final int totalOper;
		private final BitSet bitSet;

		public Context(int totalOper) {
			this.bitSet = new BitSet(totalOper);
			this.totalOper = totalOper;
			this.sum = 0;
		}
		
		void receivedOper(int operId, long operVlaue){
			if(operId< totalOper && !bitSet.get(operId)) {
				bitSet.flip(operId);
				sum += operVlaue;
			}
		}
		
		boolean receivedAllOper() {
			return bitSet.cardinality() == totalOper;
		}

	}

}