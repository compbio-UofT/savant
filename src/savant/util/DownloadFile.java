/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 *
 * @author mfiume
 */
public class DownloadFile {

    public static File downloadFile(URL u, String destDir) {
        OutputStream out = null;
        InputStream in = null;
        File f = new File(destDir + System.getProperty("file.separator") + MiscUtils.getFilenameFromPath(u.getPath()));
        try {
            out = new FileOutputStream(f.getAbsolutePath());
            in = u.openStream();
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }
        } catch (IOException ioe) {
            return null;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioe) {
            } catch (NullPointerException n) {
                return null;
            }
        }

        return f;
    }
}
