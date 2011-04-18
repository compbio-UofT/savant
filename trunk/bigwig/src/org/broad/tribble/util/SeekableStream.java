package org.broad.tribble.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: Nov 29, 2009
 * Time: 10:39:49 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SeekableStream extends InputStream {

    public abstract void seek(long position) throws IOException;

    public abstract long position() throws IOException;
}