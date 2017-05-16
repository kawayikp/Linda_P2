
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

class ServerThread implements Runnable {
    private Socket socket;
    private Message message;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            message = (Message) in.readObject();

            switch (message.getType()) {
            case ADDFORWARD:
                addforwardPrework();
                break;
            case ASKNAME:
                asknamePrework();
                break;
            case ADDHOST:
                addhostPrework();
                break;
            case ADDUPDATEHOST:
                addupdatehostPrework();
                break;
            case OUTTUPLESPACE:
                outtuplespacePreWork();
                break;
            case REBOOT:
                rebootPrework();
                break;
            case OUT:
                outPrework();
                break;
            case RD:
                rdPrework();
                break;
            case IN:
                inPrework();
                break;
            case RDBROADCAST:
                rdbroadcastPrework();
                break;
            case INBROADCAST:
                inbroadcastPrework();
                break;
            case DELETEFORWARD:
                deleteforwardPrework();
                break;
            case INTUPLESPACE:
                intuplespacePreWork();
                break;
            case DELETEUPDATEHOST:
                deleteupdatehostPreWork();
                break;
            default:
                break;
            }

            out.writeObject(message);
            out.flush();
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        switch (message.getType()) {
        case ADDFORWARD:
        case DELETEFORWARD:
            hostWork();
            break;
        case DELETEUPDATEHOSTRESPONSE:
            deleteupdatehostPostWork();
            break;
        default:
            break;
        }

        switch (message.getType()) {
        case ADDFORWARD:
        case ASKNAME:    
        case ADDHOSTRESPONSE:
        case ADDUPDATEHOSTRESPONSE:
        case OUTTUPLESPACE:
        case OUT:
        case RD:
        case IN:
        case DELETEFORWARD:
        case REBOOT:
        case DELETEUPDATEHOSTRESPONSE:
            System.out.print("linda>");
            break;
        default:
            break;
        }
    }

    private void addforwardPrework() {
        ADDFORWARDMessage fm = (ADDFORWARDMessage) message;
        fm.setResponse(ResponseType.SUCCESS);
//        System.out.println("ServerThread[asknameforwardPrework]: I'm master, I received a forward message");
        System.out.println("Master receives a forward message");
    }

    private void asknamePrework() {
        ASKNAMEMessage am = (ASKNAMEMessage) message;
        if (!am.getHostName().equals(P2.hostName)) {
            am.setResponse(ResponseType.ERROR);
            System.out.println("Host name is wrong");
        } else {
            am.setResponse(ResponseType.SUCCESS);
            System.out.println("Host name is correct");
        }
    }

    private void addhostPrework() {
        ADDHOSTMessage am = (ADDHOSTMessage) message;

        P2.nets = am.getNets();
        P2.netsMap = am.getNetsMap();
        P2.lookUpTableOriginal = am.getLookUpTableOriginal();
        P2.lookUpTableBackup = am.getLookUpTableBackup();
        P2.lookUpTableOriginalReverse = am.getLookUpTableOriginalReverse();
        P2.lookUpTableBackupReverse = am.getLookUpTableBackupReverse();

//        InputOutputController.serialize(P2.nets, P2.paths[2] + P2.netsFile);
//        InputOutputController.serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
//        InputOutputController.serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
        
        InputOutputController.P2serilize();
        message = new ADDHOSTRESPONSEMessage(am.getHostName(), am.getIP(), am.getPort(), MessageType.ADDHOSTRESPONSE);
        message.setResponse(ResponseType.SUCCESS);
        System.out.println("Added to Linda");
    }

    private void addupdatehostPrework() {
        UPDATEHOSTMessage um = (UPDATEHOSTMessage) message;
        
        // TS
        int index = -1;
        for (int i = 0; i < P2.nets.size(); i++) {
            if (P2.nets.get(i).hostName.equals(P2.hostName)) {
                index = i;
                break;
            }
        }

        Map<Integer, Map<Tuple, Integer>> tuplesOriginaltmp = new ConcurrentHashMap<>();
        Map<Integer, Map<Tuple, Integer>> tuplesBackuptmp = new ConcurrentHashMap<>();
        // old hosts 
        // TS
        if (!um.getaddHostName().equals(P2.hostName)) {
            int countOfHostBefore = P2.nets.size();
            int countOfSlotTransfer = P2.lookUpTableOriginalReverse.get(0).size() / (countOfHostBefore + 1);
            
            for (int i = 0; i < countOfSlotTransfer; i++) {
                int slotNumber = P2.lookUpTableOriginalReverse.get(index).get(i);
                Map<Tuple, Integer> maptmp = P2.tuplesOriginal.remove(slotNumber);
                if (maptmp != null) {
                    tuplesOriginaltmp.put(slotNumber, maptmp);
                }
            }
            for (int i = 0; i < countOfSlotTransfer; i++) {
                int slotNumber = P2.lookUpTableBackupReverse.get(index).get(i);
                Map<Tuple, Integer> maptmp = P2.tuplesBackup.remove(slotNumber);
                if (maptmp != null) {
                    tuplesBackuptmp.put(slotNumber, maptmp);
                }
            } 
        }

        // nets
        P2.nets = um.getNets();
        P2.netsMap = um.getNetsMap();

        // LUT_reverse
        P2.lookUpTableOriginalReverse = um.getLookUpTableOriginalReverse();
        P2.lookUpTableBackupReverse = um.getLookUpTableBackupReverse();

        // LUT
        P2.lookUpTableOriginal = um.getLookUpTableOriginal();
        P2.lookUpTableBackup = um.getLookUpTableBackup();

        // serialize
        InputOutputController.P2serilize();
        if (!um.getaddHostName().equals(P2.hostName)) {
            System.out.println("Add " + um.getaddHostName());
        }
        
        message = new ADDUPDATEHOSTRESPONSEMessage(um.getHostName(), um.getIP(), um.getPort(), MessageType.ADDUPDATEHOSTRESPONSE, tuplesOriginaltmp, tuplesBackuptmp);
    }

    private void outtuplespacePreWork() {
        OUTTUPLESPACEMessage om = (OUTTUPLESPACEMessage) message;
        P2.tuplesOriginal = om.gettuplesOriginal();
        P2.tuplesBackup = om.gettuplesBackup();
        om.setResponse(ResponseType.SUCCESS);
        
        // serialize
//        InputOutputController.serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
//        InputOutputController.serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
        InputOutputController.P2serilize();
        System.out.println("Added to Linda");
    }

    private void outPrework() {
        OUTMessage om = (OUTMessage) message;
        Tuple t = om.getTuple();
        int slotNumber = t.getSlotNumber();
        if (om.getIsMaster()) {
            P2.tuplesOriginal.putIfAbsent(slotNumber, new ConcurrentHashMap<>());
            int count = P2.tuplesOriginal.get(slotNumber).getOrDefault(t, 0);
            P2.tuplesOriginal.get(slotNumber).put(t, count + 1);
            om.setResponse(ResponseType.SUCCESS);
            InputOutputController.serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
            System.out.println("Put tuple " + t);
        } else {
            P2.tuplesBackup.putIfAbsent(slotNumber, new ConcurrentHashMap<>());
            int count = P2.tuplesBackup.get(slotNumber).getOrDefault(t, 0);
            P2.tuplesBackup.get(slotNumber).put(t, count + 1);
            om.setResponse(ResponseType.SUCCESS);
            InputOutputController.serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
            System.out.println("Put tuple " + t);
        }
    }

    private void rdPrework() {
        RDMessage rm = (RDMessage) message;
        Tuple t = rm.getTuple();
        int slotNumber = t.getSlotNumber();
        while (true) {
            if (rm.getIsMaster()) {
                if (P2.tuplesOriginal.containsKey(slotNumber) && P2.tuplesOriginal.get(slotNumber).containsKey(t)) {
                    rm.setResponse(ResponseType.SUCCESS);
                    System.out.println("Read tuple " + t);
                    break;
                }
            } else {
                if (P2.tuplesBackup.containsKey(slotNumber) && P2.tuplesBackup.get(slotNumber).containsKey(t)) {
                    rm.setResponse(ResponseType.SUCCESS);
                    System.out.println("Read tuple " + t);
                    break;
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
            }
        }
        
    }

    private void inPrework() {
        INMessage im = (INMessage) message;
        Tuple t = im.getTuple();
        int slotNumber = t.getSlotNumber();
        while (true) {
            if (im.getIsMaster()) {
                if (P2.tuplesOriginal.containsKey(slotNumber) && P2.tuplesOriginal.get(slotNumber).containsKey(t)) {
                    int count = P2.tuplesOriginal.get(slotNumber).get(t);
                    if (count == 1) {
                        P2.tuplesOriginal.get(slotNumber).remove(t);
                    } else {
                        P2.tuplesOriginal.get(slotNumber).put(t, count - 1);
                    }
                    im.setResponse(ResponseType.SUCCESS);
                    InputOutputController.serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
                    System.out.println("Remove tuple " + t);
                    break;
                } 
            } else {
                if (P2.tuplesBackup.containsKey(slotNumber) && P2.tuplesBackup.get(slotNumber).containsKey(t)) {
                    int count = P2.tuplesBackup.get(slotNumber).get(t);
                    if (count == 1) {
                        P2.tuplesBackup.get(slotNumber).remove(t);
                    } else {
                        P2.tuplesBackup.get(slotNumber).put(t, count - 1);
                    }
                    im.setResponse(ResponseType.SUCCESS);
                    InputOutputController.serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
                    System.out.println("Remove tuple " + t);
                    break;
                } 
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void rdbroadcastPrework() {
        RDBROADCASTMessage rm = (RDBROADCASTMessage) message;
        Tuple other = rm.getTuple();

        for (Map.Entry<Integer, Map<Tuple, Integer>> slotEntry : P2.tuplesOriginal.entrySet()) {
            Map<Tuple, Integer> slotMap = slotEntry.getValue();
            for (Map.Entry<Tuple, Integer> entry : slotMap.entrySet()) {
                Tuple tuple = entry.getKey();
                if (tuple.typeMatch(other)) {
                    rm.setTuple(tuple);
                    rm.setIsMaster(true);
                    rm.setResponse(ResponseType.SUCCESS);
                    return;
                }
            }
        }
        
        if (rm.getResponse() != ResponseType.SUCCESS) {
            for (Map.Entry<Integer, Map<Tuple, Integer>> slotEntry : P2.tuplesBackup.entrySet()) {
                Map<Tuple, Integer> slotMap = slotEntry.getValue();
                for (Map.Entry<Tuple, Integer> entry : slotMap.entrySet()) {
                    Tuple tuple = entry.getKey();
                    if (tuple.typeMatch(other)) {
                        rm.setTuple(tuple);
                        rm.setResponse(ResponseType.SUCCESS);
                        return;
                    }
                }
            }
        }
    }

    private void inbroadcastPrework() {
        INBROADCASTMessage rm = (INBROADCASTMessage) message;
        Tuple other = rm.getTuple();

        for (Map.Entry<Integer, Map<Tuple, Integer>> slotEntry : P2.tuplesOriginal.entrySet()) {
            Map<Tuple, Integer> slotMap = slotEntry.getValue();
            for (Map.Entry<Tuple, Integer> entry : slotMap.entrySet()) {
                Tuple tuple = entry.getKey();
                if (tuple.typeMatch(other)) {
                    rm.setTuple(tuple);
                    rm.setIsMaster(true);
                    rm.setResponse(ResponseType.SUCCESS);
                    return;
                }
            }
        }
        
        if (rm.getResponse() != ResponseType.SUCCESS) {
            for (Map.Entry<Integer, Map<Tuple, Integer>> slotEntry : P2.tuplesBackup.entrySet()) {
                Map<Tuple, Integer> slotMap = slotEntry.getValue();
                for (Map.Entry<Tuple, Integer> entry : slotMap.entrySet()) {
                    Tuple tuple = entry.getKey();
                    if (tuple.typeMatch(other)) {
                        rm.setTuple(tuple);
                        rm.setResponse(ResponseType.SUCCESS);
                        return;
                    }
                }
            }
        } 
    }

    private void  rebootPrework() {
        REBOOTMessage m = (REBOOTMessage)message;
        // update
        P2.nets = m.getNets();
        P2.netsMap = m.getNetsMap();
        Host rebootHost = m.getHost();
        // find index
        int rebootHostIndex = -1;
        int myIndex = -2;
        for (int i = 0; i < P2.nets.size(); i++) {
            if (P2.nets.get(i).equals(rebootHost)) {
                rebootHostIndex = i;
            }
            if (P2.nets.get(i).getHostName().equals(P2.hostName)) {
                myIndex = i;
            }
        }
        // send my backup to its original
        if ((rebootHostIndex + 1) % P2.nets.size() == myIndex) {
            m.setTuplesOriginal(P2.tuplesBackup);
            
            m.setLookUpTableBackup(P2.lookUpTableBackup);
            m.setLookUpTableOriginal(P2.lookUpTableOriginal);
            m.setLookUpTableBackupReverse(P2.lookUpTableBackupReverse);
            m.setLookUpTableOriginalReverse(P2.lookUpTableOriginalReverse);
            
            m.setResponse(ResponseType.SUCCESS);
        }
        
        // send my original to its backup
        if ((myIndex + 1) % P2.nets.size() == rebootHostIndex) {
            m.setTuplesBackup(P2.tuplesOriginal);
            m.setResponse(ResponseType.SUCCESS);
        }
        
        // serialization
        InputOutputController.serialize(P2.nets, P2.paths[2] + P2.netsFile);
        
        System.out.println(rebootHost.getHostName() + " reboots");
    }
    
    void deleteforwardPrework() {
        DELETEFORWARDMessage fm = (DELETEFORWARDMessage) message;
        fm.setResponse(ResponseType.SUCCESS);
        System.out.println("Master receives a forward message");
    }
    
    private void intuplespacePreWork() {
        INTUPLESPACEMessage im = (INTUPLESPACEMessage) message;
        if (im.getIsDeleted()) {
            im.settuplesOriginal(P2.tuplesOriginal);
            P2.tuplesOriginal = new ConcurrentHashMap<>();
        }
        im.settuplesBackup(P2.tuplesBackup);
        P2.tuplesBackup = new ConcurrentHashMap<>();
        im.setResponse(ResponseType.SUCCESS);
        
//        System.out.println("ServerThread[intuplespacePreWork]: Tuple space is removed");
    }

    private void deleteupdatehostPreWork() {
        DELETEUPDATEHOSTMessage dm = (DELETEUPDATEHOSTMessage)message;
        if (!P2.hostName.equals(dm.getdeleteHostName())) {
            P2.nets = dm.getNets();
            P2.netsMap = dm.getNetsMap();
            P2.lookUpTableOriginal = dm.getLookUpTableOriginal();
            P2.lookUpTableBackup = dm.getLookUpTableBackup();
            P2.lookUpTableOriginalReverse = dm.getLookUpTableOriginalReverse();
            P2.lookUpTableBackupReverse = dm.getLookUpTableBackupReverse();
            P2.tuplesOriginal.putAll(dm.gettuplesOriginal());
            P2.tuplesBackup.putAll(dm.gettuplesBackup());
            message = new DELETEUPDATEHOSTRESPONSEMessage(null, null, null, MessageType.DELETEUPDATEHOSTRESPONSE, dm.getdeleteHostName());
            message.setResponse(ResponseType.SUCCESS);
            System.out.println("delete " + dm.getdeleteHostName());
        } else {
            message = new DELETEUPDATEHOSTRESPONSEMessage(null, null, null, MessageType.DELETEUPDATEHOSTRESPONSE, dm.getdeleteHostName());
            message.setResponse(ResponseType.SUCCESS);
        }
    }
    
    private void deleteupdatehostPostWork() {
        if (P2.hostName.equals(((DELETEUPDATEHOSTRESPONSEMessage)message).getdeleteHostName())) {
            System.out.println("deleted ");
            InputOutputController.deleteFile(P2.paths);
            System.exit(0);
        }
    }
    
    
    synchronized private void hostWork() {
        Queue<Message> messageSend = null;
        if (message.getType() == MessageType.ADDFORWARD) {
            messageSend = ((ADDFORWARDMessage)message).getMessageQueue();
            HostController.initial(messageSend);
            HostController.run();
        } else {
            messageSend = ((DELETEFORWARDMessage)message).getMessageQueue();
            HostController.initial(messageSend);
            HostController.deleteHosts();
        }
    }
}
