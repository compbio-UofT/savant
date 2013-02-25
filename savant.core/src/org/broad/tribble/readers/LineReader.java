package org.broad.tribble.readers;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: May 17, 2010
 * Time: 5:09:39 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LineReader {

    public String readLine() throws IOException;

    public void close();
}
