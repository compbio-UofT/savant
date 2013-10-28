/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.util;

import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;

/**
 * Simplest kind of file filter, just uses a file-extension.
 * 
 * @author tarkvara
 */
public class FileExtensionFilter extends FileFilter implements FilenameFilter {
    private String extension;
    private String description;
    
    /**
     * Construct a filter with the appropriate string.
     *
     * @param desc a descriptive string like "PNG files"
     * @param ext a file extension like "png" (no leading dot)
     */
    public FileExtensionFilter(String desc, String ext) {
        description = String.format("%s (*.%s)", desc, ext);
        extension = ext;
    }

    public FileExtensionFilter(String ext) {
        this(ext.toUpperCase() + " files", ext);
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String ext = MiscUtils.getExtension(f.getAbsolutePath());
        return extension.equals(ext);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith("." + extension);
    }
}
