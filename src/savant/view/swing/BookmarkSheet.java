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

import savant.controller.BookmarkController;
import savant.controller.RangeController;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.util.Bookmark;
import savant.view.swing.Savant;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;

import savant.util.Range;
import savant.view.swing.model.BookmarksTableModel;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 *
 * @author mfiume
 */
public class BookmarkSheet implements BookmarksChangedListener, RangeChangedListener {

    private JTable table;
    private Savant parent;

    static boolean isRecording = false;
    static JButton recordButton;
    static JButton addButton;

    // Popup menu
    JPopupMenu tableMenu;

    public BookmarkSheet(Savant parent, JPanel panel) {

        JPanel subpanel = new JPanel();

         // set the parent
        this.parent = parent;

        // set the layout of the data sheet
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.Y_AXIS));

        /**
         * Create a toolbar. 
         */
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setMinimumSize(new Dimension(22,22));
        toolbar.setPreferredSize(new Dimension(22,22));
        toolbar.setMaximumSize(new Dimension(999999,22));
        panel.add(toolbar);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadBookmarks(table);
            }
        });
        toolbar.add(loadButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBookmarks(table);
            }
        });
        toolbar.add(saveButton);

        toolbar.add(Box.createGlue());

        addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                fc.addCurrentRangeToBookmarks();
            }
        });
        toolbar.add(addButton);

        recordButton = new JButton("Record");
        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkSheet.isRecording = !BookmarkSheet.isRecording;
                if (BookmarkSheet.isRecording) {
                    recordButton.setText("Stop Recording");
                    addButton.setEnabled(false);
                } else {
                    recordButton.setText("Record");
                    addButton.setEnabled(true);
                }
            }
        });
        toolbar.add(recordButton);

        // create a table (the most important component)
        table = new JTable(new BookmarksTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        /*
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                int index = e.getFirstIndex();
                RangeController rc = RangeController.getInstance();
                rc.setRange(fc.getFavorite(index).getRange());
            }

        });
         * 
         */

        table.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                    showTableContextualMenu(e);
                }
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public void mousePressed(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public void mouseReleased(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public void mouseEntered(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public void mouseExited(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            private void showTableContextualMenu(MouseEvent event) {
                tableMenu.show(event.getComponent(), event.getX(), event.getY());
            }

        });

        // add the table and its header to the subpanel
        panel.add(table.getTableHeader());

        subpanel.add(table);

        JScrollPane sp = new JScrollPane(table,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(sp);

        // add glue to fill the remaining space
        subpanel.add(Box.createGlue());

        RangeController rc = RangeController.getInstance();
        rc.addRangeChangedListener(this);

        initContextualMenu();
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

    public void rangeChangeReceived(RangeChangedEvent event) {
        if (isRecording) {
            BookmarkController bc = BookmarkController.getInstance();
            bc.addCurrentRangeToBookmarks();
        }
    }

    private void initContextualMenu() {
        tableMenu = new JPopupMenu();

        JMenuItem goMI = new JMenuItem("Go");
        goMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RangeController rc = RangeController.getInstance();
                rc.setRange(((Bookmark) ((BookmarksTableModel)table.getModel()).getData().get(table.getSelectedRow())).getRange());
            }
        });

        tableMenu.add ( goMI );

        JMenuItem deleteMI = new JMenuItem("Delete");
        deleteMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookmarkController fc = BookmarkController.getInstance();
                fc.removeBookmark(table.getSelectedRow());
            }
        });

        tableMenu.add ( deleteMI );
    }

    private static void loadBookmarks(JTable table) {
        BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
        List<Bookmark> bookmarks = btm.getData();

        if (bookmarks.size() > 0) {
            String message = "Clear existing bookmarks?";
            String title = "Clear Bookmarks";
                // display the JOptionPane showConfirmDialog
            int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
                bookmarks.clear();
        }

        JFrame jf = new JFrame();
        FileDialog fd = new FileDialog(jf, "Save Bookmarks", FileDialog.LOAD);
        fd.setVisible(true);
        jf.setAlwaysOnTop(true);

        // get the path (null if none selected)
        String selectedFileName = fd.getFile();

        Savant.log("load 0");

        // set the genome
        if (selectedFileName != null) {
            try {
                selectedFileName = fd.getDirectory() + selectedFileName;
                loadBookmarks(selectedFileName, bookmarks);
                btm.fireTableDataChanged();
            } catch (IOException ex) {
                String message = "Load unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveBookmarks(JTable table) {
        BookmarksTableModel btm = (BookmarksTableModel) table.getModel();
        List<Bookmark> bookmarks = btm.getData();

        JFrame jf = new JFrame();
        FileDialog fd = new FileDialog(jf, "Save Bookmarks", FileDialog.SAVE);
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
            bw.write(bm.getRange().getFrom() + "\t" + bm.getRange().getTo() + "\t" + bm.getAnnotation() + "\n");
        }

        bw.close();
    }

    private static void loadBookmarks(String filename, List<Bookmark> bookmarks) throws FileNotFoundException, IOException {


        Savant.log("load 1");

        BufferedReader br = new BufferedReader(new FileReader(filename));

        String line = "";

        while ((line = br.readLine()) != null) {
            Bookmark bm = parseBookmark(line);
            bookmarks.add(bm);
        }

        br.close();
    }

    private static Bookmark parseBookmark(String line) {

        Savant.log("load 2");

        StringTokenizer st = new StringTokenizer(line,"\t");

        int from = Integer.parseInt(st.nextToken());
        int to = Integer.parseInt(st.nextToken());
        String annotation = "";

        if (st.hasMoreElements()) {
            annotation = st.nextToken();
        }

        return new Bookmark(new Range(from,to), annotation);
    }
}
