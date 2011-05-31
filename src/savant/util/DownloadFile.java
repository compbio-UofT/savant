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

    public static File downloadFile(URL u, File destDir) {
        OutputStream out = null;
        InputStream in = null;
        File f = new File(destDir, u.getFile());

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

    public static String downloadFile(URL u) {

        StringBuilder result = new StringBuilder();
        InputStream in = null;

        try {
            in = u.openStream();
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                char [] r = (new String(buf)).toCharArray();
                result.append(r, 0, bytesRead);
            }
        } catch (IOException ioe) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
            } catch (NullPointerException n) {
                return null;
            }
        }

        return result.toString();
    }
}
