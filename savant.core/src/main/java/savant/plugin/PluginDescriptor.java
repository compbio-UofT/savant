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
package savant.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import savant.api.util.DialogUtils;
import savant.settings.DirectorySettings;
import savant.view.dialog.DownloadDialog;


/**
 * Plugin description read from the plugin.xml file.
 *
 * @author tarkvara
 */
public class PluginDescriptor implements Comparable<PluginDescriptor> {

    /**
     * Bare-bones set of tags we need to recognise in plugin.xml in order to identify plugins.
     */
    private enum PluginXMLElement {
        PLUGIN,
        ATTRIBUTE,
        PARAMETER,
        LIB,
        DOWNLOAD,
        IGNORED
    };

    /**
     * Bare-bones set of attributes we need to recognise in plugin.xml in order to identify plugins.
     */
    private enum PluginXMLAttribute {
        ID,
        VALUE,
        VERSION,
        CLASS,
        SDK_VERSION,
        NAME,
        IGNORED
    };


    final String className;
    final String id;
    final String version;
    final String name;
    final String sdkVersion;
    final File file;
    final File[] libs;
    final URL[] downloads;

    private static XMLStreamReader reader;

    private PluginDescriptor(String className, String id, String version, String name, String sdkVersion, File file, File[] libs, URL[] downloads) {
        if (className == null || id == null || version == null || name == null || file == null) {
            throw new IllegalArgumentException("Null argument passed to PluginDescriptor constructor.");
        }
        this.className = className;
        this.id = id;
        this.version = version;
        this.name = name;
        this.sdkVersion = sdkVersion != null ? sdkVersion : "1.4.2 or earlier";
        this.file = file;
        this.libs = libs;
        this.downloads = downloads;
    }

    @Override
    public String toString() {
        return id + "-" + version;
    }

    public String getClassName() {
        return className;
    }

    public String getID() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getSDKVersion() {
        return sdkVersion;
    }

    public File getFile() {
        return file;
    }

    /**
     * Retrieve an array of all jar files required by this plugin.  In most cases, this
     * will just be the plugin's own jar, but in 2.0.0 a plugin can use &lt;lib> tags to
     * specify additional external jars.
     */
    public URL[] getJars() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        if (file.getName().endsWith(".jar")) {
            urls.add(file.toURI().toURL());
            for (File f: libs) {
                if (f.isAbsolute()) {
                    urls.add(f.toURI().toURL());
                } else {
                    // Lib file is relative to plugin location.
                    urls.add(new File(file.getParentFile(), f.getPath()).toURI().toURL());
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
     * Retrieve an array of all downloadable files required by this plugin.  Similar to getJars, but
     * intended for tool plugins.
     */
    public void downloadExtras() throws MalformedURLException {
        for (URL u: downloads) {
            DownloadDialog dd = new DownloadDialog(DialogUtils.getMainWindow(), true);
            dd.downloadFile(u, DirectorySettings.getPluginsDirectory(), null);
        }
    }

    @Override
    public int compareTo(PluginDescriptor t) {
        return (id + version).compareTo(t.id + t.version);
    }


    /**
     * Here's where we do our SDK compatibility check.  Update this code whenever the API changes.
     */
    public boolean isCompatible() {
        return sdkVersion.equals("2.0.0");
    }

    /**
     * Parse the given input stream to get the plugin attributes.
     */
    private static PluginDescriptor fromStream(InputStream input, File f) throws XMLStreamException, MalformedURLException {
        reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
        String className = null;
        String id = null;
        String version = null;
        String sdkVersion = null;
        String name = null;
        List<File> libs = new ArrayList<File>();
        List<URL> downloads = new ArrayList<URL>();
        do {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (readElement()) {
                        case PLUGIN:
                            className = readAttribute(PluginXMLAttribute.CLASS);
                            id = readAttribute(PluginXMLAttribute.ID);
                            version = readAttribute(PluginXMLAttribute.VERSION);
                            sdkVersion = readAttribute(PluginXMLAttribute.SDK_VERSION);
                            name = readAttribute(PluginXMLAttribute.NAME);
                            break;
                        case ATTRIBUTE:
                            // Older plugins store the SDK version in a special <attributes> tag.
                            if ("sdk-version".equals(readAttribute(PluginXMLAttribute.ID))) {
                                sdkVersion = readAttribute(PluginXMLAttribute.VALUE);
                            }
                            break;
                        case PARAMETER:
                            if ("name".equals(readAttribute(PluginXMLAttribute.ID))) {
                                name = readAttribute(PluginXMLAttribute.VALUE);
                            }
                            break;
                        case LIB:
                            libs.add(new File(reader.getElementText()));
                            break;
                        case DOWNLOAD:
                            downloads.add(new URL(reader.getElementText()));
                            break;
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    reader.close();
                    reader = null;
                    break;
            }
        } while (reader != null);

        // Will throw an IllegalArgumentException if one of our required attributes has not been set.
        return new PluginDescriptor(className, id, version, name, sdkVersion, f, libs.toArray(new File[0]), downloads.toArray(new URL[0]));
    }
 
    /**
     * For a true plugin, the plugin.xml file lives inside a Jar file.
     * @throws PluginVersionException 
     */
    public static PluginDescriptor fromFile(File f) throws PluginVersionException {
        try {
            if (f.getName().endsWith(".jar")) {
                // For a true Java plugin, the descriptor is inside a jar file.
                JarFile jar = new JarFile(f);
                ZipEntry entry = jar.getEntry("plugin.xml");
                if (entry != null) {
                    return fromStream(jar.getInputStream(entry), f);
                }
            } else {
                // For a tool, the descriptor is just a bare XML file.
                return fromStream(new FileInputStream(f), f);
            }
        } catch (Exception x) {
            PluginController.LOG.info("Unable to get plugin from " + f, x);
        }
        throw new PluginVersionException(f.getName() + " did not contain a valid plugin");
    }

    private static PluginXMLElement readElement() {
        try {
            String elemName = reader.getLocalName().toUpperCase();
            return Enum.valueOf(PluginXMLElement.class, elemName);
        } catch (IllegalArgumentException ignored) {
            // Any elements not in our enum will just be ignored.
            return PluginXMLElement.IGNORED;
        }
    }

    private static String readAttribute(PluginXMLAttribute attr) {
        return reader.getAttributeValue(null, attr.toString().toLowerCase().replace('_', '-'));
    }


}
