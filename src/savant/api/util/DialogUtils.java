/*
 *    Copyright 2010 University of Toronto
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

import java.io.File;
import javax.swing.filechooser.FileFilter;

import savant.view.swing.Savant;

/**
 * Utility methods to allow plugins to make Savant display a dialog.
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
     * Display a Savant error dialog with the given message and the title "Savant Error".
     *
     * @param message the message to be displayed
     */
    public static void displayError(String message) {
        savant.view.swing.util.DialogUtils.displayError("Savant Error", message);
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
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(String title) {
        return savant.view.swing.util.DialogUtils.chooseFileForSave(Savant.getInstance(), title, null);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param filter file-filter for controlling what appears in the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(String title, FileFilter filter) {
        return savant.view.swing.util.DialogUtils.chooseFileForSave(Savant.getInstance(), title, filter);
    }
}
