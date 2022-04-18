package kury.TimSpik;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimSpik_AudioServer {
    private int port; //UDP port
    private DatagramSocket soc; //UDP socket
    private boolean VERBOSE = false; //if true will print lots of information to console
    private boolean METRICS = false; //true to print how much time it takes to handle the packet to console
    private List<InetAddress> connectedUsers = new ArrayList<InetAddress>(); //List of addresses of all connected users
    private ArrayList<Long> lastMillisecond = new ArrayList<Long>(); //List of all the last messages timestamps
    private int crashTreshold = 10000; //Milliseconds, if a client has not sent a packet in this amout of time consider them crashed

    public TimSpik_AudioServer(int port){
        this.port = port;
    }

    public void listen(boolean verbose) throws IOException {
        //Instantiate socket
        soc = new DatagramSocket(port);

        this.VERBOSE = verbose;
        System.out.println("Server started listening on port "  + port);

        while (true){
            //Main loop
            //Prepare the packet
            byte[] kuryStream = new byte[960000];
            DatagramPacket packet = new DatagramPacket(kuryStream, kuryStream.length);

            if(VERBOSE){
                System.out.println("Waiting for packet...");
            }

            //Wait for packet
            soc.receive(packet);

            //Save receiving timeStamp
            long pckReceived = System.currentTimeMillis();
            //Get address of sender
            InetAddress addr = packet.getAddress();

            if(VERBOSE){
                System.out.println("Packet received from " + addr);
            }
            //Get packet type JOIN QUIT or KURY
            String pktName = new String(Arrays.copyOfRange(packet.getData(), 0, 4), "UTF-8");
            if(pktName.contains("JOIN")){
                //If packet is JOIN
                if(VERBOSE){
                    System.out.println("JOIN received");
                }
                //Add user to the list if not already in it
                if(!connectedUsers.contains(addr)){
                    connectedUsers.add(addr);
                    lastMillisecond.add(System.currentTimeMillis());
                }
            } else if (pktName.contains("QUIT")){
                //If packet is QUIT
                if(VERBOSE){
                    System.out.println("QUIT received");
                }

                if(connectedUsers.contains(addr)){
                    connectedUsers.remove(addr);
                }
            }

            //Performance metric
            long start1 = System.currentTimeMillis();

            //Send packet only if in connectedUsers or if it is a QUIT
            if(connectedUsers.contains(addr) || pktName.contains(("QUIT"))){
                for (InetAddress address: connectedUsers) {
                    //send to everyone except the sender
                    if(!address.equals(addr)){
                        if(VERBOSE){
                            System.out.println("Sending packet to " + address);
                        }
                        //Relay packet to everyone
                        packet = new DatagramPacket(packet.getData(), packet.getLength(), address, port);
                        soc.send(packet);
                    }
                }
            }

            //Print performance metric
            long end1 = System.currentTimeMillis();
            if(METRICS){
                System.out.println("Packets re-sent in: " + (end1-start1) + "ms.");
                System.out.println("From beginning to end: " + (end1-pckReceived) + "ms.");
            }
        }
    }
}
