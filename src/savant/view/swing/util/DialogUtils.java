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

/*
 * DialogUtils.java
 * Created on Aug 5, 2010
 */

package savant.view.swing.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.jidesoft.dialog.JideOptionPane;

import savant.util.MiscUtils;
import savant.view.swing.Savant;

/**
 * Some utility methods for displaying message dialogs to the user.
 *
 * @author vwilliams
 */
public class DialogUtils {

    public static int askYesNo(String title, String prompt) {
        return JOptionPane.showConfirmDialog(Savant.getInstance(), prompt, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    public static void displayError(String title, String message) {
        JOptionPane.showMessageDialog(Savant.getInstance(), message, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void displayMessage(String title, String message) {
        JOptionPane.showMessageDialog(Savant.getInstance(), message, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static String displayInputMessage(String message, String defaultInput) {
        String result = JideOptionPane.showInputDialog(Savant.getInstance(), message, defaultInput);
        if ((result != null) && (result.length() > 0)) {
            return result;
        } else {
            return null;
        }
    }

    public static void displayException(String title, String message, Throwable t) {
        JideOptionPane optionPane = new JideOptionPane(message, JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
        optionPane.setTitle(title);
        JDialog dialog = optionPane.createDialog(Savant.getInstance(),"Error encountered");
        dialog.setResizable(true);
        String details = t.getMessage() + "\r\n" + MiscUtils.getStackTrace(t);
        optionPane.setDetails(details);
        //optionPane.setDetailsVisible(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Open-dialog variant which lets user select a single file.
     *
     * @param parent the parent frame (typically the Savant main frame)
     * @param title title for the dialog
     * @param filter controls which files to display (null for no filtering)
     * @return the selected file, or null if nothing was selected
     */
    public static File chooseFileForOpen(Frame parent, String title, FileFilter filter) {
        if (MiscUtils.MAC) {
            FileDialog fd = new FileDialog(parent, title, FileDialog.LOAD);
            if (filter != null) {
                fd.setFilenameFilter(new FilenameFilterAdapter(filter));
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
            int result = fd.showOpenDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                return fd.getSelectedFile();
            }
        }
        return null;
    }

    /**
     * Open-dialog variant which lets user select multiple files on Windows and
     * Linux.
     * 
     * @param parent the parent frame (typically the Savant main frame)
     * @param title title for the dialog
     * @param filter controls which files to display (null for no filtering)
     * @return an array of selected files; an empty array if nothing is selected
     */
    public static File[] chooseFilesForOpen(Frame parent, String title, FileFilter filter) {
        if (MiscUtils.MAC) {
            // Mac AWT FileDialog doesn't support multiple selection.
            return new File[] { chooseFileForOpen(parent, title, filter) };
        } else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle(title);
            fd.setDialogType(JFileChooser.OPEN_DIALOG);
            if (filter != null) {
                fd.setFileFilter(filter);
            }
            fd.setMultiSelectionEnabled(true);
            int result = fd.showOpenDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                return fd.getSelectedFiles();
            }
        }
        return new File[] {};
    }

    /**
     * Prompt the user to save a file.
     *
     * @param parent window which will serve as the parent for this dialog
     * @param title title of the dialog
     * @param defaultName default file-name to appear in the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(Frame parent, String title, String defaultName) {
        return chooseFileForSave(parent, title, defaultName, null);
    }

    /**
     * Prompt the user to save a file.
     *
     * @param parent window which will serve as the parent for this dialog
     * @param title title of the dialog
     * @param defaultName default file-name to appear in the dialog
     * @param filter file-filter for controlling what appears in the dialog
     * @return a File, or null if cancelled
     */
    public static File chooseFileForSave(Frame parent, String title, String defaultName, FileFilter filter) {
        FileDialog fd = new FileDialog(parent, title, FileDialog.SAVE);
        if (filter != null) {
            fd.setFilenameFilter(new FilenameFilterAdapter(filter));
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
