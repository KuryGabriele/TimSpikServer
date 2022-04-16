package kury.TimSpik;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimSpik_AudioServer {
    private int port;
    private DatagramSocket soc;
    private static final boolean VERBOSE = false;
    private List<InetAddress> connectedUsers = new ArrayList<InetAddress>();

    public TimSpik_AudioServer(int port){
        this.port = port;
    }

    public void listen() throws IOException {
        soc = new DatagramSocket(port);
        System.out.println("Server started listening on port "  + port);

        while (true){
            byte[] kuryStream = new byte[960000]; //TODO Aggiorna con dimensione giusta
            DatagramPacket packet = new DatagramPacket(kuryStream, kuryStream.length);

            if(VERBOSE){
                System.out.println("Waiting for packet...");
            }
            soc.receive(packet);

            InetAddress addr = packet.getAddress();

            String pktName = new String(Arrays.copyOfRange(packet.getData(), 0, 4), "UTF-8");
            if(pktName.contains("JOIN")){
                if(VERBOSE){
                    System.out.println("JOIN received");
                }
                //Add user to the list if not already in it
                if(!connectedUsers.contains(addr)){
                    connectedUsers.add(addr);
                }
            } else if (pktName.contains("QUIT")){
                if(VERBOSE){
                    System.out.println("QUIT received");
                }

                if(connectedUsers.contains(addr)){
                    connectedUsers.remove(addr);
                }
            }

            for (InetAddress address: connectedUsers) {
                //send to everyone except the sender
                if(address != addr){
                    if(VERBOSE){
                        System.out.println("Sending packet to" + address);
                    }
                    //Relay packet to everyone
                    packet = new DatagramPacket(packet.getData(), packet.getLength(), address, port);
                    soc.send(packet);
                }
            }
        }
    }
}
