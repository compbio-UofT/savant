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
package savant.view.dialog.tree;

import com.jidesoft.grid.AbstractExpandableRow;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;

public class TreeBrowserEntry extends AbstractExpandableRow implements Comparable<TreeBrowserEntry> {

    static FileSystemView _fileSystemView;

    private boolean isLeaf;
    private List<TreeBrowserEntry> children;
    private String name;
    private String type;
    private String description;
    private String url;
    private String size;

    public TreeBrowserEntry(String name, List<TreeBrowserEntry> r) {
        this.name = name;
        setChildren(r);
    }

    public TreeBrowserEntry(
                String name,
                String type,
                String description,
                String url,
                String size) {
            isLeaf = true;
            this.type = type;
            this.name = name;
            this.description = description;
            this.url = url;
            this.size = size;
        }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public Object getValueAt(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return this;
            case 1:
                return description;
            case 2:
                return type;
            case 3:
                return isLeaf ? MiscUtils.getFilenameFromPath(this.getURL()) : null;
            case 4:
                return isLeaf ? size : null;
        }
        return null;
    }

    @Override
    public Class<?> getCellClassAt(int columnIndex) {
        return null;
    }

    @Override
    public final void setChildren(List<?> value) {
        children = (List<TreeBrowserEntry>)value;
	if (children != null) {
            for (TreeBrowserEntry row : children) {
                row.setParent(this);
	    }
	}
    }

    @Override
    public boolean hasChildren() {
        return children != null;
    }

    @Override
    public List<?> getChildren() {
        return children;
    }

    static FileSystemView getFileSystemView() {
        if (_fileSystemView == null) {
            _fileSystemView = FileSystemView.getFileSystemView();
        }
        return _fileSystemView;
    }

    public Icon getIcon() {
        if (isLeaf) {
            int ind = url.lastIndexOf(".");
            if (ind == -1) { return null; }
            String ext = url.substring(ind+1);
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


    @Override
    public int compareTo(TreeBrowserEntry o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getURL() {
        return url;
    }

    public boolean isLeaf() {
        return isLeaf;
    }
}
