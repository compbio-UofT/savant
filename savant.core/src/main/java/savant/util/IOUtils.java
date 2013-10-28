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

import java.io.*;


/**
 * I/O-related utility methods.  Functions for manipulating Savant
 * files are in SavantFileUtils.
 *
 * @author mfiume, tarkvara
 */
public class IOUtils {
    public static void copyFile(File srcFile, File destFile) throws IOException {
        if (srcFile.equals(destFile)) {
            return;
        }
        copyStream(new FileInputStream(srcFile), new FileOutputStream(destFile));
    }

    /**
     * Copy all files from one directory to another.
     * @param srcDir source directory
     * @param destDir destination directory
     */
    public static void copyDir(File srcDir, File destDir) throws IOException {
        File[] files = srcDir.listFiles();
        for (File f: files) {
            copyFile(f, new File(destDir, f.getName()));
        }
    }

    /**
     * Copy all files from one directory to another.
     * @param srcDir source directory
     * @param destDir destination directory
     */
    public static void copyDir(File srcDir, File destDir, FilenameFilter filter) throws IOException {
        File[] files = srcDir.listFiles(filter);
        for (File f: files) {
            copyFile(f, new File(destDir, f.getName()));
        }
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        try {
            while ((len = input.read(buf)) > 0 ){
                output.write(buf, 0, len);
            }
        } catch (IOException x) {
            // There's a bug in BlockCompressedInputStream where it throws an IOException instead of doing a proper EOF.
            // Suppress this exception, but throw real ones.
            if (!x.getMessage().equals("Unexpected compressed block length: 1")) {
                throw x;
            }
        } finally {
            input.close();
            output.close();
        }
    }

    /**
     * Cheesy method which lets us read from an InputStream without having to instantiate a BufferedReader.
     * Intended to get around some glitches reading GZIPInputStreams over an HTTP stream.
     */
    public static String readLine(InputStream input) throws IOException {
        StringBuilder buf = new StringBuilder();
        int c;
        while ((c = input.read()) >= 0 && c != '\n') {
            buf.append((char)c);
        }
        return c >= 0 ? buf.toString() : null;
    }

    /**
     * Recursively delete a directory.
     */
    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }
}