package kury.TimSpik;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimSpik_AudioServer {
    private int port; //UDP port
    private DatagramSocket soc; //UDP socket
    private boolean VERBOSE = false; //if true will print lots of information to console
    private boolean METRICS = false; //true to print how much time it takes to handle the packet to console
    private List<InetAddress> connectedUsers = new ArrayList<InetAddress>(); //List of addresses of all connected users
    private List<String> userNicks = new ArrayList<String>();
    private ArrayList<Long> lastMillisecond = new ArrayList<Long>(); //List of all the last messages timestamps
    private int crashTreshold = 10000; //Milliseconds, if a client has not sent a packet in this amout of time consider them crashed
    private ArrayList<Integer> toRemove = new ArrayList<Integer>(); //indexes of users that crashed and need to be removed

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

            //Get packet type JOIN QUIT or KURY
            String pktName = new String(Arrays.copyOfRange(packet.getData(), 0, 4), "UTF-8");
            String senderNick = new String(Arrays.copyOfRange(packet.getData(), 4, 20), "UTF-8");
            senderNick = senderNick.replaceAll("/t" , " ");
            senderNick = senderNick.trim();


            if(VERBOSE){
                System.out.println("Packet received from " + addr + ", aka " + senderNick + ".");
            }

            if(pktName.contains("JOIN")){
                //If packet is JOIN
                if(VERBOSE){
                    System.out.println("JOIN received");
                }
                //Add user to the list if not already in it
                if(!connectedUsers.contains(addr)){
                    connectedUsers.add(addr);
                    userNicks.add(senderNick);
                    lastMillisecond.add(System.currentTimeMillis());
                }
            } else if (pktName.contains("QUIT")){
                //If packet is QUIT
                if(VERBOSE){
                    System.out.println("QUIT received");
                }

                if(connectedUsers.contains(addr)){
                    int index = connectedUsers.indexOf(addr);
                    connectedUsers.remove(addr);
                    userNicks.remove(senderNick);
                    lastMillisecond.remove(index);
                }
            }

            //Performance metric
            long start1 = System.currentTimeMillis();

            //Send packet only if in connectedUsers or if it is a QUIT
            if(connectedUsers.contains(addr) || pktName.contains(("QUIT"))){
                int index = connectedUsers.indexOf(addr);
                //update last message timestamp
                if(index != -1) {
                    lastMillisecond.set(index, System.currentTimeMillis());
                }

                for (InetAddress address: connectedUsers) {
                    index = connectedUsers.indexOf(address);
                    //If user has not sent a packet in the last 'crashTreshold' ms
                    //count it as crashed
                    if(System.currentTimeMillis() - lastMillisecond.get(index) < crashTreshold){
                        //send to everyone except the sender
                        if(!address.equals(addr)){
                            if(VERBOSE){
                                System.out.println(System.currentTimeMillis() - lastMillisecond.get(index) + " : " + crashTreshold);
                                System.out.println("Sending packet to " + address);
                            }
                            //Relay packet to everyone
                            packet = new DatagramPacket(packet.getData(), packet.getLength(), address, port);
                            soc.send(packet);
                        }
                    } else {
                        toRemove.add(index);
                    }
                }
            }

            //remove crashed users
            for (int usr:toRemove) {
                String str = "QUIT"+userNicks.get(usr);
                for(int i = str.length(); i < 20; i++){
                    str += '\t';
                }

                if(VERBOSE) {
                    System.out.println("User crashed " + userNicks.get(usr));
                }
                //Update api
                String strUrl = "https://timspik.ddns.net/setOnline/"+ userNicks.get(usr) + "/F";
                if(VERBOSE){
                    System.out.println(strUrl);
                }
                URL url = new URL(strUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestProperty("Accept", "*/*");

                int resCode = http.getResponseCode();
                if (VERBOSE) {
                    System.out.println("API returned " + resCode);
                }

                http.disconnect();

                connectedUsers.remove(usr);
                userNicks.remove(usr);
                lastMillisecond.remove(usr);
                for (InetAddress address: connectedUsers){
                    //Notify clients
                    packet = new DatagramPacket(str.getBytes(StandardCharsets.UTF_8), str.length(), address, port);
                    soc.send(packet);
                }
            }

            //Clear crashed users
            toRemove.clear();

            //Print performance metric
            long end1 = System.currentTimeMillis();
            if(METRICS){
                System.out.println("Packets re-sent in: " + (end1-start1) + "ms.");
                System.out.println("From beginning to end: " + (end1-pckReceived) + "ms.");
            }
        }
    }
}
