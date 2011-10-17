package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Mar 15, 2010
 * Time: 2:08:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class BBFileBinaryOutput {
    private static Logger log = Logger.getLogger(BBZoomLevelFormat.class);

    /*
    *   Constructor for BBFile Binary writer
    *
    *   Parameters:
    *       pathname - file pathname for receiving output
    *       buffer - byte array to be output
    *       offset - byte offset in array for mStartBase of output.
    *      len - number of bytes to write from offset.
    *
    *   returns:
    *       number of bytes written
    *
    * */
    public static int writeBuffer(String pathname, byte[] buffer, int offset, int len){
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(new File(pathname));
            fos.write(buffer, offset, len);
            fos.close();
        }
        catch(IOException ex){
            log.error("Error writing file " + pathname, ex);
            return(0);
        }

        // all written
        return(len);
    }
}
