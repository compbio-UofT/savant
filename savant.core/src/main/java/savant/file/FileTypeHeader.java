/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.file;

import savant.file.FileType;

/**
 *
 * @author mfiume
 */
public class FileTypeHeader {

    public FileType fileType;
    public int version;

    public FileTypeHeader(FileType ft, int v) {
        this.fileType = ft;
        this.version = v;
    }
}
