/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.debug;

/**
 *
 * @author mfiume
 * @deprecated
 */
public class SavantDebugger {

    public static boolean isDebuggingOn = true;

    public static void debug(String msg) {
        if (!isDebuggingOn) { return; }
        System.out.print(msg);
    }

    public static void debugln(String msg) {
        if (!isDebuggingOn) { return; }
        System.out.println(msg);
    }
}
