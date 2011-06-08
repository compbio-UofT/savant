/*
 *    Copyright 2010-2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package savant.api.util;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import savant.view.swing.Savant;

/**
 * Utility methods to allow plugins to make Savant display a dialog.  Among other
 * things, this is supposed to insulate code from knowing about whether we're operating
 * as a Swing application or not.
 *
 * @author tarkvara
 */
public class DialogUtils {

    /**
     * Same as JOptionPane.YES_OPTION.
     */
    public static final int YES = 0;

    /**
     * Same as JOptionPane.OK_OPTION.
     */
    public static final int OK = 0;

    /**
     * Same as JOptionPane.NO_OPTION.
     */
    public static final int NO = 1;

    /**
     * Same as JOptionPane.CANCEL_OPTION.
     */
    public static final int CANCEL = 2;

    /**
     * Display a Savant dialog to ask a yes/no question.
     */
    public static int askYesNo(String title, String prompt) {
        return savant.view.swing.util.DialogUtils.askYesNo(title, prompt);
    }

    /**
     * Display a Savant dialog to ask a yes/no question with the title "Savant".
     */
    public static int askYesNo(String prompt) {
        return askYesNo("Savant", prompt);
    }

    /**
     * Display a Savant dialog to ask a yes/no/cancel question with the title "Savant".
     */
    public static int askYesNoCancel(String prompt) {
        return savant.view.swing.util.DialogUtils.askYesNoCancel("Savant", prompt);
    }

    /**
     * Display a Savant error dialog with the given message and the title "Savant Error".
     *
     * @param message the message to be displayed
     */
    public static void displayError(String message) {
        savant.view.swing.util.DialogUtils.displayError("Savant Error", message);
    }

    /*
     * Display a dialog that gets input
     */
    public static String displayInputMessage(String message, String defaultInput) {
        return savant.view.swing.util.DialogUtils.displayInputMessage(message, defaultInput);
    }

    /**
     * Display a Savant error dialog with the given title and message.
     *
     * @param title title for the dialog
     * @param message the message to be displayed
     */
    public static void displayError(String title, String message) {
        savant.view.swing.util.DialogUtils.displayError(title, message);
    }

    /**
     * Display a Savant message dialog with the given message and the title "Savant".
     *
     * @param message the message to be displayed
     */
    public static void displayMessage(String message) {
        savant.view.swing.util.DialogUtils.displayMessage("Savant", message);
    }

    /**
     * Display a Savant message dialog with the given title and message.
     *
     * @param title title for the dialog
     * @param message the message to be displayed
     */
    public static void displayMessage(String title, String message) {
        savant.view.swing.util.DialogUtils.displayMessage(title, message);
    }


    /**
     * Display a Savant dialog which reports an exception with associated stack trace.
     *
     * @param title title of the dialog window
     * @param message the message to be displayed
     * @param x exception whose stack trace will be displayed in the Details section
     */
    public static void displayException(String title, String message, Throwable x) {
        savant.view.swing.util.DialogUtils.displayException(title, message, x);
    }

    /**
     * Prompt the user to open a file.
     *
     * @param title title of the dialog
     * @param filter filter for determining which files to display
     * @param initialDir initial directory for the dialog (null to use system default)
     * @return a File, or null if cancelled
     */
    public static File chooseFileForOpen(String title, FileFilter filter, File initialDir) {
        return savant.view.swing.util.DialogUtils.chooseFileForOpen(Savant.getInstance(), title, filter, initialDir);
    }

    /**
     * Open-file dialog variant which lets user select multiple files on Windows and
     * Linux.
     *
     * @param title title for the dialog
     * @param filter controls which files to display (null for no filtering)
     * @param initialDir initial directory for the dialog (null to use system default)
     * @return an array of selected files; an empty array if nothing is selected
     */
    public static File[] chooseFilesForOpen(String title, FileFilter filter, File initialDir) {
        return savant.view.swing.util.DialogUtils.chooseFilesForOpen(Savant.getInstance(), title, filter, initialDir);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param defaultName default file-name to appear in the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(String title, String defaultName) {
        return savant.view.swing.util.DialogUtils.chooseFileForSave(Savant.getInstance(), title, defaultName, null, null);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param filter file-filter for controlling what appears in the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(String title, String defaultName, FileFilter filter) {
        return savant.view.swing.util.DialogUtils.chooseFileForSave(Savant.getInstance(), title, defaultName, filter, null);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param filter file-filter for controlling what appears in the dialog
     * @param dir the default directory
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(String title, String defaultName, FileFilter filter, File dir) {
        return savant.view.swing.util.DialogUtils.chooseFileForSave(Savant.getInstance(), title, defaultName, filter, dir);
    }

    /**
     * For purposes of centring dialogs, here's the Savant main window.
     */
    public static Window getMainWindow() {
        return Savant.getInstance();
    }

    /**
     * Show a Savant progress dialog with the appropriate amount of progress.
     *
     * @param parent component to serve as the dialog's parent
     * @param message message describing the step currently in progress
     * @param fraction fraction of the process which is complete (-1.0 for an indeterminate process; 1.0 to dismiss the dialog)
     */
    public static void showProgress(Window parent, String message, double fraction) {
        savant.view.swing.util.ProgressDialog.showProgress(parent, message, fraction);
    }
}
