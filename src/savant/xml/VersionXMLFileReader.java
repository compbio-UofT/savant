/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.xml;

import java.io.File;
import java.io.IOException;
import org.jdom.*;
import org.jdom.input.*;

/**
 *
 * @author mfiume
 */
public class VersionXMLFileReader {

    public static class Version implements Comparable {
        int major;
        int minor;
        int revision;

        public Version(String vstring) {
            int t_ind = vstring.indexOf(".");
            String majstr = vstring.substring(0,t_ind); 
            vstring = vstring.substring(t_ind+1);
            t_ind = vstring.indexOf(".");
            String minstr = vstring.substring(0,t_ind);
            vstring = vstring.substring(t_ind+1);
            String revstr = vstring;

            major = Integer.parseInt(majstr);
            minor = Integer.parseInt(minstr);
            revision = Integer.parseInt(revstr);
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

        public String toString() {
            return this.major + "." + this.minor + "." + this.revision;
        }

    }

    Version version;

    public VersionXMLFileReader(String path) throws IOException, JDOMException {
        this(new File(path));
    }

    public VersionXMLFileReader(File f) throws IOException, JDOMException {
        Document d = new SAXBuilder().build(f);

        Element root = d.getRootElement();

        Element currentRelease = getChildWithAttribute(root, "version", "current_release");

        version = new Version(currentRelease.getChildText("name"));
    }

    public Version getVersion() { return version; }

    /*
    private String parseVersion(Element e) {
        return e.getChildText("name");
    }
     * 
     */

    private Element getChildWithAttribute(Element e, String name, String attribute) {
        for (Object c : e.getChildren(name)) {
            Element el = (Element) c;
            if (el.getAttributeValue("status").equals("current_release")) {
                return el;
            }
        }
        return null;
    }
}
