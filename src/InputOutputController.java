
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class InputOutputController {
    static boolean isPathExist(String path) {
        File dir = new File(path);
        boolean exist = !dir.mkdir();
        return exist;
    }
    
    static String getFileName(String path){
        File dir = new File(path);
        File[] fList = dir.listFiles();
        return fList[0].getName();
    }

    static void addFile(String[] paths, String path, String netsFile, String tuplesOriginalFile, String tuplesBackupFile){
        File dir = new File(path);
        dir.mkdirs();

        dir = new File(path + netsFile);           
        try {
            dir.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dir = new File(path + tuplesOriginalFile);  
        try {
            dir.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dir = new File(path + tuplesBackupFile);   
        try {
            dir.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < paths.length; i++) {
            dir = new File(paths[i]);
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);
        }

        dir = new File(path + netsFile);
        dir.setReadable(true, false);
        dir.setWritable(true, false);

        dir = new File(path + tuplesOriginalFile);
        dir.setReadable(true, false);
        dir.setWritable(true, false);

        dir = new File(path + tuplesBackupFile);
        dir.setReadable(true, false);
        dir.setWritable(true, false);
    }

    static void P2serilize() {
        serialize(P2.nets, P2.paths[2] + P2.netsFile);
        serialize(P2.tuplesOriginal, P2.paths[2] + P2.tuplesOriginal);
        serialize(P2.tuplesBackup, P2.paths[2] + P2.tuplesBackup);
    }
    static void deleteFile(String[] paths) {
        Path directory = Paths.get(paths[0]);
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            
        }
    }

    static <E> void serialize(E obj, String filename) {
        FileOutputStream fout = null;
        ObjectOutputStream out = null;
        try {
            fout = new FileOutputStream(filename);
            out = new ObjectOutputStream(fout);
            out.writeObject(obj);
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    static <E> E deSerialize(String filename) {
        E transOb = null;
        FileInputStream fis = null;
        ObjectInputStream fin = null;
        try {
            fis = new FileInputStream(filename);
            fin = new ObjectInputStream(fis);
            transOb = (E) fin.readObject();
            fin.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return transOb;
    }
}
