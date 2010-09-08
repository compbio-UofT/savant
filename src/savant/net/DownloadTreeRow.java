/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.net;

import com.jidesoft.grid.AbstractExpandableRow;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileSystemView;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;
import savant.view.icon.SavantIconFactory;

public class DownloadTreeRow extends AbstractExpandableRow implements Comparable<DownloadTreeRow> {

    private static final Long SIZE_NO_AVAILABLE = -1L;

    //DownloadRecord r;

    private boolean isLeaf;
        private List<?> children;

        private String name;
        private String type;
        private String description;
        private String url;
        private String size;

    public DownloadTreeRow(String name, List<DownloadTreeRow> r) {
        this.name = name;
        this.children = r;
    }

    public String getURL() {
        return this.url;
    }

    public DownloadTreeRow(
                String name,
                String type,
                String description,
                String url,
                String size) {
            this.isLeaf = true;
            this.type = type;
            this.name = name;
            this.description = description;
            this.url = url;
            this.size = size;
        }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public Object getValueAt(int columnIndex) {
        try {
            switch (columnIndex) {
                case 0:
                    return this;
                case 1:
                    return this.description;
                case 2:
                    return this.type;
                case 3:
                    return this.isLeaf() ? MiscUtils.getFilenameFromPath(this.getURL()) : null;
                case 4:
                    if (this.isLeaf) {
                        return this.size;
                    }
                    else {
                        return null;
                    }
            }
        }
        catch (SecurityException se) {
            // ignore
        }
        return null;
    }

    @Override
    public Class<?> getCellClassAt(int columnIndex) {
        return null;
    }

    public void setChildren(List<?> children) {
        this.children = children;
    }

    public boolean hasChildren() {
        return !this.isLeaf;
    }

    public List<?> getChildren() {
        if (this.children != null) {
            return this.children;
        }
        try {
            if (!this.isLeaf) {
                List<DownloadTreeRow> children = new ArrayList();
                List<DownloadTreeRow> fileChildren = new ArrayList();
                for (Object ch : this.children) {
                    DownloadTreeRow fileRow = (DownloadTreeRow) ch;
                    if (fileRow.isLeaf()) {
                        fileChildren.add(fileRow);
                    }
                    else {
                        children.add(fileRow);
                    }
                }
                children.addAll(fileChildren);
                setChildren(children);
            }
        }
        catch (SecurityException se) {
            // ignore
        }
        return this.children;
    }

    /*
    protected DownloadTreeRow createFileRow(DownloadTreeRow r) {
        return new DownloadTreeRow(r);
    }
     * 
     */

    /*
    public File getFile() {
        return _file;
    }
     * 
     */

    /*
    public Icon getIcon() {
        return _icon;
    }
     * 
     */

    static FileSystemView _fileSystemView;
    static FileSystemView getFileSystemView() {
        if (_fileSystemView == null) {
            _fileSystemView = FileSystemView.getFileSystemView();
        }
        return _fileSystemView;
    }

    public Icon getIcon() {

        if (this.isLeaf) {
            int ind = this.url.lastIndexOf(".");
            if (ind == -1) { return null; }
            String ext = this.url.substring(ind+1);
            String fn = DirectorySettings.getSavantDirectory()
                    + System.getProperty("file.separator")
                    + "." + ext;
            File f;
            try {
                f = File.createTempFile("savant_icon.", "." +ext);
                Icon i = getFileSystemView().getSystemIcon(f);
                f.delete();
                return i;
            } catch (IOException ex) {
                return null;
            }
        } else {
            return getFileSystemView().getSystemIcon(new File(DirectorySettings.getSavantDirectory()));
        }
    }

    /*
    public Icon getIcon() {
        if (this.isLeaf) {
            return SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK);
        } else {
            return SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.FOLDER);
        }
    }
     *
     */

    /*
    private Icon retrieveIcon() {
        Icon icon = _icons.get(this);
        if (icon == null) {
            icon = getIcon(getFile());
            _icons.put(this, icon);
            return icon;
        }
        else {
            return icon;
        }
    }
     *
     */
    

    /*
    public static Icon getIcon(File file) {
        return getFileSystemView().getSystemIcon(file);
    }

    public static String getTypeDescription(File file) {
        return getFileSystemView().getSystemTypeDescription(file);
    }
     * 
     */

    /*
    public static String getName(File file) {
        return getFileSystemView().getSystemDisplayName(file);
    }
     * 
     */

    public int compareTo(DownloadTreeRow o) {
        DownloadTreeRow fileRow = o;
        return getName().compareToIgnoreCase(fileRow.getName());
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getDescription() {
        return this.description;
    }
}

