package kury.TimSpik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TimSpik_Control {
    ServerSocket s;
    List<TimSpik_User> connectedUsers;
    private static final int PORT = 6982;

    //Control strings examples
    //$JOIN 'Dallas'
    //$CHAT 'Dallas' $TO 'Kury' $BODY 'cs?'
    //$QUIT 'Dallas'

    private static final String sc  = "$";
    private static final String usrJoin = sc + "JOIN";
    private static final String usrLeave = sc + "QUIT";
    private static final String chat = sc + "CHAT";
    private static final String chatTo = sc + "TO";
    private static final String body = sc +"BODY";
    private static final String connUsers = sc + "USERS";

    public TimSpik_Control() throws IOException {
        s = new ServerSocket(PORT);
        connectedUsers = new ArrayList<TimSpik_User>();
    }

    public void listen() throws IOException{
        while(true){
            //TODO keepAlive() every 10 seconds or so
            Socket soc = s.accept();
            InputStream is = soc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str = br.readLine();
            if(str.charAt(0) != '$'){
                soc.close();
            } else {
                if(str.contains(usrJoin)){
                    //TODO userJoin method

                } else if (str.contains(usrLeave)){
                    //TODO userLeave method
                } else if (str.contains(chat)){
                    //TODO chat handler
                } else if (str.contains(connUsers)){
                    //TODO getConnectedUsers
                }
            }
        }
    }

    private List<TimSpik_User> getConnectedUsers(){
        return connectedUsers;
    }

    public void keepAlive(){
        List<TimSpik_User> users = getConnectedUsers();
        for (TimSpik_User usr:users) {
            try{
                Socket soc = new Socket(usr.addr, PORT);
            } catch (IOException ex){
                //remove user
            }
        }
    }
}
