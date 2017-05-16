
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class Client {
    private Queue<Message> messageSend;
    private Queue<Message> messageReceive;

    public Client(Queue<Message> messageSend) {
        this.messageSend = messageSend;
        this.messageReceive = new ConcurrentLinkedQueue<>();
    }

    void run() {
        MessageType type = messageSend.peek().getType();
        switch (type) {
        case ADDFORWARD:
        case DELETEFORWARD:
        case OUT:
            unblockingMessage();
            break;
        case REBOOT:
        case RD:
        case IN:
        case RDBROADCAST:
        case INBROADCAST:
            blockingMessage();
            break;
        default:
            break;
        }
    }

    private void unblockingMessage() {
        int n = messageSend.size();
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            Message m = messageSend.poll();
            ClientThread t = new ClientThread(m);
            if (t.isConnected) {
                threads[i] = new Thread(t);
            } else if (m.getType() == MessageType.DELETEFORWARD) {
                    System.out.println("Master was deleted, please try again.");
            }
            
        }

        for (Thread t : threads) {
            try {
                if (t != null) {
                    // guarantee order, out to original first, backup second
                    t.start();
                    t.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        System.out.println("Client[]: " + n + " sent");
//        System.out.println("Client[]: " + messageReceive.size() + " recieved");

        switch (messageReceive.peek().getType()) {
        case ADDFORWARD:
        case DELETEFORWARD:
            forwardTailwork();
            break;
        case OUT:
            outTailwork();
            break;
        default:
            break;
        }
        
    }

    private void blockingMessage() {
        Queue<Message> messageSendcp = new LinkedList<>(messageSend);
        int n = messageSend.size();
        Thread[] threads = new Thread[n];
        boolean finished = false;

        while (!finished) {
            messageSend = new LinkedList<>(messageSendcp);
            for (int i = 0; i < n; i++) {
                Message m = messageSend.poll();
                ClientThread t = new ClientThread(m);
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

//            System.out.println("Client[]: " + n + " sent");
//            System.out.println("Client[]: " + messageReceive.size() + " recieved");

            while (!messageReceive.isEmpty()) {
                if (messageReceive.peek().getResponse() == ResponseType.SUCCESS) {
                    finished = true;
                    break;
                } else {
                    messageReceive.poll();
                }
            }
        }

        switch (messageReceive.peek().getType()) {
        case REBOOT:
            rebootTailwork();
            break;
        case RD:
            rdTailwork();
            break;
        case IN:
            inTailwork();
            break;
        case RDBROADCAST:
            rdbroadcastPostwork();
            break;
        case INBROADCAST:
            inbroadcastPostwork();
            break;
        default:
            break;
        }
    }

    private void forwardTailwork() {
        messageReceive.clear();
        System.out.println("Forward to master");
    }

    private void outTailwork() {
        // since master receive tuple first and response second, only check the first response
        OUTMessage om = (OUTMessage) messageReceive.poll();
        if (om.getIsMaster()) {
            System.out.println("Out on " + om.getHostName() + "(original)");
        } else {
            System.out.println("Out on " + om.getHostName() + "(backup)");
        }
        messageReceive.clear();
        return;
    }

    private void rdTailwork() {
        RDMessage bm = null;
        while (!messageReceive.isEmpty()) {
            RDMessage rm = (RDMessage) messageReceive.poll();
            if (rm.getIsMaster()) {
                System.out.println("Read tuple " + rm.getTuple() + " on " + rm.getHostName() + "(original)");
                messageReceive.clear();
                return;
            } else {
                bm = rm;
            }
        }
        System.out.println("Read tuple " + bm.getTuple() + " on " + bm.getHostName() + "(backup)");
    }

    private void inTailwork() {
        INMessage bm = null;
        while (!messageReceive.isEmpty()) {
            INMessage im = (INMessage) messageReceive.poll();
            if (im.getIsMaster()) {
                System.out.println("Get tuple " + im.getTuple() + " on " + im.getHostName() + "(original)");
                messageReceive.clear();
                return;
            } else {
                bm = im;
            }
        }
        System.out.println("Get tuple " + bm.getTuple() + " on " + bm.getHostName() + "(backup)");
    }

    private void rdbroadcastPostwork() {
        // 不能保证在original和backup都有的情况下，original一定回success,因为虽然out一定是先out给original，再out给backup，但broadcast的顺序是不确定的
        // e.g
        // original rdbroadcast + out
        // backup                       out + rdbroadcast
        // -> 用rd来保证顺序
        RDBROADCASTMessage m = (RDBROADCASTMessage) messageReceive.poll();

        Tuple t = m.getTuple();
        int slotNumber = t.getSlotNumber();
        // order doesn't matter
        String backupHostName = P2.lookUpTableBackup[slotNumber];
        Host backupHost = P2.netsMap.get(backupHostName);
        RDMessage rm = new RDMessage(backupHost.getHostName(), backupHost.getIP(), backupHost.getPort(), MessageType.RD, t, false);
        messageSend.offer(rm);
        
        String originalHostName = P2.lookUpTableOriginal[slotNumber];
        Host originalHost = P2.netsMap.get(originalHostName);
        rm = new RDMessage(originalHost.getHostName(), originalHost.getIP(), originalHost.getPort(), MessageType.RD, t, true);
        messageSend.offer(rm);

        messageReceive.clear();
        run();

    }

    private void inbroadcastPostwork() {
        INBROADCASTMessage m = (INBROADCASTMessage) messageReceive.poll();

        Tuple t = m.getTuple();
        int slotNumber = t.getSlotNumber();
        // order doesn't matter
        String backupHostName = P2.lookUpTableBackup[slotNumber];
        Host backupHost = P2.netsMap.get(backupHostName);
        INMessage im = new INMessage(backupHost.getHostName(), backupHost.getIP(), backupHost.getPort(), MessageType.IN, t, false);
        messageSend.offer(im);
        
        String originalHostName = P2.lookUpTableOriginal[slotNumber];
        Host originalHost = P2.netsMap.get(originalHostName);
        im = new INMessage(originalHost.getHostName(), originalHost.getIP(), originalHost.getPort(), MessageType.IN, t, true);
        messageSend.offer(im);

        messageReceive.clear();
        run();
    }

    private void rebootTailwork() {
        while (!messageReceive.isEmpty()) {
            REBOOTMessage m = (REBOOTMessage) (messageReceive.poll());
            if (m.getTuplesOriginal() != null) {
                P2.tuplesOriginal = m.getTuplesOriginal();
                //
                P2.lookUpTableOriginal = m.getLookUpTableOriginal();
                P2.lookUpTableBackup = m.getLookUpTableBackup();
                P2.lookUpTableOriginalReverse = m.getLookUpTableOriginalReverse();
                P2.lookUpTableBackupReverse = m.getLookUpTableBackupReverse();
            }
            if (m.getTuplesBackup() != null) {
                P2.tuplesBackup = m.getTuplesBackup();
            }
        }

        System.out.println("Reboot successfully");
    }

    class ClientThread implements Runnable {
        private Socket socket;
        private Message message;
        private boolean isConnected;

        private ObjectOutputStream out;
        private ObjectInputStream in;

        ClientThread(Message message) {
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
