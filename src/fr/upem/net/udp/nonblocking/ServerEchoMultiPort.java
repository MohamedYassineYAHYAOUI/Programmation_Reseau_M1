package fr.upem.net.udp.nonblocking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Objects;
import java.util.logging.Logger;

public class ServerEchoMultiPort {

	class DatagramChannelData {
		
		private final int BUFFER_SIZE = 1024;
		private InetSocketAddress exp;
		private final ByteBuffer buff ; 
		
		DatagramChannelData(InetSocketAddress exp){
			this.exp = Objects.requireNonNull(exp);
			this.buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
		}
		
		boolean receiveBuffer(DatagramChannel dc) throws IOException{
	    	buff.clear();
	    	exp = (InetSocketAddress) dc.receive(buff);
	    	if(exp !=null) {
	    		buff.flip();
	    	}
	    	return exp !=null;
		}
		
		boolean sendBuffer(DatagramChannel dc) throws IOException{
			dc.send(buff, exp);
	    	return !buff.hasRemaining();
		}
		
	}
	
	
    private static final Logger logger = Logger.getLogger(ServerEchoMultiPort.class.getName());

    private final Selector selector;

    
    
    

    private ServerEchoMultiPort(Selector selector) throws IOException {
    	this.selector = selector;
   }
    

    public static ServerEchoMultiPort serverEchoMultiPortFactory(int p1, int p2) throws IOException  {
   
    	
    	var selector = Selector.open();
    	var server = new ServerEchoMultiPort(selector);
    	
    	for(int p=p1; p<p2; p++) {
            var dc = DatagramChannel.open();
            var exp =new InetSocketAddress(p);
            dc.bind(exp);
            dc.configureBlocking(false); // set dc in non-blocking mode and register it to the selector	 
            dc.register(selector, SelectionKey.OP_READ, server.new DatagramChannelData(exp));
    	}
    	return server;
    }

    public void serve() throws IOException {
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
	        
        }catch (IOException e) {
           throw new UncheckedIOException(e);
        }
    }

    private void doRead(SelectionKey key) throws IOException {
    	var readCh =(DatagramChannel) key.channel();
    	var dcData = (DatagramChannelData) key.attachment();
    	
    	if(dcData.receiveBuffer(readCh)){
    		key.interestOps(SelectionKey.OP_WRITE);
    	}else {
    		logger.info("receive failed");
    	}
	 }

    private void doWrite(SelectionKey key) throws IOException {
    	var writeCh =(DatagramChannel) key.channel();
    	var dcData = (DatagramChannelData) key.attachment();

    	if(dcData.sendBuffer(writeCh)) {
    		key.interestOps(SelectionKey.OP_READ);
    	}else {
    		logger.info("send failed");
    	}
	}

    public static void usage() {
        System.out.println("Usage : ServerEchoMultiPort port1 port2 ...");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            usage();
            return;
        }

        ServerEchoMultiPort server= serverEchoMultiPortFactory(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
        server.serve();
    }
}
