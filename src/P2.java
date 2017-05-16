import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;


public class P2 {
    static String hostName;
    private static Server server;
    static final int NUMBER_OF_SLOTS = 65535;

    static Map<Integer, Map<Tuple, Integer>> tuplesOriginal;    
    static Map<Integer, Map<Tuple, Integer>> tuplesBackup;     

    static List<Host> nets;                
    static Map<String, Host> netsMap;      

    static String[] lookUpTableOriginal;   
    static String[] lookUpTableBackup;      

    static List<List<Integer>> lookUpTableOriginalReverse;  
    static List<List<Integer>> lookUpTableBackupReverse;    

    static final String TOP_DIRECTORY = "/tmp/yliu3/";
    static String[] paths;
    static String netsFile;
    static String tuplesOriginalFile;
    static String tuplesBackupFile;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Invalid host name, please try again.");
            System.exit(0);
        }

        P2.startHost(args[0]);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("linda>");
            String commandLine = br.readLine().trim();
            if (commandLine.length() == 0) {
                continue;
            } else if (commandLine.equals("t")) {
                System.out.println("Original = " + P2.tuplesOriginal + "\nBackup = " + P2.tuplesBackup);
                continue;
            } else if (commandLine.equals("n")) {
                System.out.println(P2.nets);
                continue;
            } else if (commandLine.equals("l")) {
                P2.listCompare();
                continue;
            } else if (commandLine.equals("exit")) {
                InputOutputController.deleteFile(paths);
                System.out.println("exit");
                System.exit(0);
            }

            Queue<Message> mq = null;
            try {
                mq = MessageBuilder.buildMessage(commandLine);
            } catch (Exception e) {
                System.out.println(" Please try again");
                continue;
            }

            // first step in the system must be add host
            if (P2.nets.size() == 1 && mq.peek().getType() != MessageType.ADDFORWARD) {
                System.out.println("Linda system must have at least 2 hosts, please add hosts first.");
                continue;
            }
            Client client = new Client(mq);
            client.run();
        }
    }

    // start the host
    private static void startHost(String hostName) throws Exception {
        // case1: reboot
        if (InputOutputController.isPathExist(TOP_DIRECTORY)) {
            reboot(hostName);
            // case2: new host
        } else {
            InitializeNewHost(hostName);
        }
    }

    // rebooted host start
    private static void reboot(String hostName) throws Exception {
        paths = new String[] { "/tmp/yliu3/", "/tmp/yliu3/linda/", "/tmp/yliu3/linda/" + hostName + "/" };
        netsFile = "nets.txt";
        tuplesOriginalFile = "tuples_original.txt";
        tuplesBackupFile = "tuples_backup.txt";

        // check reboot name
        String preHost = InputOutputController.getFileName(paths[1]);
        if (!preHost.equals(hostName)) {
            System.out.println("Reboot failed. The crashed host's name is " + preHost + ". Please input correct name.");
            System.exit(1);
        }

        // initialization
        P2.hostName = hostName;
        server = new Server();
        new Thread(server).start();

        // nets
        P2.nets = InputOutputController.deSerialize(P2.paths[2] + P2.netsFile);
        Host rebootHost = new Host(P2.hostName, Server.IP, Server.port);
        P2.netsMap = new ConcurrentHashMap<>();
        for (int i = 0; i < P2.nets.size(); i++) {
            if (P2.nets.get(i).equals(rebootHost)) {
                P2.nets.set(i, rebootHost);
            }
            P2.netsMap.put(P2.nets.get(i).hostName, P2.nets.get(i));
        }

        // serialize
        InputOutputController.serialize(P2.nets, P2.paths[2] + P2.netsFile);

        // generate reboot message
        Queue<Message> messageSend = new LinkedList<>();
        for (int i = 0; i < P2.nets.size(); i++) {
            if (!P2.nets.get(i).equals(rebootHost)) {
                Host h = P2.nets.get(i);
                Message m = new REBOOTMessage(h.hostName, h.IP, h.port, MessageType.REBOOT, rebootHost, P2.nets, P2.netsMap);
                messageSend.offer(m);
            }
        }
        
        if (!messageSend.isEmpty()) {
            Client client = new Client(messageSend);
            client.run();
        }
    }

    // new host start
    private static void InitializeNewHost(String hostName) throws IOException {
        // initialization
        P2.hostName = hostName;
        server = new Server();
        new Thread(server).start();

        paths = new String[] { "/tmp/yliu3/", "/tmp/yliu3/linda/", "/tmp/yliu3/linda/" + hostName + "/" };
        netsFile = "nets.txt";
        tuplesOriginalFile = "tuples_original.txt";
        tuplesBackupFile = "tuples_backup.txt";

        // tuple space
        tuplesOriginal = new ConcurrentHashMap<>();
        tuplesBackup = new ConcurrentHashMap<>();

        // nets
        nets = new ArrayList<>();
        nets.add(new Host(hostName, Server.IP, Server.port));      
        netsMap = new HashMap<>();
        netsMap.put(hostName, new Host(hostName, Server.IP, Server.port));

        // LUT
        lookUpTableOriginal = new String[NUMBER_OF_SLOTS];
        for (int i = 0; i < NUMBER_OF_SLOTS; i++) {
            lookUpTableOriginal[i] = P2.hostName;
        }
        lookUpTableBackup = new String[NUMBER_OF_SLOTS];
        for (int i = 0; i < NUMBER_OF_SLOTS; i++) {
            lookUpTableBackup[i] = P2.hostName;
        }

        // reverseLUT
        lookUpTableOriginalReverse = new ArrayList<>();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SLOTS; i++) {
            list.add(i);
        }
        lookUpTableOriginalReverse.add(list);
        lookUpTableBackupReverse = new ArrayList<>();
        lookUpTableBackupReverse.add(new ArrayList<>(list));

        // serialization
        InputOutputController.addFile(paths, paths[2], netsFile, tuplesOriginalFile, tuplesBackupFile);
        InputOutputController.P2serilize();
    }

    private static void listCompare() {
        int n = lookUpTableOriginalReverse.size();
        for (int i = 0; i < n; i++) {
            if (P2.lookUpTableOriginalReverse.get(i).size() != P2.lookUpTableBackupReverse.get((i + 1) % n).size()) {
                System.out.println("list length is not the same");
            } else {
                int m = P2.lookUpTableOriginalReverse.get(i).size();
                for (int j = 0; j < m; j++) {
                    if (!P2.lookUpTableOriginalReverse.get(i).get(j).equals(P2.lookUpTableBackupReverse.get((i + 1) % n).get(j))) {
                        System.out.println("element is not the same");
                    }
                }
            }
        }
    }
}

class Host implements Serializable {
    private static final long serialVersionUID = 1L;
    String hostName;
    String IP;
    Integer port;

    Host(String hostName, String IP, Integer port) {
        this.hostName = hostName;
        this.IP = IP;
        this.port = port;
    }

    String getHostName() {
        return hostName;
    }

    String getIP() {
        return IP;
    }

    Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this.getClass() != o.getClass()) {
            return false;
        }

        Host other = (Host) o;
        return this.getHostName().equals(other.getHostName());
    }

    @Override
    public int hashCode() {
        return hostName.hashCode();
    }

    @Override
    public String toString() {
        return hostName + "(" + IP + ":" + port + ")";
    }
}
