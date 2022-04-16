package kury.TimSpik;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class AudioServerTester {
    public static void main(String[] args) {
        TimSpik_AudioServer as = new TimSpik_AudioServer(6981);
        try{
            as.listen(args.length > 0);
        } catch(Exception ex){
            System.err.println(ex.getMessage());
        }
    }
}
