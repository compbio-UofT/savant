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
package savant.view.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.event.BookmarksChangedEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.BookmarkController;
import savant.controller.LocationController;
import savant.util.Bookmark;
import savant.util.Range;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.model.BookmarksTableModel;

/**
 *
 * @author mfiume
 */
public class BookmarkSheet extends JPanel implements Listener<BookmarksChangedEvent> {

    private static final Log LOG = LogFactory.getLog(BookmarkSheet.class);

    private JTable table;

    static boolean isRecording = false;
    static JButton recordButton;
    static JButton addButton;
    static boolean confirmDelete = true;

    public BookmarkSheet(Container c) {

        // set the layout of the data sheet
        c.setLayout(new BorderLayout());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        /**
         * Create a toolbar. 
         */
        JMenuBar toolbar = new JMenuBar();
        toolbar.setLayout(new BoxLayout(toolbar,BoxLayout.X_AXIS));
        c.add(toolbar, BorderLayout.NORTH);

        JButton previousButton = new JButton();
        previousButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.UP));
        previousButton.setToolTipText("Go to previous bookmark [ Ctrl+( ]");
        previousButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        previousButton.putClientProperty( "JButton.segmentPosition", "first" );
        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToPreviousBookmark();
            }

        });
        toolbar.add(previousButton);

        JButton nextButton = new JButton();
        nextButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.DOWN));
        nextButton.setToolTipText("Go to next bookmark [ Ctrl+) ]");
        nextButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        nextButton.putClientProperty( "JButton.segmentPosition", "last" );
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToNextBookmark();
            }

        });
        toolbar.add(nextButton);

        JButton goButton = new JButton("Go");
        goButton.setToolTipText("Go to selected bookmark");
        goButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        goButton.putClientProperty( "JButton.segmentPosition", "only" );
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goToSelectedBookmark();
            }

        });
        toolbar.add(goButton);

        toolbar.add(Box.createGlue());

        addButton = new JButton();
        addButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.BKMK_ADD));
        addButton.setToolTipText("Add bookmark for current range");
        addButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        addButton.putClientProperty( "JButton.segmentPosition", "first" );
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                fc.addCurrentRangeToBookmarks();
            }
        });
        toolbar.add(addButton);

        JButton deleteButton = new JButton();
        deleteButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.BKMK_RM));
        deleteButton.setToolTipText("Delete selected bookmarks");
        deleteButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        deleteButton.putClientProperty( "JButton.segmentPosition", "last" );
        deleteButton.addActionListener(new ActionListener() {
            @Override
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
                    int confirmDeleteDialog = JOptionPane.showOptionDialog(DialogUtils.getMainWindow(),
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
        loadButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        loadButton.putClientProperty( "JButton.segmentPosition", "first" );
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadBookmarks(table);
            }
        });
        toolbar.add(loadButton);

        JButton saveButton = new JButton();
        saveButton.setIcon(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.SAVE));
        saveButton.setToolTipText("Save bookmarks to file");
        saveButton.putClientProperty( "JButton.buttonType", "segmentedRoundRect" );
        saveButton.putClientProperty( "JButton.segmentPosition", "last" );
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveBookmarks(table);
            }
        });
        toolbar.add(saveButton);

        // create a table (the most important component)
        table = new JTable(new BookmarksTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(Color.gray);
        //table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // add the table and its header to the subpanel
        c.add(table.getTableHeader());

        add(table);

        final JScrollPane sp = new JScrollPane(table,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setWheelScrollingEnabled(false);
        sp.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getValue() + e.getUnitsToScroll()*2);
            }
        });

        c.add(sp);

        // add glue to fill the remaining space
        add(Box.createGlue());
    }

    @Override
    public void handleEvent(BookmarksChangedEvent event) {
        refreshData(BookmarkController.getInstance().getBookmarks());
    }

    private void refreshData(List<Bookmark> favorites) {
        ((BookmarksTableModel) table.getModel()).setData(favorites);
        ((BookmarksTableModel) table.getModel()).fireTableDataChanged();
    }

    private void loadBookmarks(JTable table) {
        final BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
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

        final File selectedFile = DialogUtils.chooseFileForOpen("Load Bookmarks", null, null);

        // set the genome
        if (selectedFile != null) {
            
            int result = JOptionPane.showOptionDialog(null, "Would you like to add padding to each bookmark range?", "Add a margin?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            final boolean addMargin = (result == JOptionPane.YES_OPTION);

            Window w = SwingUtilities.getWindowAncestor(BookmarkSheet.this);
            JOptionPane optionPane = new JOptionPane("<html>Loading bookmarks from file.<br>This may take a moment.</html>", JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION);
            final JDialog dialog = new JDialog(w, "Loading Bookmarks", Dialog.ModalityType.MODELESS);
            dialog.setContentPane(optionPane);
            dialog.pack();
            dialog.setLocationRelativeTo(w);
            dialog.setVisible(true);
            new Thread("BookmarkSheet.loadBookmarks") {
                @Override
                public void run() {
                    try {
                        BookmarkController.getInstance().addBookmarksFromFile(selectedFile, addMargin);
                        btm.fireTableDataChanged();
                    } catch (Exception ex) {
                        DialogUtils.displayError("Error", "Unable to load bookmarks: " + ex.getMessage());
                    } finally {
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                }
            }.start();
        }
    }

    private static void saveBookmarks(JTable table) {
        BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
        List<Bookmark> bookmarks = btm.getData();

        // get the path (null if none selected)
        File selectedFile = DialogUtils.chooseFileForSave("Save Bookmarks", "Bookmarks.txt", null);

        // set the genome
        if (selectedFile != null) {
            try {
                saveBookmarks(selectedFile.getAbsolutePath(), bookmarks);
            } catch (IOException ex) {
                DialogUtils.displayError("Error", "Unable to save bookmarks: " + ex.getMessage());
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

    public void goToSelectedBookmark() {
        int viewRow = table.getSelectedRow();
        if (viewRow > -1) {
            goToBookmark(viewRow);
        }
    }

    public void goToNextBookmark() {
        if(table.getRowCount() == 0) return;
        int viewRow = table.getSelectedRow();
        if (viewRow == -1 || viewRow == table.getRowCount()-1) { viewRow = 0; }
        else { viewRow += 1; }
        selectRow(viewRow);
    }

    private void selectRow(int viewRow) {
        int modelRow = table.convertRowIndexToModel(viewRow);
        table.removeRowSelectionInterval(0, table.getRowCount()-1);
        table.addRowSelectionInterval(viewRow, viewRow);
        goToSelectedBookmark();
    }

    public void goToPreviousBookmark() {
        if(table.getRowCount() == 0) return;
        int viewRow = table.getSelectedRow();
        if (viewRow == -1 || viewRow == 0) { viewRow = table.getRowCount()-1; }
        else { viewRow -= 1; }
        selectRow(viewRow);
    }

    public void goToBookmark(int viewRow) {
        if (viewRow == -1 || table.getRowCount() == 0) { return; }
        LocationController lc = LocationController.getInstance();
        BookmarksTableModel tableModel = (BookmarksTableModel) table.getModel();
        Bookmark bookmark = tableModel.getData().get(table.convertRowIndexToModel(viewRow));
        lc.setLocation(bookmark.getReference(), (Range)bookmark.getRange());
    }
}
