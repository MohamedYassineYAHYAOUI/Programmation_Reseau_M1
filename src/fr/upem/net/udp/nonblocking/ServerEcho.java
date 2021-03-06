package fr.upem.net.udp.nonblocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.logging.Logger;

public class ServerEcho {

    private static final Logger logger = Logger.getLogger(ServerEcho.class.getName());

    private final DatagramChannel dc;
    private final Selector selector;
    private final int BUFFER_SIZE = 1024;
    private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private SocketAddress exp;
    private int port;

    public ServerEcho(int port) throws IOException {
        this.port=port;
        selector = Selector.open();
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        dc.configureBlocking(false); // set dc in non-blocking mode and register it to the selector	 
        dc.register(selector, SelectionKey.OP_READ);
   }


    public void serve() throws IOException {
        logger.info("ServerEcho started on port "+port);
        while (!Thread.interrupted()) {
        	try {
                selector.select(this::treatKey);
        	}catch(UncheckedIOException e) {
        		throw e.getCause();
        	}
        }
    }

    private void treatKey(SelectionKey key) {
        try{
        if (key.isValid() && key.isWritable()) {
            doWrite(key);
        }
        if (key.isValid() && key.isReadable()) {
            doRead(key);
        }
        } catch (IOException e) {
           throw new UncheckedIOException(e);
        }
    }

    private void doRead(SelectionKey key) throws IOException {
    	var readCh =(DatagramChannel) key.channel();
    	buff.clear();
    	exp = (InetSocketAddress) readCh.receive(buff);
    	
    	if(exp !=null){
    		buff.flip();
    		key.interestOps(SelectionKey.OP_WRITE);
    	}else {
    		logger.info("failed receive");
    	}
    
	 }

    private void doWrite(SelectionKey key) throws IOException {
    	
    	var writeCh =(DatagramChannel) key.channel();
    	writeCh.send(buff, exp);
    	if(!buff.hasRemaining()) {
    		key.interestOps(SelectionKey.OP_READ);
    	}else {
    		logger.info("send failed");
    	}
	}

    public static void usage() {
        System.out.println("Usage : ServerEcho port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        ServerEcho server= new ServerEcho(Integer.valueOf(args[0]));
        server.serve();
    }

}
