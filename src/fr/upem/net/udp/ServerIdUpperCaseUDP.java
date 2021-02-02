package fr.upem.net.udp;

import java.util.logging.Logger;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ServerIdUpperCaseUDP {

	//private static final Logger logger = Logger.getLogger(ServerIdUpperCaseUDP.class.getLogger());
	private static final Logger logger = Logger.getLogger(ServerIdUpperCaseUDP.class.getName());
    private static final int BUFFER_SIZE = 1024;
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final DatagramChannel dc;
    private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public ServerIdUpperCaseUDP(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        logger.info("ServerBetterUpperCaseUDP started on port " + port);
    }

    public void serve() throws IOException {
    	
    	ByteBuffer sendBuff = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while (!Thread.interrupted()) {
          buff.clear();
          var exp = (InetSocketAddress) dc.receive(buff);
          buff.flip();
          if(buff.remaining() < Long.BYTES) {
        	  continue;
		  }
          logger.info("received "+buff.remaining()+" bytes from "+exp.toString());
          var id = buff.getLong();
          var msg = UTF8.decode(buff).toString();

          String upperCaseMsg = msg.toUpperCase();
          logger.info("sending \""+id +" "+upperCaseMsg+"\" to "+exp.toString());      
          
          sendBuff.clear();
          sendBuff.putLong(id);
          sendBuff.put(UTF8.encode(upperCaseMsg));
          sendBuff.flip();
          dc.send(sendBuff, exp);

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
