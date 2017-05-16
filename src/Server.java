
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;


class Server implements Runnable{
    static String IP;
    static int port;
    static ServerSocket serverSocket;


    Server() throws IOException {
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        IP = InetAddress.getLocalHost().getHostAddress();
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

