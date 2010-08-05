/*
 *    Copyright 2010 University of Toronto
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

/*
 * HttpUtils.java
 * Created on Aug 4, 2010
 */

package savant.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Some useful methods for performing HTTP-related functions.
 *
 * @author vwilliams
 */
public class HttpUtils {

    private static Log log = LogFactory.getLog(HttpUtils.class);

    public static String getETag(URL fileURL) throws IOException {

        // ensure that this is an HTTP url
        if (!fileURL.getProtocol().equalsIgnoreCase("http"))
            throw new IllegalArgumentException("Invalid argument; Can only retrieve ETag for HTTP URLs");

        URLConnection conn = null;
        try {
            conn = fileURL.openConnection();
            return conn.getHeaderField("ETag");
        }
        finally {
            if (conn instanceof HttpURLConnection) ((HttpURLConnection) conn).disconnect();    
        }
    }
}
