package fr.upem.net.udp.nonblocking;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ClientIdUpperCaseUDPOneByOne {

    private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPOneByOne.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final int BUFFER_SIZE = 1024;

    private enum State {SENDING, RECEIVING, FINISHED};

    private final List<String> lines;
    private final List<String> upperCaseLines = new ArrayList<>();
    private final int timeout;
    private final InetSocketAddress serverAddress;
    private final DatagramChannel dc;
    private final Selector selector;
    private final SelectionKey uniqueKey;
    
    private long lineIndex;
    private final ByteBuffer buffer;
    private long sendTime;
    private long currentTime;
    private boolean packegSent;
    // TODO add new fields 

    private State state;

    private static void usage() {
        System.out.println("Usage : ClientIdUpperCaseUDPOneByOne in-filename out-filename timeout host port ");
    }

    public ClientIdUpperCaseUDPOneByOne(List<String> lines, int timeout, InetSocketAddress serverAddress) throws IOException {
        this.lines = lines;
        this.timeout = timeout;
        this.serverAddress = serverAddress;
        this.dc = DatagramChannel.open();
        dc.configureBlocking(false);
        dc.bind(null);
        this.selector = Selector.open();
        this.uniqueKey = dc.register(selector, SelectionKey.OP_WRITE);
        this.state = State.SENDING;
        this.lineIndex = 0;
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

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

        //Read all lines of inFilename opened in UTF-8
        List<String> lines = Files.readAllLines(Paths.get(inFilename), UTF8);
        //Create client with the parameters and launch it
        ClientIdUpperCaseUDPOneByOne client = new ClientIdUpperCaseUDPOneByOne(lines, timeout, serverAddress);
        List<String> upperCaseLines = client.launch();
        Files.write(Paths.get(outFilename), upperCaseLines, UTF8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);

    }

    private List<String> launch() throws IOException, InterruptedException {
        while (!isFinished()) {
            try {
                selector.select(this::treatKey,updateInterestOps());
            } catch(UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
        dc.close();
        return upperCaseLines;
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isWritable()) {
                doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                doRead();
            }
        } catch(IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
    * Updates the interestOps on key based on state of the context
    *
    * @return the timeout for the next select (0 means no timeout)
    */
    
    private int updateInterestOps() {
    	currentTime = System.currentTimeMillis();
    	
    	if( state == State.SENDING) {
    		uniqueKey.interestOps(SelectionKey.OP_WRITE);
    		return 0;
    	}

    	if( state == State.RECEIVING){
    		uniqueKey.interestOps(SelectionKey.OP_READ);
    	}
    	return (int) (timeout - (currentTime - sendTime ));
    }

    private boolean isFinished() {
        return state == State.FINISHED;
    }

    /**
    * Performs the receptions of packets
    *
    * @throws IOException
    */

    private void doRead() throws IOException {
    	
    	buffer.clear();
    	var exp = (InetSocketAddress) dc.receive(buffer);
    	if( exp != null) {
    		buffer.flip();
    		long id = buffer.getLong();
    		if(id ==lineIndex ) {
    			upperCaseLines.add( UTF8.decode(buffer).toString());
    			lineIndex++;
    			state = State.SENDING;
    		}
    		if(lineIndex == lines.size()) {
    			logger.info("finished");
    			state = State.FINISHED;
    		}
    	}
    }

    /**
    * Tries to send the packets
    *
    * @throws IOException
    */

    private void doWrite() throws IOException {
    	
    	buffer.clear();
    	buffer.putLong(lineIndex);
    	buffer.put(UTF8.encode(lines.get((int)lineIndex)));
    	buffer.flip();

    	dc.send(buffer, serverAddress);
    	
    	if(!buffer.hasRemaining() ) {
    		sendTime = System.currentTimeMillis();
        	state =State.RECEIVING;
        	buffer.compact();
    	}
    	

    }
}







