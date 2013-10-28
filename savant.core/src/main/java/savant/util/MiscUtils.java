/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.util;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import net.sf.samtools.SAMRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Various utility methods and constants of general usefulness.
 *
 * @author mfiume, tarkvara
 */
public class MiscUtils {

    public static final boolean MAC;
    public static final boolean WINDOWS;
    public static final boolean LINUX;
    public static final String UNSAVED_MARK = " *";

    /** OS-specific constant for determining menu-options. Either CTRL_MASK or META_MASK. */
    public static final int MENU_MASK;

    private static final Log LOG = LogFactory.getLog(MiscUtils.class);

    static {
        String os = System.getProperty("os.name").toLowerCase();
        MAC = os.startsWith("mac");
        WINDOWS = os.startsWith("windows");
        LINUX = os.contains("linux");
        MENU_MASK = MAC ? InputEvent.META_MASK : InputEvent.CTRL_MASK;
    }

    /** [[ Miscellaneous Functions ]] */
    /**
     * Format an integer to a string (adding commas)
     * @param num The number to format
     * @return A formatted string
     */
     public static String numToString(double num) {
         return numToString(num,0);
     }

    public static String numToString(double num, int significantdigits) {
        String formatString = "###,###";

        if (significantdigits > 0) {
            formatString += ".";
            for (int i = 0; i < significantdigits; i++) {
                formatString += "#";
            }
        }

        DecimalFormat df = new DecimalFormat(formatString);
        return df.format(num);
    }

    /**
     * Get an integer from a string
     * @param str The string representing an integer (possibly with commas)
     * @return An integer
     */
    public static int stringToInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            LOG.info(MiscUtils.getMessage(e));
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
     * Extract the extension from the given path.
     *
     * @param path The path from which to extract the extension
     * @return The extension of the file at the given path
     */
    public static String getExtension(String path) {
        int dotIndex = path.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == path.length() - 1) {
            return "";
        } else {
            return path.substring(dotIndex + 1);
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
    public static int transformPixelToPosition(int pixel, int widthOfComponent, Range positionalRange) {
        double positionsperpixel = ((double)positionalRange.getLength()) / widthOfComponent;
        return positionalRange.getFrom() + (int)Math.floor(positionsperpixel*pixel);
    }

    /**
     * Translate a (genome) position to a pixel
     * @param position genome position, first base is 1
     * @param widthOfComponent The width of the component in which the pixel occurs
     * @param positionalRange The (genome) range the component applies to
     * @return The pixel represented by the position
     */
    public static int transformPositionToPixel(int position, int widthOfComponent, Range positionalRange) {
         double pixelsperposition = ((double) widthOfComponent) / positionalRange.getLength();
         return (int)Math.round((position - positionalRange.getFrom())*pixelsperposition);
    }

    public static List<String> set2List(Set<String> set) {
        List<String> l = new ArrayList<String>();
        for (String s : set) {
            l.add(s);
        }
        Collections.sort(l);
        return l;
    }

    public static String posToShortString(int genomepos) {
        return posToShortString(genomepos, 0);
    }

    public static String posToShortStringWithSeparation(int pos, int separation) {

        if (separation > 10) {
            int backdigits = (int) Math.floor(Math.log10(separation));
            int significantDigits = 0;
            if (pos > 1000000000) {
                significantDigits = 9 - backdigits;
            } else if (pos > 1000000) {
                significantDigits = 6 - backdigits;
            } else if (pos > 1000) {
                significantDigits = 3 - backdigits;
            }

            return posToShortString(pos, significantDigits);
        } else {
            // For separation of 10 or less, there's no point in using the G/M/k forms.
            return MiscUtils.numToString(pos);
        }
     }

    public static String posToShortString(int pos, int significantDigits) {
        String result;

        if (pos > 1000000000) {
            result = MiscUtils.numToString(pos / 1.0E9, significantDigits) + " G";
        } else if (pos > 1000000) {
            result = MiscUtils.numToString(pos / 1.0E6, significantDigits) + " M";
        } else if (pos > 1000) {
            result = MiscUtils.numToString(pos / 1000.0, significantDigits) + " k";
        } else {
            result = MiscUtils.numToString(pos, significantDigits);
        }

        return result;
    }

    public static int[] getTickPositions(double min, double max) {
        // The 0.35 is a factor which gives us roughly the right density of ticks at all magnifications.
        int log = (int)Math.floor(Math.log10(max - min) - 0.35);
        int step = log > 0 ? (int)Math.pow(10, log) : 1;
        int[] result = new int[(int)(max - min) / step + 1];
        int p0 = ((((int)min - 1) / step) + 1) * step;
        for (int i = 0; i < result.length; i++) {
            result[i] = p0;
            p0 += step;
        }
        return result;
    }

    /**
     * Given a range, return a reasonable set of tick positions for that range.
     */
    public static int[] getTickPositions(Range r) {
        return getTickPositions(r.getFrom(), r.getTo() + 1);
    }


    /**
     * Sometimes Throwable.getMessage() returns a useless string (e.g. "null" for a NullPointerException).
     * Return a string which is more meaningful to the end-user.
     */
    public static String getMessage(Throwable t) {
        if (t instanceof NullPointerException) {
            return "Null pointer exception";
        } else if (t instanceof FileNotFoundException) {
            return String.format("File %s not found", t.getMessage());
        } else if (t instanceof ArrayIndexOutOfBoundsException) {
            return "Array index out of bounds: " + t.getMessage();
        } else if (t instanceof OutOfMemoryError) {
            return "Out of memory: " + t.getMessage();
        } else if (t instanceof NumberFormatException) {
            String msg = t.getMessage();
            int quotePos = msg.indexOf('\"');
            if (quotePos > 0) {
                // Exception message is of form "For input string: \"foo\"".
                return String.format("Unable to interpret %s as a number", msg.substring(quotePos));
            }
            return msg;
        } else {
            if (t.getMessage() != null) {
                return t.getMessage();
            } else {
                return t.toString();
            }
        }
    }

    public static String getStackTrace(Throwable t) {
        if (t == null) { return ""; }
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
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

    /**
     * Register the escape key so that it can be used to cancel the associated JDialog.
     */
    public static void registerCancelButton(final JButton cancelButton) {
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JDialog dialog = (JDialog)SwingUtilities.getWindowAncestor(cancelButton);
        dialog.getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cancelButton.doClick();
             }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Invoke the given runnable on the AWT event thread.
     *
     * @param r the action to be invoked
     */
    public static void invokeLaterIfNecessary(Runnable r) {
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }

    public static String reverseString(String str) {
        int strlen = str.length();
        char[] result = new char[strlen];
        for (int i = 1; i <= strlen; i++) {
            result[strlen-i] = str.charAt(i-1);
        }
        return new String(result);
    }

    /**
     * If rec1 is likely a mate of rec2, return true.
     *
     * @param rec1 first record
     * @param rec2 second record
     * @param extraCheck if true, equality check is insufficient to avoid self-mating; check positions as well
     */
    public static boolean isMate(SAMRecord rec1, SAMRecord rec2, boolean extraCheck) {

        // If rec1 and rec2 came from the same source (e.g. the same call to getRecords),
        // an equality test is sufficient to avoid mating with ourselves.
        if (rec1 == rec2) {
            return false;
        }
        String name1 = rec1.getReadName();
        String name2 = rec2.getReadName();
        int len1 = name1.length();
        int len2 = name2.length();

        if (extraCheck) {
            // Check if names equal and coordinates match as expected.
            if (name1.equals(name2) && rec1.getMateAlignmentStart() == rec2.getAlignmentStart() && rec1.getAlignmentStart() == rec2.getMateAlignmentStart()) {
                return true;
            }
        } else {
            // Check if names equal.
            if (name1.equals(name2)) {
                return true;
            }
        }

        //list of possible suffices...may grow over time.
        String[][] suffices = {{"\\1","\\2"},{"_F","_R"},{"_F3","_R3"}};

        //check suffices
        for(String[] pair : suffices){
            int len = pair[0].length(); //assumes both suffices of same length
            if(name1.substring(0, len1-len).equals(name2.substring(0, len2-len)) &&
                ((name1.substring(len1-len).equals(pair[0]) && name2.substring(len2-len).equals(pair[1])) ||
                (name1.substring(len1-len).equals(pair[1]) && name2.substring(len2-len).equals(pair[0]))))
                return true;
        }

        //not mates
        return false;
    }

    /**
     * Blend two colours, in the given proportions.  Resulting alpha is always 1.0.
     * @param col1 the first colour
     * @param col2 the second colour
     * @param weight1 the weight given to col1 (from 0.0-1.0)
     */
    public static Color blend(Color col1, Color col2, float weight1) {

        float weight2 = (1.0F - weight1) / 255;
        weight1 /= 255;

        // This constructor expects values from 0.0F to 1.0F, so weights have to be scaled appropriately.
        return new Color(col1.getRed() * weight1 + col2.getRed() * weight2, col1.getGreen() * weight1 + col2.getGreen() * weight2, col1.getBlue() * weight1 + col2.getBlue() * weight2);
    }

    /**
     * Utility method to create a polygonal path from a list of coordinates
     * @param coords a sequence of x,y coordinates (should be an even number and at least 4)
     */
    public static Path2D.Double createPolygon(double... coords) {
        if (coords.length < 4 || (coords.length & 1) != 0) throw new IllegalArgumentException("Invalid coordinates for createPolygon");

        Path2D.Double result = new Path2D.Double(Path2D.WIND_NON_ZERO, coords.length / 2);
        result.moveTo(coords[0], coords[1]);
        for (int i = 2; i < coords.length; i += 2) {
            result.lineTo(coords[i], coords[i + 1]);
        }
        result.closePath();
        return result;
    }

    /**
     * Patterned off GraphPane.drawMessageHelper, draws a string centred in the given box.
     */
    public static void drawMessage(Graphics2D g2, String message, Rectangle2D box){
        FontMetrics metrics = g2.getFontMetrics();
        Rectangle2D stringBounds = g2.getFont().getStringBounds(message, g2.getFontRenderContext());
        float x = (float)(box.getX() + (box.getWidth() - stringBounds.getWidth()) / 2.0);
        float y = (float)(box.getY() + (box.getHeight() + metrics.getAscent() - metrics.getDescent()) / 2.0);

        g2.drawString(message, x, y);
    }
}