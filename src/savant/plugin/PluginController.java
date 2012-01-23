/*
 *    Copyright 2010-2012 University of Toronto
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
package savant.plugin;

import java.awt.BorderLayout;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.event.PluginEvent;
import savant.api.util.DialogUtils;
import savant.controller.DataSourcePluginController;
import savant.settings.BrowserSettings;
import savant.settings.DirectorySettings;
import savant.util.Controller;
import savant.util.IOUtils;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;

/**
 *
 * @author mfiume, tarkvara
 */
public class PluginController extends Controller {

    static final Log LOG = LogFactory.getLog(PluginController.class);
    private static final String UNINSTALL_FILENAME = ".uninstall_plugins";

    private static PluginController instance;

    private File uninstallFile;
    private List<String> pluginsToRemove = new ArrayList<String>();
    private Map<String, PluginDescriptor> knownPlugins = new HashMap<String, PluginDescriptor>();
    private Map<String, SavantPlugin> loadedPlugins = new HashMap<String, SavantPlugin>();
    private Map<String, String> pluginErrors = new LinkedHashMap<String, String>();
    private PluginLoader pluginLoader;
    private PluginIndex repositoryIndex = null;
    

    /** SINGLETON **/
    public static synchronized PluginController getInstance() {
        if (instance == null) {
            instance = new PluginController();
        }
        return instance;
    }

    /**
     * Private constructor.  Should only be called by getInstance().
     */
    private PluginController() {
        try {
            uninstallFile = new File(DirectorySettings.getSavantDirectory(), UNINSTALL_FILENAME);

            LOG.info("Uninstall list " + UNINSTALL_FILENAME);
            if (uninstallFile.exists()) {
                deleteFileList(uninstallFile);
            }
            copyBuiltInPlugins();
        } catch (Exception ex) {
            LOG.error("Error loading plugins.", ex);
        }
    }

    /**
     * Try to load all JAR and XML files in the given directory.
     */
    @SuppressWarnings("CallToThreadRun")
    public void loadPlugins(File pluginsDir) {
        File[] files = pluginsDir.listFiles(new PluginFileFilter());
        for (File f: files) {
            try {
                addPlugin(f);
            } catch (PluginVersionException x) {
                LOG.warn("No compatible plugins found in " + f);
            }
        }

        // Check to see if we have any outdated plugins.
        if (pluginErrors.size() > 0) {
            List<String> updated = new ArrayList<String>();
            for (String s: pluginErrors.keySet()) {
                // Plugin is invalid, and we don't have a newer version.
                if (checkForPluginUpdate(s)) {
                    updated.add(s);
                }
            }
            if (updated.size() > 0) {
                DialogUtils.displayMessage("Plugins Updated", String.format("<html>The following plugins were updated to be compatible with Savant %s:<br><br><i>%s</i></html>", BrowserSettings.VERSION, StringUtils.join(updated, ", ")));
                for (String s: updated) {
                    pluginErrors.remove(s);
                }
            }
            if (pluginErrors.size() > 0) {
                StringBuilder errorStr = null;
                for (String s: pluginErrors.keySet()) {
                    if (errorStr == null) {
                        errorStr = new StringBuilder();
                    } else {
                        errorStr.append("<br>");
                    }
                    errorStr.append(s);
                    errorStr.append(" – ");
                    errorStr.append(pluginErrors.get(s));
                }
                if (errorStr != null) {
                    // The following dialog will only report plugins which we can tell are faulty before calling loadPlugin(), typically
                    // by checking the version in plugin.xml.
                    DialogUtils.displayMessage("Plugins Not Loaded", String.format("<html>The following plugins could not be loaded:<br><br><i>%s</i><br><br>They will not be available to Savant.</html>", errorStr));
                }
            }
        }

        Set<URL> jarURLs = new HashSet<URL>();
        for (PluginDescriptor desc: knownPlugins.values()) {
            try {
                if (!pluginErrors.containsKey(desc.getID())) {
                    jarURLs.addAll(Arrays.asList(desc.getJars()));
                }
            } catch (MalformedURLException ignored) {
            }
        }
        if (jarURLs.size() > 0) {
            pluginLoader = new PluginLoader(jarURLs.toArray(new URL[0]), getClass().getClassLoader());

            // If present, the savant.data plugin always gets loaded first, and in the main thread.
            PluginDescriptor dataDesc = knownPlugins.get("savant.data");
            if (dataDesc != null && !pluginErrors.containsKey("savant.data")) {
                new LoaderThread(dataDesc).run();
            }
            for (PluginDescriptor desc: knownPlugins.values()) {
                if (desc != dataDesc && !pluginErrors.containsKey(desc.getID())) {
                    new LoaderThread(desc).start();
                }
            }
        }
    }

    public List<PluginDescriptor> getDescriptors() {
        List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();
        result.addAll(knownPlugins.values());
        Collections.sort(result);
        return result;
    }

    public SavantPlugin getPlugin(String id) {
        return loadedPlugins.get(id);
    }

    public void queuePluginForRemoval(String id) {
        FileWriter fstream = null;
        try {
            PluginDescriptor info = knownPlugins.get(id);
            LOG.info("Adding plugin " + info.getFile().getAbsolutePath() + " to uninstall list " + uninstallFile.getPath());

            if (!uninstallFile.exists()) {
                uninstallFile.createNewFile();
            }
            fstream = new FileWriter(uninstallFile, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(info.getFile().getAbsolutePath() + "\n");
            out.close();

            DialogUtils.displayMessage("Uninstallation Complete", "Please restart Savant for changes to take effect.");
            pluginsToRemove.add(id);

            fireEvent(new PluginEvent(PluginEvent.Type.QUEUED_FOR_REMOVAL, id, null));

        } catch (IOException ex) {
            LOG.error("Error uninstalling plugin: " + uninstallFile, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isPluginQueuedForRemoval(String id) {
        return pluginsToRemove.contains(id);
    }

    public String getPluginStatus(String id) {
        if (pluginsToRemove.contains(id)) {
            return "Queued for removal";
        }
        if (loadedPlugins.get(id) != null) {
            return "Loaded";
        }
        String err = pluginErrors.get(id);
        if (err != null) {
            return err;
        }
        if (knownPlugins.get(id) != null) {
            // Plugin is valid, but hasn't shown up in the loadedPlugins map.
            return "Loading";
        }
        return "Unknown";
    }

    /**
     * Give a panel plugin an opportunity to initialise itself.
     *
     * @param plugin
     */
    private JPanel initGUIPlugin(SavantPanelPlugin plugin) {
        JPanel canvas = new JPanel();
        canvas.setLayout(new BorderLayout());
        plugin.init(canvas);
        return canvas;
    }

    /**
     * Give a DataSource plugin a chance to initalise itself.
     * @param plugin
     */
    private void initSavantDataSourcePlugin(SavantDataSourcePlugin plugin) {
        DataSourcePluginController.getInstance().addDataSourcePlugin(plugin);
        plugin.init();
    }


    private void deleteFileList(File fileListFile) {
        BufferedReader br = null;
        String line = "";
        try {
            br = new BufferedReader(new FileReader(fileListFile));

            while ((line = br.readLine()) != null) {
                LOG.info("Uninstalling " + line);
                if (!new File(line).delete()) {
                    throw new IOException("Delete of " + line + " failed");
                }
            }
        } catch (IOException ex) {
            LOG.error("Problem uninstalling " + line, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
            }
        }
        fileListFile.delete();
    }

    private void copyBuiltInPlugins() {
        File destDir = DirectorySettings.getPluginsDirectory();
        File srcDir = null;
        FilenameFilter pluginFilter = new PluginFileFilter();
        if (MiscUtils.MAC) {
            srcDir = new File(com.apple.eio.FileManager.getPathToApplicationBundle() + "/Contents/Plugins");
            if (srcDir.exists()) {
                try {
                    IOUtils.copyDir(srcDir, destDir, pluginFilter);
                    return;
                } catch (Exception ignored) {
                    // We should expect to see this when running in the debugger.
                }
            }
        }
        try {
            srcDir = new File("plugins");
            IOUtils.copyDir(srcDir, destDir, pluginFilter);
        } catch (Exception x) {
            LOG.error("Unable to copy builtin plugins from " + srcDir.getAbsolutePath() + " to " + destDir, x);
        }
    }


    private void loadPlugin(PluginDescriptor desc) throws Throwable {
        Class pluginClass = pluginLoader.loadClass(desc.getClassName());
        SavantPlugin plugin = (SavantPlugin)pluginClass.newInstance();
        plugin.setDescriptor(desc);

        // Init the plugin based on its type
        JPanel canvas = null;
        if (plugin instanceof SavantPanelPlugin) {
            canvas = initGUIPlugin((SavantPanelPlugin)plugin);
        } else if (plugin instanceof SavantDataSourcePlugin) {
            initSavantDataSourcePlugin((SavantDataSourcePlugin)plugin);
        }
        loadedPlugins.put(desc.getID(), plugin);
        fireEvent(new PluginEvent(PluginEvent.Type.LOADED, desc.getID(), canvas));
    }

    /**
     * Try to add a plugin from the given file.  It is inserted into our internal
     * data structures, but not yet loaded.
     */
    public PluginDescriptor addPlugin(File f) throws PluginVersionException {
        PluginDescriptor desc = PluginDescriptor.fromFile(f);
        if (desc != null) {
            LOG.info("Found usable " + desc + " in " + f.getName());
            PluginDescriptor existingDesc = knownPlugins.get(desc.getID());
            if (existingDesc != null && existingDesc.getVersion().compareTo(desc.getVersion()) >= 0) {
                LOG.info("   Ignored " + desc + " due to presence of existing " + existingDesc);
                return null;
            }
            knownPlugins.put(desc.getID(), desc);
            if (desc.isCompatible()) {
                if (existingDesc != null) {
                    LOG.info("   Replaced " + existingDesc);
                    pluginErrors.remove(desc.getID());
                }
            } else {
                LOG.info("Found incompatible " + desc + " (SDK version " + desc.getSDKVersion() + ") in " + f.getName());
                pluginErrors.put(desc.getID(), "Invalid SDK version (" + desc.getSDKVersion() + ")");
                throw new PluginVersionException("Invalid SDK version (" + desc.getSDKVersion() + ")");
            }
        }
        return desc;
    }

    /**
     * Copy the given file to the plugins directory, add it, and load it.
     * @param selectedFile
     */
    public void installPlugin(File selectedFile) throws Throwable {
        File pluginFile = new File(DirectorySettings.getPluginsDirectory(), selectedFile.getName());
        IOUtils.copyFile(selectedFile, pluginFile);
        PluginDescriptor desc = addPlugin(pluginFile);
        if (desc != null) {
            if (pluginLoader == null) {
                pluginLoader = new PluginLoader(desc.getJars(), getClass().getClassLoader());
            } else {
                pluginLoader.addJars(desc.getJars());
            }
            loadPlugin(desc);
        }
    }

    private boolean checkForPluginUpdate(String id) {
        try {
            if (repositoryIndex == null) {
                repositoryIndex = new PluginIndex(BrowserSettings.PLUGIN_URL);
            }
            URL updateURL = repositoryIndex.getPluginURL(id);
            if (updateURL != null) {
                // The following assumes that plugins in the repository have a naming scheme
                // which exposes their version number as -1.2.3.jar.  Since we manage the
                // repository we can enforce this naming convention.
                String repoVersion = MiscUtils.getFilenameFromPath(updateURL.getFile());
                repoVersion = repoVersion.substring(repoVersion.indexOf('-') + 1, repoVersion.lastIndexOf('.'));
                if (repoVersion.compareTo(knownPlugins.get(id).version) > 0) {
                    LOG.info("Downloading updated version of " + id + " from " + updateURL);
                    addPlugin(NetworkUtils.downloadFile(updateURL, DirectorySettings.getPluginsDirectory(), null));
                    return true;
                } else {
                    LOG.info("Repository version " + updateURL + " is no newer than local version of " + id + ".");
                }
            }
        } catch (IOException x) {
            LOG.error("Unable to install update for " + id, x);
        } catch (PluginVersionException x) {
            LOG.error("Update for " + id + " not loaded.");
        }
        return false;
    }

    class LoaderThread extends Thread {
        PluginDescriptor desc;

        LoaderThread(PluginDescriptor pd) {
            super("PluginLoader-" + pd);
            desc = pd;
        }
        
        @Override
        public void run() {
            try {
                loadPlugin(desc);
            } catch (Throwable x) {
                LOG.error("Unable to load " + desc.getName(), x);
                pluginErrors.put(desc.getID(), x.getClass().getName());
                DialogUtils.displayMessage("Plugin Not Loaded", String.format("<html>The following plugin could not be loaded:<br><br><i>%s – %s</i><br><br>It will not be available to Savant.</html>", desc.getID(), x));
            }
        }
    }

    class PluginLoader extends URLClassLoader {
        PluginLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        void addJars(URL[] urls) {
            for (URL u: urls) {
                addURL(u);
            }
        }
    }
    
    class PluginFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith(".jar") || name.endsWith(".xml");
        }
    }
}
