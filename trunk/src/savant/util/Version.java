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

import java.io.IOException;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author mfiume, tarkvara
 */
public class Version implements Comparable {
    private static final Log LOG = LogFactory.getLog(Version.class);

    int major;
    int minor;
    int revision;

    /**
     * Construct a version directly from a string of the form <i>major</i>.<i>minor</i>.<i>revision</i>.
     * @param versionString
     */
    public Version(String versionString) {
        String[] pieces = versionString.split("\\.");
        if (pieces.length != 3) {
            throw new IllegalArgumentException(String.format("{0} is not a valid version number.", versionString));
        }
        major = Integer.parseInt(pieces[0]);
        minor = Integer.parseInt(pieces[1]);
        revision = Integer.parseInt(pieces[2]);
    }

    /**
     * Factory method which construct a Version object from a URL pointing to an XML file.
     * @param uri
     * @return
     */
    public static Version fromURL(URL url) throws IOException {
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(NetworkUtils.openStream(url));
            boolean done = false;
            boolean foundCurrentVersion = false;
            do {
                switch (reader.next()) {
                    case XMLStreamConstants.START_ELEMENT:
                        String elemName = reader.getLocalName();
                        if (elemName.equals("version") && "current_release".equals(reader.getAttributeValue(null, "status"))) {
                            foundCurrentVersion = true;
                        } else if (foundCurrentVersion && elemName.equals("name")) {
                            return new Version(reader.getElementText());
                        } else {
                            foundCurrentVersion = false;
                        }
                        break;
                    case XMLStreamConstants.END_DOCUMENT:
                        reader.close();
                        done = true;
                        break;
                }
            } while (!done);
        } catch (XMLStreamException x) {
            throw new IOException("Unable to get version number from web-site.", x);
        }
        return null;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Version) {
            Version v1 = this;
            Version v2 = (Version) o;

            if (v1.major > v2.major) {
                return 1;
            } else if (v1.major < v2.major) {
                return -1;
            }

            if (v1.minor > v2.minor) {
                return 1;
            } else if (v1.minor < v2.minor) {
                return -1;
            }

            if (v1.revision > v2.revision) {
                return 1;
            } else if (v1.revision < v2.revision) {
                return -1;
            }

            return 0;
        }

        return 1;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + revision;
    }
}
