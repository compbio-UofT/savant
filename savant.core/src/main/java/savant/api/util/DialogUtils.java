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

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.jidesoft.dialog.JideOptionPane;

import savant.util.MiscUtils;
import savant.util.error.report.BugReportDialog;
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
        return JOptionPane.showConfirmDialog(getMainWindow(), prompt, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
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
        return JOptionPane.showConfirmDialog(getMainWindow(), prompt, "Savant", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Display a Savant error dialog with the given title and message.
     *
     * @param title title for the dialog
     * @param message the message to be displayed
     */
    public static void displayError(String title, String message) {
        JOptionPane.showMessageDialog(getMainWindow(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Display a Savant error dialog with the given message and the title "Savant Error".
     *
     * @param message the message to be displayed
     */
    public static void displayError(String message) {
        displayError("Savant Error", message);
    }

    /**
     * Display a dialog that gets an input string from the user.
     *
     * @param title title of the dialog window
     * @param message prompt message that appears in the dialog
     * @param defaultInput default string which appears in the text-field
     */
    public static String displayInputMessage(String title, String message, String defaultInput) {
        String result = JOptionPane.showInputDialog(getMainWindow(), message, "Savant", JOptionPane.QUESTION_MESSAGE);
        if (result != null && result.length() > 0) {
            return result;
        }
        return null;
    }

    /**
     * Display a Savant message dialog with the given title and message.
     *
     * @param title title for the dialog
     * @param message the message to be displayed
     */
    public static void displayMessage(String title, String message) {
        JOptionPane.showMessageDialog(getMainWindow(), message, title, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Display a Savant message dialog with the given message and the title "Savant".
     *
     * @param message the message to be displayed
     */
    public static void displayMessage(String message) {
        displayMessage("Savant", message);
    }


    /**
     * Display a Savant dialog which reports an exception with associated stack trace.
     *
     * @param title title of the dialog window
     * @param message the message to be displayed
     * @param t exception whose stack trace will be displayed in the Details section
     */
    public static void displayException(final String title, final String message, final Throwable t) {
        MiscUtils.invokeLaterIfNecessary(new Runnable() {
            @Override
            public void run() {

                String msg = message;
                if (t.getCause() != null) {
                    msg += "\r\nCause: " + MiscUtils.getMessage(t.getCause()) + ".";
                }
                JideOptionPane optionPane = new JideOptionPane(msg, JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
                optionPane.setTitle(title);
                optionPane.setOptions(new String[] {});
                JButton reportButton = new JButton("Report Issue");
                ((JComponent)optionPane.getComponent(optionPane.getComponentCount()-1)).add(reportButton);
                final JDialog dialog = optionPane.createDialog(getMainWindow(), "Error encountered");
                dialog.setResizable(true);
                String details = t.getMessage() + "\r\n" + MiscUtils.getStackTrace(t);
                optionPane.setDetails(details);
                dialog.pack();

                reportButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e2) {
                    String issue = "Hey Savant Developers,\n\n";
                    issue += "I am encountering an error in Savant. I have provided additional diagnostic information below.\n\n";

                    issue += "=== DESCRIBE THE ISSUE BELOW ===\n\n\n";


                    issue += "=== ERROR DETAILS ===\n";
                    issue += MiscUtils.getStackTrace(t);

                    dialog.dispose();
                    (new BugReportDialog(Savant.getInstance(),issue)).setVisible(true);
                }

                });

                dialog.setVisible(true);
            }
        });
    }

    /**
     * Prompt the user to open a single file.
     *
     * @param title title of the dialog
     * @param filter filter for determining which files to display
     * @param initialDir initial directory for the dialog (null to use system default)
     * @return a <code>File</code>, or null if cancelled
     */
    public static File chooseFileForOpen(String title, FileFilter filter, File initialDir) {
        if (MiscUtils.MAC) {
            FileDialog fd = getFileDialog(title, FileDialog.LOAD);
            if (filter != null) {
                fd.setFilenameFilter(new FilenameFilterAdapter(filter));
            }
            if (initialDir != null) {
                fd.setDirectory(initialDir.getAbsolutePath());
            }
            fd.setVisible(true);
            fd.setAlwaysOnTop(true);
            String selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                return new File(fd.getDirectory(), selectedFileName);
            }
        } else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle(title);
            fd.setDialogType(JFileChooser.OPEN_DIALOG);
            if (filter != null) {
                fd.setFileFilter(filter);
            }
            if (initialDir != null) {
                fd.setCurrentDirectory(initialDir);
            }
            int result = fd.showOpenDialog(getMainWindow());
            if (result == JFileChooser.APPROVE_OPTION) {
                return fd.getSelectedFile();
            }
        }
        return null;
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
        if (MiscUtils.MAC) {
            // Mac AWT FileDialog doesn't support multiple selection.
            File f = chooseFileForOpen(title, filter, initialDir);
            if (f != null) {
                return new File[] { f };
            }
        } else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle(title);
            fd.setSelectedFile(initialDir);
            fd.setDialogType(JFileChooser.OPEN_DIALOG);
            if (filter != null) {
                fd.setFileFilter(filter);
            }
            fd.setMultiSelectionEnabled(true);
            int result = fd.showOpenDialog(getMainWindow());
            if (result == JFileChooser.APPROVE_OPTION) {
                return fd.getSelectedFiles();
            }
        }
        return new File[] {};
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param defaultName default file-name to appear in the dialog
     * @return a <code>File</code>, or null if cancelled
     */
    public static File chooseFileForSave(String title, String defaultName) {
        return chooseFileForSave(title, defaultName, null, null);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param title title of the dialog
     * @param defaultName default file-name to appear in the dialog
     * @param filter file-filter for controlling what appears in the dialog
     * @param initialDir the default directory
     * @return a <code>File</code>, or null if cancelled
     */
    public static File chooseFileForSave(String title, String defaultName, FileFilter filter, File initialDir) {
        FileDialog fd = getFileDialog(title, FileDialog.SAVE);
        if (filter != null) {
            fd.setFilenameFilter(new FilenameFilterAdapter(filter));
        }
        if (initialDir != null) {
            fd.setDirectory(initialDir.getAbsolutePath());
        }
        fd.setFile(defaultName);
        fd.setAlwaysOnTop(true);
        fd.setVisible(true);
        String selectedFile = fd.getFile();
        if (selectedFile != null) {
            return new File(fd.getDirectory(), selectedFile);
        }
        return null;
    }

    /**
     * Choose an appropriate parent for the dialog being requested.  Usually the Savant main
     * window, but may sometimes be an open dialog.
     */
    public static Window getMainWindow() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    }

    /**
     * In a remarkable piece of bad design, Java provides separate FileDialog constructors
     * depending on whether the parent is a Frame or a Dialog.
     */
    private static FileDialog getFileDialog(String title, int type) {
        Window w = getMainWindow();
        if (w instanceof Frame) {
            return new FileDialog((Frame)w, title, type);
        } else {
            return new FileDialog((Dialog)w, title, type);
        }
    }

    /**
     * Show a Savant progress dialog with the appropriate amount of progress.
     *
     * @param message message describing the step currently in progress
     * @param fraction fraction of the process which is complete (-1.0 for an indeterminate process; 1.0 to dismiss the dialog)
     */
    public static void showProgress(String message, double fraction) {
        savant.util.swing.ProgressDialog.showProgress(message, fraction);
    }


    /**
     * Little class so that caller can pass us a FileFilter and will still be able
     * to use it with a Mac FileDialog.
     */
    static class FilenameFilterAdapter implements FilenameFilter {
        FileFilter filter;

        FilenameFilterAdapter(FileFilter f) {
            filter = f;
        }

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir, name));
        }
    }
}
