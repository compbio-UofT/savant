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
import javax.swing.filechooser.FileFilter;
import javax.swing.JOptionPane;

import com.jidesoft.dialog.JideOptionPane;

import savant.util.MiscUtils;
import savant.view.swing.Savant;

/**
 * Some utility methods for displaying message dialogs to the user.
 *
 * @author vwilliams
 */
public class DialogUtils {

    public static void displayMessage(String message) {
        JOptionPane.showMessageDialog(Savant.getInstance(), message);
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
                fd.setFilenameFilter(new FileFilterAdapter(filter));
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

    public static File chooseFileForSave(Frame parent, String title) {
        FileDialog fd = new FileDialog(parent, title, FileDialog.SAVE);
        fd.setVisible(true);
        fd.setAlwaysOnTop(true);
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
    static class FileFilterAdapter implements FilenameFilter {
        FileFilter filter;

        FileFilterAdapter(FileFilter f) {
            filter = f;
        }

        @Override
        public boolean accept(File dir, String name) {
            return filter.accept(new File(dir, name));
        }
    }
}
