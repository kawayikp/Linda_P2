
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;


class Server implements Runnable{
    static String IP;
    static int port;
    static ServerSocket serverSocket;


    Server() {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        port = serverSocket.getLocalPort();
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(P2.hostName + " at " + IP + " : " + port); 
    }

    @Override
    public void run() {
        while (true) {
            try {
                new Thread(new ServerThread(serverSocket.accept())).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

