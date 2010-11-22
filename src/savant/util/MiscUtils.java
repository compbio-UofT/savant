/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;

import savant.view.swing.Savant;


/**
 *
 * @author mfiume
 */
public class MiscUtils {

    public static final boolean MAC;
    public static final boolean WINDOWS;
    public static final boolean LINUX;
    public static final String UNSAVED_MARK = " *";

    static {
        String os = System.getProperty("os.name").toLowerCase();
        MAC = os.startsWith("mac");
        WINDOWS = os.startsWith("windows");
        LINUX = os.contains("linux");
    }

    /** [[ Miscellaneous Functions ]] */
    /**
     * Format an integer to a string (adding commas)
     * @param num The number to format
     * @return A formatted string
     */
    public static String numToString(long num) {
        //TODO: implement formatter
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(num);
    }

    /**
     * Get an integer from a string
     * @param str The string respresenting an integer
     * (possibly with commas)
     * @return An integer
     */
    public static int stringToInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Savant.log(e.getLocalizedMessage());
            return -1;
        }
    }

    /**
     * Get a string representation of the the current time
     * @return A string representing the current time
     */
    public static String now() {
        Calendar cal = Calendar.getInstance();
        return DateFormat.getTimeInstance().format(cal.getTime());
    }


    /**
     * Remove the specified character from the given string.
     * @param s The string from which to remove the character
     * @param c The character to remove from the string
     * @return The string with the character removed
     */
    public static String removeChar(String s, char c) {
        String r = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                r += s.charAt(i);
            }
        }
        return r;
    }

    public static String getFilenameFromPath(String path) {
        int lastSlashIndex = path.lastIndexOf(System.getProperty("file.separator"));
        if (lastSlashIndex == -1) {
            lastSlashIndex = path.lastIndexOf("/");
        }
        return path.substring(lastSlashIndex+1, path.length());
    }

    /**
     * Extract the extension from the given path
     * @param path The path from which to extract the extension
     * @return The extension of the file at the given path
     */
    public static String getExtension(String path) {
        int indexOfDot = path.lastIndexOf(".");

        if (indexOfDot == -1 || indexOfDot == path.length() - 1) {
            return "";
        } else {
            return path.substring(indexOfDot + 1);
        }
    }

    public static String getTemporaryDirectory() {
        String tmpDir;
        if (MAC || LINUX) {
            tmpDir = System.getenv("TMPDIR");
            if (tmpDir != null) {
                return tmpDir;
            }
            else {
                return "/tmp/savant";
            }
        } else {
            if ((tmpDir = System.getenv("TEMP")) != null) {
                return tmpDir;
            }
            else if ((tmpDir = System.getenv("TMP")) != null) {
                return tmpDir;
            }
            else {
                return System.getProperty("user.dir");
            }
        }
    }

    /**
     * Translate a pixel to a (genome) position
     * @param pixel The pixel to transform
     * @param widthOfComponent The width of the component in which the pixel occurs
     * @param positionalRange The (genome) range the component applies to
     * @return The position represented by the pixel
     */
    public static long transformPixelToPosition(int pixel, int widthOfComponent, Range positionalRange) {
        double positionsperpixel = ((double)positionalRange.getLength()) / widthOfComponent;
        return positionalRange.getFrom() + Math.round(positionsperpixel*pixel);
    }

    /**
     * Translate a (genome) position to a pixel
     * @param position genome position, first base is 1
     * @param widthOfComponent The width of the component in which the pixel occurs
     * @param positionalRange The (genome) range the component applies to
     * @return The pixel represented by the position
     */
    public static int transformPositionToPixel(long position, int widthOfComponent, Range positionalRange) {
         double pixelsperposition = ((double) widthOfComponent) / positionalRange.getLength();
         return (int) Math.round((position - positionalRange.getFrom())*pixelsperposition);
    }

    public static List<String> set2List(Set<String> set) {
        List<String> l = new ArrayList<String>();
        for (String s : set) {
            l.add(s);
        }
        Collections.sort(l);
        return l;
    }

    public static String posToShortString(long genomepos) {
        String mousePos;

        long quotient;
        if ((quotient = genomepos / 1000000000) > 1) {
            mousePos = MiscUtils.numToString(quotient) + " G";
        } else if ((quotient = genomepos / 1000000) > 1) {
            mousePos = MiscUtils.numToString(quotient) + " M";
        } else if ((quotient = genomepos / 1000) > 1) {
            mousePos = MiscUtils.numToString(quotient) + " K";
        } else {
            mousePos = MiscUtils.numToString(genomepos) + "";
        }

        return mousePos;
    }

    // get chr from chr1, for e.g.
    public static String removeNumbersFromString(String str) {
        // get the prefix of the current reference (e.g. "chr" in "chr1")
        for (int i = 0; i < 10; i++) {
            str = MiscUtils.removeChar(str, (i + "").charAt(0));
        }
        return str;
    }


    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
   }


    /*
     * Return string without sequence title (chr, contig)
     */
    public static String homogenizeSequence(String s){
        String result = s;
        if(result.contains("chr")){
            result = result.replaceAll("chr", "");
        }
        if(result.contains("Chr")){
            result = result.replaceAll("Chr", "");
        }
        if(result.contains("contig")){
            result = result.replaceAll("contig", "");
        }
        if(result.contains("Contig")){
            result = result.replaceAll("Contig", "");
        }
        return result;
    }

    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
               if(files[i].isDirectory()) {
                 deleteDirectory(files[i]);
               }
               else {
                 files[i].delete();
               }
            }
        }
        return( path.delete() );
    }

     public static void setFrameVisibility(String frameKey, boolean isVisible, DockingManager m) {
        DockableFrame f = m.getFrame(frameKey);
        if (isVisible) {
            m.showFrame(frameKey);
        } else {
            m.hideFrame(frameKey);
        }
    }

     public static double roundToSignificantDigits(double num, int n) {
        if(num == 0) {
            return 0;
        } else if (n == 0) {
            return Math.round(num);
        }

        String s = num + "";
        int index = s.indexOf(".");
        while (n >= s.length() - index) {
            s = s + "0";
        }
        return Double.parseDouble(s.substring(0,index+n+1));
    }

    public static String getSophisticatedByteString(long bytes) {
        if (bytes < 1000) {
            return bytes + " KB";
        } else if (bytes < 1000000000) {
            return roundToSignificantDigits(((double) bytes/1000000),1) + " MB";
        } else {
            return roundToSignificantDigits(((double) bytes/1000000000),2) + " GB";
        }
    }

    /**
     * If u is a file:// URI, return the absolute path.  If it's a network URI, leave
     * it unchanged.
     *
     * @param u the URI to be neatened
     * @return a canonical string representing the URI.
     */
    public static String getNeatPathFromURI(URI u) {
        if ("file".equals(u.getScheme())) {
            return (new File(u)).getAbsolutePath();
        }
        return u.toString();
     }

    /**
     * Set the title of a window to reflect whether it is saved or not.  On Windows
     * and Linux, this appends an asterisk to the title of an unsaved document; on
     * Mac, it puts a dot inside the close-button.
     * @param f
     * @param unsaved
     */
    public static void setUnsavedTitle(JFrame f, String title, boolean unsaved) {
        f.getRootPane().putClientProperty("Window.documentModified", unsaved);
        if (!MAC && unsaved) {
            f.setTitle(title + UNSAVED_MARK);
        } else {
            f.setTitle(title);
        }
    }
}
