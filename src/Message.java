
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Queue;

abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String hostName;
    protected final String IP;
    protected final Integer port;

    protected MessageType mType;
    protected ResponseType rType;

    Message(String hostName, String IP, Integer port, MessageType mType) {
        this.hostName = hostName;
        this.IP = IP;
        this.port = port;
        this.mType = mType;
        this.rType = ResponseType.DEFAULT;
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

    MessageType getType() {
        return mType;
    }

    ResponseType getResponse() {
        return rType;
    }

    void setResponse(ResponseType rType) {
        this.rType = rType;
    }

    @Override
    public String toString() {
        return "HostName = " + hostName + " MessageType = " + mType;
    }
}

class ADDFORWARDMessage extends Message {
    private static final long serialVersionUID = 1L;
    Queue<Message> mq;

    ADDFORWARDMessage(String hostName, String IP, Integer port, MessageType mType, Queue<Message> mq) {
        super(hostName, IP, port, mType);
        this.mq = mq;
    }

    Queue<Message> getMessageQueue() {
        return mq;
    }

    @Override
    public String toString() {
        String s = super.toString();
        s += mq;
        return s;
    }
}

class ASKNAMEMessage extends Message {
    private static final long serialVersionUID = 1L;

    ASKNAMEMessage(String hostName, String IP, Integer port, MessageType mType) {
        super(hostName, IP, port, mType);
    }
}

class ADDHOSTMessage extends Message {

    private static final long serialVersionUID = 1L;

    private List<Host> nets;
    private Map<String, Host> netsMap;
    private String[] lookUpTableOriginal;
    private String[] lookUpTableBackup;
    private List<List<Integer>> lookUpTableOriginalReverse;
    private List<List<Integer>> lookUpTableBackupReverse;

    ADDHOSTMessage(String hostName, String IP, Integer port, MessageType mType) {
        super(hostName, IP, port, mType);
        
    }

    
    List<Host> getNets() {
        return nets;
    }

    void setNets(List<Host> nets) {
        this.nets = nets;
    }

    Map<String, Host> getNetsMap() {
        return netsMap;
    }

    void setNetsMap(Map<String, Host> netsMap) {
        this.netsMap = netsMap;
    }

    String[] getLookUpTableOriginal() {
        return lookUpTableOriginal;
    }

    void setLookUpTableOriginal(String[] lookUpTableOriginal) {
        this.lookUpTableOriginal = lookUpTableOriginal;
    }

    String[] getLookUpTableBackup() {
        return lookUpTableBackup;
    }

    void setLookUpTableBackup(String[] lookUpTableBackup) {
        this.lookUpTableBackup = lookUpTableBackup;
    }

    List<List<Integer>> getLookUpTableOriginalReverse() {
        return lookUpTableOriginalReverse;
    }

    void setLookUpTableOriginalReverse(List<List<Integer>> lookUpTableOriginalReverse) {
        this.lookUpTableOriginalReverse = lookUpTableOriginalReverse;
    }

    List<List<Integer>> getLookUpTableBackupReverse() {
        return lookUpTableBackupReverse;
    }

    void setLookUpTableBackupReverse(List<List<Integer>> lookUpTableBackupReverse) {
        this.lookUpTableBackupReverse = lookUpTableBackupReverse;
    }

}

class UPDATEHOSTMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String addHostName;
    private List<Host> nets;
    private Map<String, Host> netsMap;
    private String[] lookUpTableOriginal;
    private String[] lookUpTableBackup;
    private List<List<Integer>> lookUpTableOriginalReverse;
    private List<List<Integer>> lookUpTableBackupReverse;

    UPDATEHOSTMessage(String hostName, String IP, Integer port, MessageType mType, String addHostName) {
        super(hostName, IP, port, mType);
        this.addHostName = addHostName;
    }

    String getaddHostName() {
        return addHostName;
    }
    
    List<Host> getNets() {
        return nets;
    }

    void setNets(List<Host> nets) {
        this.nets = nets;
    }

    Map<String, Host> getNetsMap() {
        return netsMap;
    }

    void setNetsMap(Map<String, Host> netsMap) {
        this.netsMap = netsMap;
    }

    String[] getLookUpTableOriginal() {
        return lookUpTableOriginal;
    }

    void setLookUpTableOriginal(String[] lookUpTableOriginal) {
        this.lookUpTableOriginal = lookUpTableOriginal;
    }

    String[] getLookUpTableBackup() {
        return lookUpTableBackup;
    }

    void setLookUpTableBackup(String[] lookUpTableBackup) {
        this.lookUpTableBackup = lookUpTableBackup;
    }

    List<List<Integer>> getLookUpTableOriginalReverse() {
        return lookUpTableOriginalReverse;
    }

    void setLookUpTableOriginalReverse(List<List<Integer>> lookUpTableOriginalReverse) {
        this.lookUpTableOriginalReverse = lookUpTableOriginalReverse;
    }

    List<List<Integer>> getLookUpTableBackupReverse() {
        return lookUpTableBackupReverse;
    }

    void setLookUpTableBackupReverse(List<List<Integer>> lookUpTableBackupReverse) {
        this.lookUpTableBackupReverse = lookUpTableBackupReverse;
    }
}

class OUTTUPLESPACEMessage extends Message {
    private static final long serialVersionUID = 1L;

    private Map<Integer, Map<Tuple, Integer>> tuplesOriginal;
    private Map<Integer, Map<Tuple, Integer>> tuplesBackup;

    OUTTUPLESPACEMessage(String hostName, String IP, Integer port, MessageType mType, Map<Integer, Map<Tuple, Integer>> NesHosttuplesOriginal,
            Map<Integer, Map<Tuple, Integer>> NesHosttuplesBackup) {
        super(hostName, IP, port, mType);
        this.tuplesOriginal = NesHosttuplesOriginal;
        this.tuplesBackup = NesHosttuplesBackup;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesOriginal() {
        return tuplesOriginal;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesBackup() {
        return tuplesBackup;
    }
}

class ADDHOSTRESPONSEMessage extends Message {
    private static final long serialVersionUID = 1L;

    ADDHOSTRESPONSEMessage(String hostName, String IP, Integer port, MessageType mType) {
        super(hostName, IP, port, mType);
    }
}

class ADDUPDATEHOSTRESPONSEMessage extends Message {
    private static final long serialVersionUID = 1L;

    private Map<Integer, Map<Tuple, Integer>> tuplesOriginal;
    private Map<Integer, Map<Tuple, Integer>> tuplesBackup;

    ADDUPDATEHOSTRESPONSEMessage(String hostName, String IP, Integer port, MessageType mType, Map<Integer, Map<Tuple, Integer>> tuplesOriginal,
            Map<Integer, Map<Tuple, Integer>> tuplesBackup) {
        super(hostName, IP, port, mType);
        this.tuplesOriginal = tuplesOriginal;
        this.tuplesBackup = tuplesBackup;
    }

    Map<Integer, Map<Tuple, Integer>> getTuplesOriginal() {
        return tuplesOriginal;
    }

    Map<Integer, Map<Tuple, Integer>> getTuplesBackup() {
        return tuplesBackup;
    }
}

class OUTMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Tuple t;
    private boolean isMaster;

    OUTMessage(String hostName, String IP, Integer port, MessageType mType, Tuple t, boolean isMaster) {
        super(hostName, IP, port, mType);
        this.t = t;
        this.isMaster = isMaster;
    }

    Tuple getTuple() {
        return t;
    }

    boolean getIsMaster() {
        return isMaster;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " tuple = " + t;
    }
}

class RDMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Tuple t;
    private boolean isMaster;

    RDMessage(String hostName, String IP, Integer port, MessageType mType, Tuple t, boolean isMaster) {
        super(hostName, IP, port, mType);
        this.t = t;
        this.isMaster = isMaster;
    }

    Tuple getTuple() {
        return t;
    }

    void setTuple(Tuple t) {
        this.t = t;
    }

    boolean getIsMaster() {
        return isMaster;
    }

    void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " tuple = " + t;
    }
}

class RDBROADCASTMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Tuple t;
    private boolean isMaster;

    public RDBROADCASTMessage(String hostName, String IP, Integer port, MessageType mType, Tuple t) {
        super(hostName, IP, port, mType);
        this.t = t;
    }

    Tuple getTuple() {
        return t;
    }

    void setTuple(Tuple t) {
        this.t = t;
    }

    boolean getIsMaster() {
        return isMaster;
    }

    void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " tuple = " + t;
    }
}

class INMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Tuple t;
    private boolean isMaster;

    INMessage(String hostName, String IP, Integer port, MessageType mType, Tuple t, boolean isMaster) {
        super(hostName, IP, port, mType);
        this.t = t;
        this.isMaster = isMaster;
    }

    Tuple getTuple() {
        return t;
    }

    void setTuple(Tuple t) {
        this.t = t;
    }

    boolean getIsMaster() {
        return isMaster;
    }

    void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " tuple = " + t;
    }
}

class INBROADCASTMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Tuple t;
    private boolean isMaster;

    INBROADCASTMessage(String hostName, String IP, Integer port, MessageType mType, Tuple t) {
        super(hostName, IP, port, mType);
        this.t = t;
    }

    Tuple getTuple() {
        return t;
    }

    void setTuple(Tuple t) {
        this.t = t;
    }

    boolean getIsMaster() {
        return isMaster;
    }

    void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " tuple = " + t;
    }
}

class REBOOTMessage extends Message {
    private static final long serialVersionUID = 1L;
    private Host host;

    private List<Host> nets;
    private Map<String, Host> netsMap;
    private Map<Integer, Map<Tuple, Integer>> tuplesOriginal;
    private Map<Integer, Map<Tuple, Integer>> tuplesBackup;
    private List<List<Integer>> lookUpTableOriginalReverse;
    private List<List<Integer>> lookUpTableBackupReverse;
    private String[] lookUpTableOriginal;
    private String[] lookUpTableBackup;

    REBOOTMessage(String hostName, String IP, Integer port, MessageType mType, Host host, List<Host> nets, Map<String, Host> netsMap) {
        super(hostName, IP, port, mType);
        this.host = host;
        this.nets = nets;
        this.netsMap = netsMap;
    }

    Host getHost() {
        return host;
    }

    void setHost(Host host) {
        this.host = host;
    }

    List<Host> getNets() {
        return nets;
    }

    void setNets(List<Host> nets) {
        this.nets = nets;
    }

    Map<String, Host> getNetsMap() {
        return netsMap;
    }

    void setNetsMap(Map<String, Host> netsMap) {
        this.netsMap = netsMap;
    }

    Map<Integer, Map<Tuple, Integer>> getTuplesOriginal() {
        return tuplesOriginal;
    }

    void setTuplesOriginal(Map<Integer, Map<Tuple, Integer>> tuplesOriginal) {
        this.tuplesOriginal = tuplesOriginal;
    }

    Map<Integer, Map<Tuple, Integer>> getTuplesBackup() {
        return tuplesBackup;
    }

    void setTuplesBackup(Map<Integer, Map<Tuple, Integer>> tuplesBackup) {
        this.tuplesBackup = tuplesBackup;
    }

    List<List<Integer>> getLookUpTableOriginalReverse() {
        return lookUpTableOriginalReverse;
    }

    void setLookUpTableOriginalReverse(List<List<Integer>> lookUpTableOriginalReverse) {
        this.lookUpTableOriginalReverse = lookUpTableOriginalReverse;
    }

    List<List<Integer>> getLookUpTableBackupReverse() {
        return lookUpTableBackupReverse;
    }

    void setLookUpTableBackupReverse(List<List<Integer>> lookUpTableBackupReverse) {
        this.lookUpTableBackupReverse = lookUpTableBackupReverse;
    }

    String[] getLookUpTableOriginal() {
        return lookUpTableOriginal;
    }

    void setLookUpTableOriginal(String[] lookUpTableOriginal) {
        this.lookUpTableOriginal = lookUpTableOriginal;
    }

    String[] getLookUpTableBackup() {
        return lookUpTableBackup;
    }

    void setLookUpTableBackup(String[] lookUpTableBackup) {
        this.lookUpTableBackup = lookUpTableBackup;
    }
}

class DELETEFORWARDMessage extends Message {
    private static final long serialVersionUID = 1L;
    Queue<Message> mq;

    DELETEFORWARDMessage(String hostName, String IP, Integer port, MessageType mType, Queue<Message> mq) {
        super(hostName, IP, port, mType);
        this.mq = mq;
    }

    Queue<Message> getMessageQueue() {
        return mq;
    }

    @Override
    public String toString() {
        String s = super.toString();
        s += mq;
        return s;
    }
}

class INTUPLESPACEMessage extends Message {
    private static final long serialVersionUID = 1L;
    private boolean isDeleted;
    private Map<Integer, Map<Tuple, Integer>> tuplesOriginal;
    private Map<Integer, Map<Tuple, Integer>> tuplesBackup;

    INTUPLESPACEMessage(String hostName, String IP, Integer port, MessageType mType, boolean isDeleted) {
        super(hostName, IP, port, mType);
        this.isDeleted = isDeleted;
    }

    boolean getIsDeleted() {
        return this.isDeleted;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesOriginal() {
        return tuplesOriginal;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesBackup() {
        return tuplesBackup;
    }

    void settuplesOriginal(Map<Integer, Map<Tuple, Integer>> NesHosttuplesOriginal) {
        this.tuplesOriginal = NesHosttuplesOriginal;
    }

    void settuplesBackup(Map<Integer, Map<Tuple, Integer>> NesHosttuplesBackup) {
        this.tuplesBackup = NesHosttuplesBackup;
    }
}

class DELETEUPDATEHOSTMessage extends Message {
    private static final long serialVersionUID = 1L;

    private String deleteHostName;

    private List<Host> nets;
    private Map<String, Host> netsMap;
    private String[] lookUpTableOriginal;
    private String[] lookUpTableBackup;
    private List<List<Integer>> lookUpTableOriginalReverse;
    private List<List<Integer>> lookUpTableBackupReverse;
    private Map<Integer, Map<Tuple, Integer>> tuplesOriginal;
    private Map<Integer, Map<Tuple, Integer>> tuplesBackup;

    DELETEUPDATEHOSTMessage(String hostName, String IP, Integer port, MessageType mType, String deleteHostName, List<Host> nets,
            Map<String, Host> netsMap, String[] lookUpTableOriginal, String[] lookUpTableBackup, List<List<Integer>> lookUpTableOriginalReverse,
            List<List<Integer>> lookUpTableBackupReverse, Map<Integer, Map<Tuple, Integer>> tuplesOriginal,
            Map<Integer, Map<Tuple, Integer>> tuplesBackup) {
        super(hostName, IP, port, mType);
        this.deleteHostName = deleteHostName;
        this.nets = nets;
        this.netsMap = netsMap;
        this.lookUpTableOriginal = lookUpTableOriginal;
        this.lookUpTableBackup = lookUpTableBackup;
        this.lookUpTableOriginalReverse = lookUpTableOriginalReverse;
        this.lookUpTableBackupReverse = lookUpTableBackupReverse;
        this.tuplesOriginal = tuplesOriginal;
        this.tuplesBackup = tuplesBackup;

    }

    String getdeleteHostName() {
        return deleteHostName;
    }

    List<Host> getNets() {
        return nets;
    }

    Map<String, Host> getNetsMap() {
        return netsMap;
    }

    String[] getLookUpTableOriginal() {
        return lookUpTableOriginal;
    }

    String[] getLookUpTableBackup() {
        return lookUpTableBackup;
    }

    List<List<Integer>> getLookUpTableOriginalReverse() {
        return lookUpTableOriginalReverse;
    }

    List<List<Integer>> getLookUpTableBackupReverse() {
        return lookUpTableBackupReverse;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesOriginal() {
        return tuplesOriginal;
    }

    Map<Integer, Map<Tuple, Integer>> gettuplesBackup() {
        return tuplesBackup;
    }
}

class DELETEUPDATEHOSTRESPONSEMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String deleteHostName;

    DELETEUPDATEHOSTRESPONSEMessage(String hostName, String IP, Integer port, MessageType mType, String deleteHostName) {
        super(hostName, IP, port, mType);
        this.deleteHostName = deleteHostName;
    }

    String getdeleteHostName() {
        return deleteHostName;
    }
}

enum MessageType {
    DEFAULT, ADDFORWARD, ASKNAME, ADDHOST, ADDHOSTRESPONSE, ADDUPDATEHOST, ADDUPDATEHOSTRESPONSE, OUTTUPLESPACE, REBOOT, 
    DELETEFORWARD, DELETEUPDATEHOST, DELETEUPDATEHOSTRESPONSE, INTUPLESPACE, OUT, RD, IN, INBROADCAST, RDBROADCAST,

}

enum ResponseType {
    DEFAULT, SUCCESS, ERROR,
}