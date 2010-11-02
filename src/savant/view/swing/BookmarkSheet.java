/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.view.swing;

import savant.view.icon.SavantIconFactory;
import savant.controller.BookmarkController;
import savant.controller.RangeController;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
//import savant.controller.event.range.RangeChangedEvent;
//import savant.controller.event.range.RangeChangedListener;
import savant.util.Bookmark;
import savant.util.Range;
import savant.view.swing.model.BookmarksTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;

import savant.controller.event.range.RangeChangedListener;
import static java.awt.FileDialog.SAVE;

/**
 *
 * @author mfiume
 */
public class BookmarkSheet implements BookmarksChangedListener /*, RangeChangedListener*/ {

    private JTable table;

    static boolean isRecording = false;
    static JButton recordButton;
    static JButton addButton;
    static boolean confirmDelete = true;

    public BookmarkSheet(Savant parent, Container c) {

        JPanel subpanel = new JPanel();

        // set the layout of the data sheet
        c.setLayout(new BorderLayout());
        subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.Y_AXIS));

        /**
         * Create a toolbar. 
         */
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setLayout(new BoxLayout(toolbar,BoxLayout.X_AXIS));
        c.add(toolbar, BorderLayout.NORTH);

        JButton previousButton = new JButton();
        previousButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.UP));
        previousButton.setToolTipText("Go to previous bookmark");
        previousButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goToPreviousBookmark();
            }

        });
        toolbar.add(previousButton);

        JButton nextButton = new JButton();
        nextButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.DOWN));
        nextButton.setToolTipText("Go to next bookmark");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goToNextBookmark();
            }

        });
        toolbar.add(nextButton);

        JButton goButton = new JButton("Go");
        goButton.setToolTipText("Go to selected bookmark");
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goToSelectedBookmark();
            }

        });
        toolbar.add(goButton);

        toolbar.add(Box.createGlue());

        addButton = new JButton();
        addButton.setBorder(null);
        addButton.setBorderPainted(false);
        addButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.BKMK_ADD));
        addButton.setToolTipText("Add bookmark for current range");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                fc.addCurrentRangeToBookmarks();
            }
        });
        toolbar.add(addButton);

        JButton deleteButton = new JButton();
        deleteButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.BKMK_RM));
        deleteButton.setToolTipText("Delete selected bookmarks");
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                int[] selectedRows = table.getSelectedRows();
                Arrays.sort(selectedRows);
                boolean delete = false;

                if(selectedRows.length > 0 && confirmDelete){
                    Object[] options = {"Yes",
                                        "No",
                                        "Yes, don't ask again"};
                    JLabel message = new JLabel("Are you sure you want to delete " + selectedRows.length + " item(s)?");
                    message.setPreferredSize(new Dimension(300,20));
                    int confirmDeleteDialog = JOptionPane.showOptionDialog(Savant.getInstance(),
                        message,
                        "Confirm Delete",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);
                    
                    if(confirmDeleteDialog==0){
                        delete = true;
                    } else if (confirmDeleteDialog==2){
                        delete = true;
                        confirmDelete = false;
                    }
                } else if(selectedRows.length > 0 && !confirmDelete){
                    delete = true;
                }

                if(delete){
                    for(int i = selectedRows.length -1; i >= 0; i--){
                        fc.removeBookmark(selectedRows[i]);
                    }
                }
            }
        });
        toolbar.add(deleteButton);

        toolbar.add(Box.createGlue());

        JButton loadButton = new JButton();
        loadButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.OPEN));
        loadButton.setToolTipText("Load bookmarks from file");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadBookmarks(table);
            }
        });
        toolbar.add(loadButton);

        JButton saveButton = new JButton();
        saveButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SAVE));
        saveButton.setToolTipText("Save bookmarks to file");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookmarks(table);
            }
        });
        toolbar.add(saveButton);

        // create a table (the most important component)
        table = new JTable(new BookmarksTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        //table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // add the table and its header to the subpanel
        c.add(table.getTableHeader());

        subpanel.add(table);

        JScrollPane sp = new JScrollPane(table,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        c.add(sp);

        // add glue to fill the remaining space
        subpanel.add(Box.createGlue());

        //RangeController rc = RangeController.getInstance();
        //rc.addRangeChangedListener(this);

        // initContextualMenu();
    }

    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        this.refreshData(event.favorites());
    }

    private void refreshData(List<Bookmark> favorites) {
        Savant.log("Setting data");
        ((BookmarksTableModel) table.getModel()).setData(favorites);
        Savant.log("Done data");
        ((BookmarksTableModel) table.getModel()).fireTableDataChanged();
    }

    /*
    public void rangeChangeReceived(RangeChangedEvent event) {
        if (isRecording) {
            BookmarkController bc = BookmarkController.getInstance();
            bc.addCurrentRangeToBookmarks();
        }
    }
     */

    private static void loadBookmarks(JTable table) {
        BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
        List<Bookmark> bookmarks = btm.getData();

        if (bookmarks.size() > 0) {
            String message = "Clear existing bookmarks?";
            String title = "Clear Bookmarks";
                // display the JOptionPane showConfirmDialog
            int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION){
                btm.clearData();
                BookmarkController.getInstance().clearBookmarks();
            }
        }

        JFrame jf = new JFrame();
        String selectedFileName;
        if (Savant.mac) {
            FileDialog fd = new FileDialog(jf, "Load Bookmarks", FileDialog.LOAD);
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);
            // get the path (null if none selected)
            selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
            }
        }
        else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle("Load Bookmards");
            fd.setDialogType(JFileChooser.OPEN_DIALOG);
            int result = fd.showOpenDialog(jf);
            if (result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION ) return;
            selectedFileName = fd.getSelectedFile().getPath();
        }

        Savant.log("load 0");

        // set the genome
        if (selectedFileName != null) {
            try {
                loadBookmarks(selectedFileName, bookmarks);
                btm.fireTableDataChanged();
            } catch (IOException ex) {
                String message = "Load unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
        //System.out.println(BookmarkController.getInstance().getBookmarks());
    }

    private static void saveBookmarks(JTable table) {
        BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
        List<Bookmark> bookmarks = btm.getData();

        JFrame jf = new JFrame();
        FileDialog fd = new FileDialog(jf, "Save Bookmarks", SAVE);
        fd.setVisible(true);
        jf.setAlwaysOnTop(true);

        // get the path (null if none selected)
        String selectedFileName = fd.getFile();

        // set the genome
        if (selectedFileName != null) {
            try {
                selectedFileName = fd.getDirectory() + selectedFileName;
                saveBookmarks(selectedFileName, bookmarks);
            } catch (IOException ex) {
                String message = "Save unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveBookmarks(String filename, List<Bookmark> bookmarks) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

        for (Bookmark bm : bookmarks) {
            bw.write(bm.getReference() + "\t" + bm.getRange().getFrom() + "\t" + bm.getRange().getTo() + "\t" + bm.getAnnotation() + "\n");
        }

        bw.close();
    }

    private static void loadBookmarks(String filename, List<Bookmark> bookmarks) throws FileNotFoundException, IOException {

        Savant.log("load 1");

        BufferedReader br = new BufferedReader(new FileReader(filename));

        BookmarkController bmc = BookmarkController.getInstance();

        String line = "";

        while ((line = br.readLine()) != null) {
            Bookmark bm = parseBookmark(line);
            bookmarks.add(bm);
            bmc.addBookmarkSilent(bm);
        }

        br.close();
    }

    private static Bookmark parseBookmark(String line) {

        Savant.log("load 2");

        StringTokenizer st = new StringTokenizer(line,"\t");

        String ref = st.nextToken();
        long from = Long.parseLong(st.nextToken());
        long to = Long.parseLong(st.nextToken());
        String annotation = "";

        while (st.hasMoreElements()) {
            annotation += st.nextToken() + " ";
        }
        annotation.trim();

        return new Bookmark(ref, new Range(from,to), annotation);
    }

    public void goToSelectedBookmark() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow > -1) {
            goToBookmark(selectedRow);
        }
    }

    public void goToNextBookmark() {
        int row = table.getSelectedRow();
        if (row == -1 || row == table.getRowCount()-1) { row = 0; }
        else { row += 1; }
        selectRow(row);
    }

    private void selectRow(int row) {
        table.removeRowSelectionInterval(0, table.getRowCount()-1);
        table.addRowSelectionInterval(row, row);
        goToSelectedBookmark();
    }

    public void goToPreviousBookmark() {
        int row = table.getSelectedRow();
        if (row == -1 || row == 0) { row = table.getRowCount()-1; }
        else { row -= 1; }
        selectRow(row);
    }

    public void goToBookmark(int i) {
        if (i == -1 && table.getRowCount() == 0) { return; }
        else { i = 0; }
        RangeController rc = RangeController.getInstance();
        BookmarksTableModel tableModel = (BookmarksTableModel) table.getModel();
        Bookmark bookmark = tableModel.getData().get(i);
        rc.setRange(bookmark.getReference(),bookmark.getRange());
    }
}
