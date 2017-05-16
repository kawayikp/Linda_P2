
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

class MessageBuilder {
    private static final Pattern IPV4 = Pattern.compile("(([1-9]|1\\d|2[0-4])?\\d|25[0-5])(\\.(([1-9]|1\\d|2[0-4])?\\d|25[0-5])){3}");
    private final static String TYPEMATCH_INTEGER_NAME = "class java.lang.Integer";
    private final static String TYPEMATCH_FLOAT_NAME = "class java.lang.Float";
    private final static String TYPEMATCH_STRING_NAME = "class java.lang.String";

    // dispatch the request to special message builder
    static Queue<Message> buildMessage(String s) throws Exception {
        if (s.substring(0, 3).equalsIgnoreCase("add")) {
            return buildForwardAskNameMessage(s.substring(3).trim());
        } else if (s.substring(0, 3).equalsIgnoreCase("out")) {
            return buildOutMessage(s.substring(3).trim());
        } else if (s.substring(0, 2).equalsIgnoreCase("rd")) {
            return buildRdMessage(s.substring(2).trim());
        } else if (s.substring(0, 2).equalsIgnoreCase("in")) {
            return buildInMessage(s.substring(2).trim());
        } else if (s.substring(0, 6).equalsIgnoreCase("delete")) {
            return buildDeleteMessage(s.substring(6).trim());
        } else {
            System.out.print("Command is wrong.");
            throw new Exception();
        }
    }

    private static Queue<Message> buildDeleteMessage(String s) throws Exception {
        Queue<Message> list = new LinkedList<>();
        if (s.length() <= 2 || s.charAt(0) != '(' || s.charAt(s.length() -1) != ')') {
            throw new Exception("delete format is wrong");
        }
        s = s.substring(1, s.length() - 1).trim();
        String[] ss = s.split(",");
        for (String name : ss) {
            list.offer(new INTUPLESPACEMessage(name.trim(), null, null, MessageType.INTUPLESPACE, true));
        }
        Queue<Message> messageSend = new LinkedList<>();
        messageSend.offer(new DELETEFORWARDMessage(P2.nets.get(0).hostName, P2.nets.get(0).IP, P2.nets.get(0).port, MessageType.DELETEFORWARD, list));
        return messageSend;
    }

    private static Queue<Message> buildForwardAskNameMessage(String s) throws Exception {
        Queue<Message> list = new LinkedList<>();
        /* 
         * (h2, ip2, port2) (h3, ip3, port3)
         * 1. split by ")", get "(h2, ip2, port2"
         * 2. remove "(", get "h2, ip2, port2"
         * 3. split by ",", get ["h2", "ip2", "port2"]
         * 4. check IP and port
         */
        String[] ss = s.split("\\)");              
        for (int i = 0; i < ss.length; i++) {
            String[] temp = ss[i].trim().split(","); 
            if (temp.length != 3) {
                System.out.print("Add host format is wrong.");
                throw new Exception();
            }
            String name = temp[0].substring(1).trim();
            String IP = getIP(temp[1].trim());
            Integer port = getPort(temp[2].trim());
            Message m = new ASKNAMEMessage(name, IP, port, MessageType.ASKNAME);
            list.offer(m);
        }

        Queue<Message> mq = new LinkedList<>();
        Message m = new ADDFORWARDMessage(P2.nets.get(0).hostName, P2.nets.get(0).IP, P2.nets.get(0).port, MessageType.ADDFORWARD, list);
        mq.offer(m);
        return mq;
    }

    private static Queue<Message> buildOutMessage(String s) throws Exception {
        if (isTypeMatch(s)) {
            System.out.print("Tuple format is wrong. ");
            throw new Exception();
        }
        
        Tuple tuple = tupleBuilder(s);
        int slotNumber = tuple.getSlotNumber();
        Host master = P2.netsMap.get(P2.lookUpTableOriginal[slotNumber]);
        Host backup = P2.netsMap.get(P2.lookUpTableBackup[slotNumber]);
        Queue<Message> messageList = new LinkedList<>();
        // original first, backup second
        messageList.add(new OUTMessage(master.getHostName(), master.getIP(), master.getPort(), MessageType.OUT, tuple, true));
        messageList.add(new OUTMessage(backup.getHostName(), backup.getIP(), backup.getPort(), MessageType.OUT, tuple, false));
        return messageList;
    }

    private static Queue<Message> buildRdMessage(String s) throws Exception {
        Queue<Message> messageList = new LinkedList<>();

        if (isTypeMatch(s)) {
            Tuple tuple = tupleBuilder(s);
            messageList = broadcastHelper(MessageType.RDBROADCAST, tuple);
        } else {
            Tuple tuple = tupleBuilder(s);
            int slotNumber = tuple.getSlotNumber();
            Host master = P2.netsMap.get(P2.lookUpTableOriginal[slotNumber]);
            Host backup = P2.netsMap.get(P2.lookUpTableBackup[slotNumber]);
            // order doesn't matter
            messageList.add(new RDMessage(master.getHostName(), master.getIP(), master.getPort(), MessageType.RD, tuple, true));
            messageList.add(new RDMessage(backup.getHostName(), backup.getIP(), backup.getPort(), MessageType.RD, tuple, false));
        }
        return messageList;
    }

    private static Queue<Message> buildInMessage(String s) throws Exception {
        Queue<Message> messageList = new LinkedList<>();

        if (isTypeMatch(s)) {
            Tuple tuple = tupleBuilder(s);
            messageList = broadcastHelper(MessageType.INBROADCAST, tuple);
        } else {

            Tuple tuple = tupleBuilder(s);
            int slotNumber = tuple.getSlotNumber();
            Host master = P2.netsMap.get(P2.lookUpTableOriginal[slotNumber]);
            Host backup = P2.netsMap.get(P2.lookUpTableBackup[slotNumber]);
            // order doesn't matter
            messageList.add(new INMessage(backup.getHostName(), backup.getIP(), backup.getPort(), MessageType.IN, tuple, false));
            messageList.add(new INMessage(master.getHostName(), master.getIP(), master.getPort(), MessageType.IN, tuple, true));
        }
        return messageList;
    }

    private static Queue<Message> broadcastHelper(MessageType mType, Tuple tuple) {
        Queue<Message> messageList = new LinkedList<>();
        for (int i = 0; i < P2.nets.size(); i++) {
            Host h = P2.nets.get(i);
            switch (mType) {
            case RDBROADCAST:
                messageList.add(new RDBROADCASTMessage(h.hostName, h.IP, h.port, MessageType.RDBROADCAST, tuple));
                break;
            case INBROADCAST:
                messageList.add(new INBROADCASTMessage(h.hostName, h.IP, h.port, MessageType.INBROADCAST, tuple));
                break;
            default:
                System.out.println("Can't reach this line");
                break;
            }
        }
        return messageList;
    }

    private static Tuple tupleBuilder(String s) throws Exception {
        /*
         * input : 
         * case1: ("abc", +3, -7.5, -7.5f, 2e3)
         * case2: ("abc", ?i:int, ?f:float, ?s:string)
         * 
         * 1.substring and split by ",", get ["abc" , +3, -7.5, -7.5f, 2e3]
         * 2.check each field
         *      case1: variable match
         *      case2: String
         *      case3: float
         *      case4: int
         */
        if (s.length() < 2 || s.charAt(0) != '(' || s.charAt(s.length() - 1) != ')') {
            System.out.print("Tuple format is wrong. ");
            throw new Exception();
        }
        s = s.substring(1, s.length() - 1);     // "abc", +3, -7.5, -7.5f, 2e3
        String[] ss = s.split(",");             // ["abc" , +3, -7.5, -7.5f, 2e3]
        if (s.length() != 0 && ss.length == 0) {// out/in/rd (,) (,,)
            System.out.print("Tuple format is wrong. ");
            throw new Exception();
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < ss.length; i++) {
            String temp = ss[i].trim();         // ?i:int || "abc" || +3 || -7.5f || 2e3
            Object o = null;
            // type match
            if (temp.length() == 0) {           // out/in/rd ()
                break;
            } else if (temp.charAt(0) == '?') {
                int index = temp.indexOf(':');
                String name = temp.substring(1, index);
                String type = temp.substring(index + 1);
                if (type.equalsIgnoreCase("int")) {
                    type = TYPEMATCH_INTEGER_NAME;
                } else if (type.equalsIgnoreCase("float")) {
                    type = TYPEMATCH_FLOAT_NAME;
                } else if (type.equalsIgnoreCase("string")) {
                    type = TYPEMATCH_STRING_NAME;
                } else {
                    System.out.print("Type match format is wrong. ");
                    throw new Exception();
                }
                o = new ArrayList<String>(Arrays.asList(name, type));
                // type is string
            } else if (temp.charAt(0) == '"' && temp.charAt(temp.length() - 1) == '"') {
                o = new String(temp.substring(1, temp.length() - 1));
                // type is float
            } else if (temp.indexOf('.') != -1 || temp.indexOf('e') != -1 || temp.indexOf('E') != -1) {
                try {
                    o = Float.valueOf(temp);
                } catch (Exception e) {
                    System.out.print("Float format is wrong. ");
                    throw new Exception();
                }

                if ((Float) o == Float.POSITIVE_INFINITY) {
                    System.out.print("Float is overflow. ");
                    throw new Exception();
                }
                // type is int
            } else {
                try {
                    o = Integer.valueOf(temp);
                } catch (Exception e) {
                    System.out.print("Tuple format is wrong. ");
                    throw new Exception();
                }
            }
            list.add(o);
        }
        Tuple t = new Tuple(list);
        return t;
    }

    // only check whether there is a  field like ?x:
    private static boolean isTypeMatch(String s) throws Exception {
        s = s.substring(1, s.length() - 1);     // ("abc", +3, -7.5, -7.5f, 2e3)
        /*
         * input is 
         * case1: "abc", +3, -7.5, -7.5f, 2e3
         * case2: ?i:int, ?f:float, ?s:string
         * 
         * 1. split by "," , get field  [" ?i:int ", " 3"]
         * 2. check whether is type match
         */
        String[] ss = s.split(",");             // [" ?i:int ", " 3"]
        for (int i = 0; i < ss.length; i++) {
            String temp = ss[i].trim();         // "abc"    +3  -7.5f   2e3
            // type match
            if (temp.length() != 0 && temp.charAt(0) == '?') {  // in case tuple = ()
                if (temp.indexOf(':') > 1) {
                    return true;
                } else {
                    System.out.print("Tuple format is wrong. ");// variable name is empty
                    throw new Exception();
                }
            }
        }
        return false;
    }

    private static String getIP(String s) throws Exception {
        if (IPV4.matcher(s).matches()) {
            return s;
        } else {
            System.out.print("IP is wrong.");
            throw new Exception();
        }
    }

    private static Integer getPort(String s) throws Exception {
        Integer port = Integer.parseInt(s);
        if (port <= 1023) {
            System.out.print("Port number is wrong. ");
            throw new Exception();
        }
        return port;
    }
}
