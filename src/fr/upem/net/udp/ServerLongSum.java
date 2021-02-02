package fr.upem.net.udp;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public class ServerLongSum {
	
	class ServerLongSumClientData {
		private final BitSet packetBitSet;
		private final ArrayList<Integer> values; 
		private final int totalOper;
		
		ServerLongSumClientData(int totalOper){
			this.packetBitSet = new BitSet(totalOper);
			this.values = new ArrayList<>();
			this.totalOper = totalOper;
		}
		
		boolean receivedAllPackets() {
			return packetBitSet.cardinality() == totalOper;
		}

	}
	
	private enum PacketType {OP, ACK, RES}
	
	private static final Logger logger = Logger.getLogger(ServerIdUpperCaseUDP.class.getName());
    private static final int SEND_BUFFER_SIZE = (2*Long.BYTES)+Byte.BYTES;
    private static final int RECIEVE_BUFFER_SIZE = (4*Long.BYTES)+Byte.BYTES;
    private final DatagramChannel dc;
    private final ByteBuffer buffRec = ByteBuffer.allocateDirect(RECIEVE_BUFFER_SIZE);
    private final ByteBuffer buffSend = ByteBuffer.allocateDirect(SEND_BUFFER_SIZE);
    private final HashMap<InetSocketAddress, HashMap<Integer, ServerLongSumClientData >> packetLogger;
    

    public ServerLongSum(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        this.packetLogger = new HashMap<InetSocketAddress, HashMap<Integer, ServerLongSumClientData >> ();
        logger.info("ServerBetterUpperCaseUDP started on port " + port);
    }

    
    /**
     * get the session ID form the packet
     * @param buffRec packet buffer
     * @return packet sessionID
     * @throws IllegalStateException if the packet type is invalid or too small 
     */
    private int packetID(ByteBuffer buffRec) throws IllegalStateException{
    	if(buffRec.remaining() < RECIEVE_BUFFER_SIZE) {
    		throw new IllegalStateException("packet size is too small");
    	}
    	if(buffRec.get() != 1) {
    		throw new IllegalStateException("invalid packet type");
    	};
    	return (int)buffRec.getLong();
    }
    
    
    private int analysePacket(ByteBuffer buffRec) {
    	buffRec.
    }
    
    
    public void serve() throws IOException {
        while (!Thread.interrupted()) {
        	try {
            	buffRec.clear();
            	var exp = (InetSocketAddress) dc.receive(buffRec);
            	buffRec.flip();
            	int sessionID = packetID(buffRec);
            	
            	analysePacket();
            	
            	if( packetLogger.get(exp).get(sessionID).receivedAllPackets()) {
            		//send res packet
            		
            	}else {
            		// send ACKpacket
            	}
            	
        	}catch (IllegalStateException e) {
        		logger.info("ignored packet : "+e.getMessage());
        	}catch (IOException e) {
        		logger.log(Level.SEVERE, "error in server "+e.getMessage());
        		return;
        	}
          /*
          1) receive request from client
          2) read id
          3) decode msg in request
          
          String upperCaseMsg = msg.toUpperCase();          
          4) create packet with id, upperCaseMsg in UTF-8
          5) send the packet to client
          */
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerIdUpperCaseUDP port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        ServerIdUpperCaseUDP server;
        int port = Integer.valueOf(args[0]);
        if (!(port >= 1024) & port <= 65535) {
            logger.severe("The port number must be between 1024 and 65535");
            return;
        }
        try {
            server = new ServerIdUpperCaseUDP(port);
        } catch (BindException e) {
            logger.severe("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
        server.serve();
    }
}
