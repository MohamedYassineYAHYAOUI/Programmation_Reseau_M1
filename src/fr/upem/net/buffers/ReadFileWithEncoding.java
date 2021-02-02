package fr.upem.net.buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ReadFileWithEncoding {

	
    private static void usage(){
        System.out.println("Usage: ReadFileWithEncoding charset filename");
    }

    private static String stringFromFile(Charset cs,Path path) throws IOException {
    	try(FileChannel out = FileChannel.open(path, StandardOpenOption.READ)){
        	var buffer = ByteBuffer.allocate((int)out.size());
    		
    		
    		//var buffer = ByteBuffer.allocate(1024);
    		out.read(buffer);

//    		while(buffer.hasRemaining() && out.) {
//        		out.read(buffer); // si la taille > 1024 ?
//        	}
    		
        	buffer.flip();
    		var str = new String(buffer.array(), cs);
    		buffer.clear();
    		return str;
    	}
    }

    public static void main(String[] args) throws IOException {
        if (args.length!=2){
            usage();
            return;
        }
        Charset cs=Charset.forName(args[0]);
        Path path=Paths.get(args[1]);
        System.out.print(stringFromFile(cs,path));
    }


}
