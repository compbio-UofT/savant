/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author mfiume
 */
public class IOUtils {

    public static String readString(RandomAccessFile raf) throws IOException {

        int len = raf.readInt();
        byte[] str = new byte[len];
        for (int i = 0; i < len; i++) {
            str[i] = raf.readByte();
        }
        
        return new String(str);
    }

     public static void writeString(RandomAccessFile raf, String s) throws IOException {
         writeFixedLengthString(raf,s,s.length());
     }

    public static void writeFixedLengthString(RandomAccessFile raf, String s, int len) throws IOException {

        int pad = len - s.length();

        long before = raf.getFilePointer();

        raf.writeInt(len);
        if (!s.equals("")) { raf.writeBytes(s.substring(0, Math.min(s.length(),len))); }
        while (pad > 0) {
            raf.writeBytes(" ");
            pad--;
        }

        long after = raf.getFilePointer();
        
        //System.out.println("\tWriting " + len + " chars from [" + s + "] padded by " + (len - s.length()) + " = " + (after-before) + " bytes");
    }

    public static String readExactlyChars(RandomAccessFile raf, int len) throws IOException {
        char[] str = new char[len];
        for (int i = 0; i < len; i++) { str[i] = raf.readChar(); }
        return new String(str).trim();
    }

    public static int boolToInt(boolean b) { if (b) { return 1; } else { return 0; } }
    public static boolean intToBool(int i) { if (i == 1) { return true; } else { return false; } }

    public static void deleteFile(String path) {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
    }

    public static RandomAccessFile openNewFile(String path) throws FileNotFoundException, IOException {
        deleteFile(path);
        return openFile(path);
    }

    public static RandomAccessFile openFile(String path) throws FileNotFoundException, IOException {
        return openFile(path,true);
    }

    public static RandomAccessFile openFile(String path, boolean seekToEnd) throws FileNotFoundException, IOException {
        RandomAccessFile f = new RandomAccessFile(path, "rw");
        if (seekToEnd) {
            seekToEnd(f);
        }
        return f;
    }

    /**
     * Seek to the end of the given RandomAccessFile
     * @param f
     * @throws IOException
     */
    public static void seekToEnd(RandomAccessFile f) throws IOException {
        f.seek(f.length());
    }
}
