
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

class Tuple implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Object> list;

    Tuple(List<Object> list) {
        this.list = list;
    }

    void set(int index, Object o) {
        list.set(index, o);
    }

    // check whether this tuple matches the input tuple
    boolean typeMatch(Tuple other) {
        if (list.size() != other.list.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            // exact match
            if (other.list.get(i).getClass() == ArrayList.class) {
                @SuppressWarnings("unchecked")
                ArrayList<String> otherlist = (ArrayList<String>) (other.list.get(i));
                if (!list.get(i).getClass().toString().equals(otherlist.get(1))) {
                    return false;
                }
                // variable match
            } else if (list.get(i).getClass() != other.list.get(i).getClass() || !list.get(i).equals(other.list.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Tuple other = (Tuple) o;
        if (list.size() != other.list.size()) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            Object o1 = list.get(i);
            Object o2 = other.list.get(i);

            // variable match
            if (o1.getClass() != o2.getClass() || !o1.equals(o2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        byte[] digest = null;
        try {
            digest = MD5Sum(this);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5Sum error.");
        }
        BigInteger bigI = new BigInteger(digest);
        BigInteger bigImod = new BigInteger(Integer.MAX_VALUE + "");
        return Integer.parseInt(bigI.mod(bigImod).toString());
    }

    // also for MD5Sum and type matching
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        String prefix = "";
        for (Object o : list) {
            if (o.getClass() == String.class) {
                sb.append(prefix).append("\"").append(o).append("\"");
            } else {
                sb.append(prefix).append(o);
            }

            prefix = ",";
        }
        sb.append(")");
        return sb.toString();
    }

    int getSlotNumber() {
        byte[] digest = null;
        try {
            digest = MD5Sum(this);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5Sum error.");
        }

        return ((digest[0] & 0xFF) << 8 | (digest[1] & 0xFF));
//        return ((digest[15] & 0xFF) << 8 | (digest[14] & 0xFF));     // if is little endian
    }

    static byte[] MD5Sum(Tuple tuple) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(tuple.toString().getBytes());
        return digest;
    }
}