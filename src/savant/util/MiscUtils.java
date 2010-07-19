/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import java.util.Set;
import savant.view.swing.Savant;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class MiscUtils {

    /** [[ Miscellaneous Functions ]] */
    /**
     * Format an integer to a string (adding commas)
     * @param num The number to format
     * @return A formatted string
     */
    public static String intToString(int num) {
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
        return cal.getTime().toGMTString();
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

    public static String getTemporaryDirectory() {
        String os = System.getProperty("os.name");

        String tmpDir;
        if (os.toLowerCase().contains("mac") || os.toLowerCase().contains("linux")) {
            tmpDir = System.getenv("TMPDIR");
            if (tmpDir != null) {
                return tmpDir;
            }
            else {
                return "/tmp/savant";
            }
        }
        else {
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
     * @param positionalRange The (genome) range the componentn applies to
     * @return The position represented by the pixel
     */
    public static int transformPixelToPosition(int pixel, int widthOfComponent, Range positionalRange) {
        double positionsperpixel = ((double)positionalRange.getLength()) / widthOfComponent;
        return positionalRange.getFrom() + (int) Math.round(positionsperpixel*pixel);
    }

    /**
     * Translate a (genome) position to a pixel
     * @param pixel The pixel to transform
     * @param widthOfComponent The width of the component in which the pixel occurs
     * @param positionalRange The (genome) range the componentn applies to
     * @return The pixel represented by the position
     */
    public static int transformPositionToPixel(int position, int widthOfComponent, Range positionalRange) {
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

    public static String intToShortString(int genomepos) {
        String mousePos;

        int quotient;
        if ((quotient = genomepos / 1000000) > 1) {
            mousePos = MiscUtils.intToString(quotient) + " M";
        } else if ((quotient = genomepos / 100) > 1) {
            mousePos = MiscUtils.intToString(quotient) + " K";
        } else {
            mousePos = MiscUtils.intToString(genomepos) + "";
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
}
