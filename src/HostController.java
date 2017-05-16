
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class HostController {
    static private Queue<Message> messageSend;
    static private Queue<Message> messageReceive;

    static void initial(Queue<Message> messageSend) {
        HostController.messageSend = messageSend;
        messageReceive = new ConcurrentLinkedQueue<>();
    }

    static void run() {
        int n = messageSend.size();
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            HostControllerThread t = new HostControllerThread(messageSend.poll());
            if (t.isConnected) {
                threads[i] = new Thread(t);
                threads[i].start();
            }
        }

        for (Thread t : threads) {
            try {
                if (t != null) {
                    t.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //        System.out.println("HostController[]: " + n + " sent");
        //        System.out.println("HostController[]: " + messageReceive.size() + " recieved");
        
        // may be empty
        if (!messageReceive.isEmpty()) {
            switch (messageReceive.peek().getType()) {
            case ASKNAME:
                asknamePostwork();
                break;
            case ADDHOSTRESPONSE:
                addhostTailwork();
                break;
            case ADDUPDATEHOSTRESPONSE:
                addupdatehostPostwork();
                break;
            case OUTTUPLESPACE:
                outtuplespaceTailwork();
                break;
            case INTUPLESPACE:
                deleteHostPostwork();
                break;
            case DELETEUPDATEHOSTRESPONSE:
                deleteHostTailwork();
                break;
            case DELETEFORWARD:
                deleteforwardTailwork();
                break;
            default:
                break;
            }
        }
    }

    /*---------------------------------------add host------------------------------------------------*/
    private static void asknamePostwork() {
        // true if and only if the system is initialized at beginning (there will at least 2 hosts in the system afterwards)
        boolean isInitialAdd = (P2.nets.size() == 1);
        Set<String> nameSet = new HashSet<>();
        nameSet.addAll(P2.netsMap.keySet());

        // 1. check
        while (!messageReceive.isEmpty()) {
            Message am = messageReceive.poll();
            if (am.getResponse() == ResponseType.ERROR) {   // host name is wrong
                System.out.println("Host name(" + am.getHostName() + ") is wrong, skip the host.");
            } else if (!nameSet.add(am.getHostName())) {    // duplicated host name
                System.out.println("Duplicate host name(" + am.getHostName() + "), skip the host.");
            } else {
                if (isInitialAdd) {
                    messageSend.offer(new ADDHOSTMessage(am.getHostName(), am.getIP(), am.getPort(), MessageType.ADDHOST));
                } else {
                    messageSend.offer(new UPDATEHOSTMessage(am.getHostName(), am.getIP(), am.getPort(), MessageType.ADDUPDATEHOST, am.getHostName()));
                }
            }
        }

        if (isInitialAdd && !messageSend.isEmpty()) {
            addhostPrework();
        } else if (!isInitialAdd && !messageSend.isEmpty()) {
            updatehostsPrework();
        }
    }

    // case1: when linda has only one host, initialize all the hosts together
    private static void addhostPrework() {
        /*
         1. nets: need to update
         2. LUT_reverse: need to update
         3. LUT: need to update
         4. tuple: do nothing
         */

        // nets
        for (Message am : messageSend) {
            P2.nets.add(new Host(am.getHostName(), am.getIP(), am.getPort()));
            P2.netsMap.put(am.getHostName(), new Host(am.getHostName(), am.getIP(), am.getPort()));
        }
        int countOfHost = P2.nets.size();
        int slotSize = P2.NUMBER_OF_SLOTS / countOfHost;

        // LUT_Original_reverse
        P2.lookUpTableOriginalReverse = new ArrayList<>();
        for (int i = 0; i < countOfHost; i++) {
            List<Integer> list = new ArrayList<>();
            for (int j = i * slotSize; j < (i + 1) * slotSize; j++) {
                list.add(j);
            }
            if (i == countOfHost - 1) {
                for (int j = countOfHost * slotSize; j < P2.NUMBER_OF_SLOTS; j++) {
                    list.add(j);
                }
            }
            P2.lookUpTableOriginalReverse.add(list);
        }

        // LUT_Backup_reverse
        P2.lookUpTableBackupReverse = new ArrayList<>();
        for (int i = 0; i < countOfHost; i++) {
            P2.lookUpTableBackupReverse.add(new ArrayList<>(P2.lookUpTableOriginalReverse.get((i - 1 + countOfHost) % countOfHost)));
        }

        // LUT_original
        for (int i = 0; i < countOfHost; i++) {
            String hostName = P2.nets.get(i).getHostName();
            for (int slot : P2.lookUpTableOriginalReverse.get(i)) {
                P2.lookUpTableOriginal[slot] = hostName;
            }
        }

        // LUT_backup
        for (int i = 0; i < countOfHost; i++) {
            String hostName = P2.nets.get(i).getHostName();
            for (int slot : P2.lookUpTableBackupReverse.get(i)) {
                P2.lookUpTableBackup[slot] = hostName;
            }
        }

        // TS_o

        // TS_b

        for (Message m : messageSend) {
            ((ADDHOSTMessage) m).setNets(P2.nets);
            ((ADDHOSTMessage) m).setNetsMap(P2.netsMap);
            ((ADDHOSTMessage) m).setLookUpTableOriginal(P2.lookUpTableOriginal);
            ((ADDHOSTMessage) m).setLookUpTableBackup(P2.lookUpTableBackup);
            ((ADDHOSTMessage) m).setLookUpTableOriginalReverse(P2.lookUpTableOriginalReverse);
            ((ADDHOSTMessage) m).setLookUpTableBackupReverse(P2.lookUpTableBackupReverse);
        }
        // serialize
        InputOutputController.serialize(P2.nets, P2.paths[2] + P2.netsFile);
        run();
    }

    private static void addhostTailwork() {
        messageReceive.clear();
        System.out.println("Linda system initialized.");
    }

    private static void updatehostsPrework() {
        Queue<Message> messageSendcp = new ConcurrentLinkedQueue<>(messageSend);
        messageSend.clear();
        for (Message m : messageSendcp) {
            updateHostPrework(m);
        }
    }

    private static void updateHostPrework(Message message) {
        // !!!copy
        List<Host> netsCP = new ArrayList<>(P2.nets);
        Map<String, Host> netsMapCP = new HashMap<>(P2.netsMap);
        String[] lookUpTableOriginalCP = Arrays.copyOf(P2.lookUpTableOriginal, P2.lookUpTableOriginal.length);
        String[] lookUpTableBackupCP = Arrays.copyOf(P2.lookUpTableBackup, P2.lookUpTableBackup.length);
        List<List<Integer>> lookUpTableOriginalReverseCP = new ArrayList<>(P2.lookUpTableOriginalReverse);
        List<List<Integer>> lookUpTableBackupReverseCP = new ArrayList<>(P2.lookUpTableBackupReverse);

        // 1/n
        int countOfHostBefore = netsCP.size();
        int countOfSlotTransfer = lookUpTableOriginalReverseCP.get(0).size() / (countOfHostBefore + 1);

        // LUT_reverse_original
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < countOfHostBefore; i++) {
            list.addAll(new ArrayList<>(lookUpTableOriginalReverseCP.get(i).subList(0, countOfSlotTransfer)));
            lookUpTableOriginalReverseCP.set(i,
                    new ArrayList<>(lookUpTableOriginalReverseCP.get(i).subList(countOfSlotTransfer, lookUpTableOriginalReverseCP.get(i).size())));
        }
        lookUpTableOriginalReverseCP.add(list);

        // LUT_reverse_backup
        list = new ArrayList<>();
        for (int i = 0; i < countOfHostBefore; i++) {
            list.addAll(new ArrayList<>(lookUpTableBackupReverseCP.get((i + 1) % countOfHostBefore).subList(0, countOfSlotTransfer)));
            lookUpTableBackupReverseCP.set((i + 1) % countOfHostBefore, new ArrayList<>(lookUpTableBackupReverseCP.get((i + 1) % countOfHostBefore)
                    .subList(countOfSlotTransfer, lookUpTableBackupReverseCP.get((i + 1) % countOfHostBefore).size())));
        }
        lookUpTableBackupReverseCP.add(list);
        swap(lookUpTableBackupReverseCP, 0, lookUpTableBackupReverseCP.size() - 1);

        // nets
        netsCP.add(new Host(message.getHostName(), message.getIP(), message.getPort()));
        netsMapCP.put(message.getHostName(), new Host(message.getHostName(), message.getIP(), message.getPort()));

        // LUT_original
        String addHostName = message.getHostName();
        for (int slot : lookUpTableOriginalReverseCP.get(countOfHostBefore)) {
            lookUpTableOriginalCP[slot] = addHostName;
        }

        // LUT_backup
        for (int slot : lookUpTableBackupReverseCP.get(countOfHostBefore)) {
            lookUpTableBackupCP[slot] = addHostName;
        }

        // !!! LUT_backup
        for (int slot : lookUpTableBackupReverseCP.get(0)) {
            lookUpTableBackupCP[slot] = P2.hostName;
        }

        for (Host h : netsCP) {
            UPDATEHOSTMessage um = new UPDATEHOSTMessage(h.hostName, h.IP, h.port, MessageType.ADDUPDATEHOST, addHostName);
            um.setNets(netsCP);
            um.setNetsMap(netsMapCP);
            um.setLookUpTableOriginal(lookUpTableOriginalCP);
            um.setLookUpTableBackup(lookUpTableBackupCP);
            um.setLookUpTableOriginalReverse(lookUpTableOriginalReverseCP);
            um.setLookUpTableBackupReverse(lookUpTableBackupReverseCP);
            messageSend.offer(um);
        }
        run();
    }

    private static void swap(List<List<Integer>> list, int i, int j) {
        List<Integer> temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private static void addupdatehostPostwork() {
        Map<Integer, Map<Tuple, Integer>> tuplesOriginal = new ConcurrentHashMap<>();
        Map<Integer, Map<Tuple, Integer>> tuplesBackup = new ConcurrentHashMap<>();
        while (!messageReceive.isEmpty()) {
            ADDUPDATEHOSTRESPONSEMessage m = (ADDUPDATEHOSTRESPONSEMessage) messageReceive.poll();
            tuplesOriginal.putAll(m.getTuplesOriginal());
            tuplesBackup.putAll(m.getTuplesBackup());
        }

        Host newHost = P2.nets.get(P2.nets.size() - 1);
        // new host update TS
        Message m = new OUTTUPLESPACEMessage(newHost.getHostName(), newHost.getIP(), newHost.getPort(), MessageType.OUTTUPLESPACE, tuplesOriginal,
                P2.tuplesBackup);
        // master update TS_backup
        P2.tuplesBackup = tuplesBackup;

        // send TS to new host 
        messageSend.add(m);

        //         InputOutputController.serialize(P2.nets, P2.paths[2] + P2.nets);
        //         InputOutputController.serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
        // serialization
        InputOutputController.serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
        run();
    }

    private static void outtuplespaceTailwork() {
        messageReceive.clear();
    }

    /*---------------------------------------delete host------------------------------------------------*/
    static void deleteHosts() {
        Queue<Message> messageSendcp = new ConcurrentLinkedQueue<>(messageSend);
        messageSend.clear();

        // check whether delete master
        Message deleteMaster = null;
        int n = messageSendcp.size();
        for (int i = 0; i < n; i++) {
            Message m = messageSendcp.poll();
            if (!m.getHostName().equals(P2.hostName)) {
                messageSendcp.offer(m);
            } else {
                deleteMaster = m;
            }
        }

        for (Message m : messageSendcp) {
            if (!P2.netsMap.containsKey(m.getHostName())) {
                System.out.println("Host name = " + m.getHostName() + " is wrong, skip this host");
                continue;
            } else if (P2.nets.size() == 2) {
                System.out.println("Linda System must have at least 2 hosts, can't delete host now.");
                return;
            }
            Host deleteHost = P2.netsMap.get(m.getHostName());
            Message im = new INTUPLESPACEMessage(deleteHost.hostName, deleteHost.IP, deleteHost.port, MessageType.INTUPLESPACE, true);
            messageSend.offer(im);

            // delete Host's next host
            for (int i = 0; i < P2.nets.size(); i++) {
                if (P2.nets.get(i).hostName.equals(m.getHostName())) {
                    Host nextHost = P2.nets.get((i + 1) % P2.nets.size());
                    im = new INTUPLESPACEMessage(nextHost.hostName, nextHost.IP, nextHost.port, MessageType.INTUPLESPACE, false);
                    messageSend.offer(im);
                }
            }
            run();
        }

        // delete master at last, forward to the second host in nets
        if (deleteMaster != null) {
            Queue<Message> list = new LinkedList<>();
            list.offer(deleteMaster);
            Message m = new DELETEFORWARDMessage(P2.nets.get(1).hostName, P2.nets.get(1).IP, P2.nets.get(1).port, MessageType.DELETEFORWARD, list);
            messageSend.offer(m);
            run();
        }
    }

    static private void deleteHostPostwork() {
        INTUPLESPACEMessage m = null;
        while (!messageReceive.isEmpty()) {
            m = (INTUPLESPACEMessage) messageReceive.poll();
            if (m.getIsDeleted()) {
                messageReceive.clear();
                break;
            }
        }

        Map<Integer, Map<Tuple, Integer>> tuplesOriginalTmp = m.gettuplesOriginal();
        Map<Integer, Map<Tuple, Integer>> tuplesBackupTmp = m.gettuplesBackup();

        // !!!copy
        List<Host> netsCP = new ArrayList<>(P2.nets);
        Map<String, Host> netsMapCP = new HashMap<>(P2.netsMap);
        String[] lookUpTableOriginalCP = Arrays.copyOf(P2.lookUpTableOriginal, P2.lookUpTableOriginal.length);
        String[] lookUpTableBackupCP = Arrays.copyOf(P2.lookUpTableBackup, P2.lookUpTableBackup.length);
        List<List<Integer>> lookUpTableOriginalReverseCP = new ArrayList<>(P2.lookUpTableOriginalReverse);
        List<List<Integer>> lookUpTableBackupReverseCP = new ArrayList<>(P2.lookUpTableBackupReverse);

        int index = -1;
        for (int i = 0; i < netsCP.size(); i++) {
            if (m.getHostName().equals(netsCP.get(i).hostName)) {
                index = i;
                break;
            }
        }

        // nets
        netsCP.remove(index);
        netsMapCP.remove(m.getHostName());

        int countOfHost = netsCP.size();
        int countOfSlotTransfer = lookUpTableOriginalReverseCP.get(0).size() / countOfHost;

        // LUT_reverse: step1
        List<Integer> list1 = new ArrayList<>(lookUpTableOriginalReverseCP.get(index));
        List<Integer> list2 = new ArrayList<>(lookUpTableOriginalReverseCP.get(index));

        lookUpTableBackupReverseCP.set((index + 1) % (countOfHost + 1), lookUpTableBackupReverseCP.get(index));
        lookUpTableOriginalReverseCP.remove(index);
        lookUpTableBackupReverseCP.remove(index);

        // LUT_reverse: step2
        // LUT_original + LUT_reverse_original
        for (int i = 0; i < countOfHost; i++) {
            List<Integer> temp = list1.subList(0, countOfSlotTransfer);
            list1 = list1.subList(countOfSlotTransfer, list1.size());
            // LUT_o
            for (Integer slot : temp) {
                lookUpTableOriginalCP[slot] = netsCP.get(i).hostName;
            }
            // LUT_reverse_o
            lookUpTableOriginalReverseCP.get(i).addAll(temp);

            if (i == countOfHost - 1) {
                for (Integer slot : list1) {
                    lookUpTableOriginalCP[slot] = netsCP.get(i).hostName;
                }
                lookUpTableOriginalReverseCP.get(i).addAll(list1);
            }
        }

        // LUT_reverse_backup
        for (int i = 0; i < countOfHost; i++) {
            List<Integer> temp = list2.subList(0, countOfSlotTransfer);
            list2 = list2.subList(countOfSlotTransfer, list2.size());
            // LUT_o
            for (Integer slot : temp) {
                lookUpTableBackupCP[slot] = netsCP.get((i + 1) % countOfHost).hostName;
            }
            // LUT_reverse_o
            lookUpTableBackupReverseCP.get((i + 1) % countOfHost).addAll(temp);
            if (i == countOfHost - 1) {
                for (Integer slot : list2) {
                    lookUpTableBackupCP[slot] = netsCP.get((i + 1) % countOfHost).hostName;
                }
                lookUpTableBackupReverseCP.get((i + 1) % countOfHost).addAll(list2);
            }
        }

        // ts
        List<Map<Integer, Map<Tuple, Integer>>> tuplesOriginalList = new ArrayList<>();
        List<Map<Integer, Map<Tuple, Integer>>> tuplesBackupList = new ArrayList<>();
        for (int i = 0; i < netsCP.size(); i++) {
            tuplesOriginalList.add(new ConcurrentHashMap<>());
            tuplesBackupList.add(new ConcurrentHashMap<>());
        }

        for (Map.Entry<Integer, Map<Tuple, Integer>> entry : tuplesOriginalTmp.entrySet()) {
            Integer slot = entry.getKey();
            String hostName = lookUpTableOriginalCP[slot];
            int indexTmp = -1;
            for (int i = 0; i < netsCP.size(); i++) {
                if (netsCP.get(i).hostName.equals(hostName)) {
                    indexTmp = i;
                    break;
                }
            }
            tuplesOriginalList.get(indexTmp).put(slot, entry.getValue());
            tuplesBackupList.get((indexTmp + 1) % countOfHost).put(slot, entry.getValue());
        }

        tuplesBackupList.get((index) % (countOfHost)).putAll(tuplesBackupTmp);

        for (int i = 0; i < netsCP.size(); i++) {
            Message dm = new DELETEUPDATEHOSTMessage(netsCP.get(i).hostName, netsCP.get(i).IP, netsCP.get(i).port, MessageType.DELETEUPDATEHOST,
                    m.getHostName(), netsCP, netsMapCP, lookUpTableOriginalCP, lookUpTableBackupCP, lookUpTableOriginalReverseCP,
                    lookUpTableBackupReverseCP, tuplesOriginalList.get(i), tuplesBackupList.get(i));
            messageSend.offer(dm);
        }

        messageSend.offer(new DELETEUPDATEHOSTMessage(m.hostName, m.IP, m.port, MessageType.DELETEUPDATEHOST, m.getHostName(), null, null, null, null,
                null, null, null, null));
        run();
    }

    static void deleteHostTailwork() {
        messageReceive.clear();
    }

    static void deleteforwardTailwork() {
        messageReceive.clear();
        System.out.println("Forward to the second master");
    }

    static class HostControllerThread implements Runnable {
        private Socket socket;
        Message message;
        private boolean isConnected;

        private ObjectOutputStream out;
        private ObjectInputStream in;

        HostControllerThread(Message message) {
            try {
                socket = new Socket(message.IP, message.port);
                this.message = message;
                isConnected = true;
            } catch (IOException e) {
                System.out.println("Failed to connect to " + message.getHostName());
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(message);
                out.flush();

                message = (Message) in.readObject();

                inqueueMessageReceive();
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void inqueueMessageReceive() {
            messageReceive.add(message);
        }
    }

}
