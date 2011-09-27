/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 *
 * @author mfiume
 */
public class CommandUtils {

    public static void runCommand(String command) throws IOException {
        runCommand(command, null, false);
    }

    public static void runCommand(String command, PrintStream os) throws IOException {
        runCommand(command, os, true);
    }

    private static void runCommand(String command, PrintStream os, boolean doReport) throws IOException {

        reportln(os, doReport, "Running command:\t" + command);

        Process p = Runtime.getRuntime().exec(command);
        try {
            int returnVal = p.waitFor();
            reportln(os, doReport, "Process completed with status: " + returnVal);
        } catch (InterruptedException ex) {
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String s;

        if (doReport) {
            // read the output from the command
            os.println("STANDARD OUTPUT:");
            while ((s = stdInput.readLine()) != null) {
                os.println(s);
            }

            // read any errors from the attempted command
            os.println("STANDARD ERROR:");
            while ((s = stdError.readLine()) != null) {
                os.println(s);
            }
        }

    }

    private static void reportln(PrintStream os, boolean doReport, String string) {
        if (doReport) {
            os.println(string);
        }
    }

    private static void report(PrintStream os, boolean doReport, String string) {
        if (doReport) {
            os.print(string);
        }
    }
}
