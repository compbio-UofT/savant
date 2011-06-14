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
package savant.controller;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.swing.JPanel;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.event.PluginEvent;
import savant.experimental.PluginTool;
import savant.plugin.PluginDescriptor;
import savant.plugin.SavantDataSourcePlugin;
import savant.plugin.SavantPanelPlugin;
import savant.plugin.SavantPlugin;
import savant.settings.DirectorySettings;
import savant.util.IOUtils;
import savant.util.MiscUtils;
import savant.view.tools.ToolsModule;

/**
 *
 * @author mfiume, tarkvara
 */
public class PluginController extends Controller {

    private static final Log LOG = LogFactory.getLog(PluginController.class);
    private static final String UNINSTALL_FILENAME = ".uninstall_plugins";

    private static PluginController instance;

    private File uninstallFile;
    private List<String> pluginsToRemove = new ArrayList<String>();
    private HashMap<String, PluginDescriptor> validPlugins = new HashMap<String, PluginDescriptor>();
    private HashMap<String, SavantPlugin> loadedPlugins = new HashMap<String, SavantPlugin>();
    private HashMap<String, String> pluginErrors = new HashMap<String, String>();
    private PluginLoader pluginLoader;
    

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
     * Try to load all JAR files in the given directory.
     */
    public void loadPlugins(File pluginsDir) {
        File[] files = pluginsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar");
            }
        });
        for (File f: files) {
            try {
                PluginDescriptor desc = PluginDescriptor.fromFile(f);
                if (desc != null) {
                    if (desc.isCompatible()) {
                        LOG.info("Found compatible " + desc.getID() + "-" + desc.getVersion());
                        validPlugins.put(desc.getID(), desc);
                    } else {
                        LOG.info("Found incompatible " + desc.getID() + "-" + desc.getVersion() + " (SDK version " + desc.getSDKVersion() + ")");
                        pluginErrors.put(desc.getID(), "Invalid SDK version (" + desc.getSDKVersion() + ")");
                    }
                }
            } catch (IOException x) {
                LOG.warn("No plugin found in " + f);
            }
        }

        if (validPlugins.size() > 0) {
            Set<URL> jarURLs = new HashSet<URL>();
            for (PluginDescriptor desc: validPlugins.values()) {
                try {
                    jarURLs.add(desc.getFile().toURI().toURL());
                } catch (MalformedURLException ignored) {
                }
            }
            pluginLoader = new PluginLoader(jarURLs.toArray(new URL[0]));

            for (final PluginDescriptor desc: validPlugins.values()) {
                new Thread("PluginLoader") {
                    @Override
                    public void run() {
                        try {
                            loadPlugin(desc);
                        } catch (Throwable x) {
                            LOG.error("Unable to load " + desc.getName(), x);
                            pluginErrors.put(desc.getID(), "Error");
                        }
                    }
                }.start();
            }
        }
    }

    public Collection<PluginDescriptor> getDescriptors() {
        return validPlugins.values();
    }

    public SavantPlugin getPlugin(String id) {
        return loadedPlugins.get(id);
    }

    public void queuePluginForRemoval(String id) {
        FileWriter fstream = null;
        try {
            PluginDescriptor info = validPlugins.get(id);
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
        if (validPlugins.get(id) != null) {
            // Plugin is valid, but hasn't shown up in the loadedPlugins map.
            return "Loading";
        }
        String err = pluginErrors.get(id);
        if (err != null) {
            return err;
        }
        return "Unknown";
    }

    /**
     * Give plugin tool an opportunity to initialise itself.
     *
     * @param plugin
     */
    private void initPluginTool(PluginTool plugin) {
        plugin.init();
        ToolsModule.addTool(plugin);
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
        if (MiscUtils.MAC) {
            srcDir = new File(com.apple.eio.FileManager.getPathToApplicationBundle() + "/Contents/Plugins");
            if (srcDir.exists()) {
                try {
                    IOUtils.copyDir(srcDir, destDir);
                    return;
                } catch (Exception ignored) {
                    // We should expect to see this when running in the debugger.
                }
            }
        }
        try {
            srcDir = new File("plugins");
            IOUtils.copyDir(srcDir, destDir);
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
        } else if (plugin instanceof PluginTool) {
            initPluginTool((PluginTool)plugin);
        } else if (plugin instanceof SavantDataSourcePlugin) {
            initSavantDataSourcePlugin((SavantDataSourcePlugin)plugin);
        }
        loadedPlugins.put(desc.getID(), plugin);
        fireEvent(new PluginEvent(PluginEvent.Type.ADDED, desc.getID(), canvas));
    }

    /**
     * Try to add a plugin from the given file.
     */
    public void addPlugin(File f) throws Throwable {
        PluginDescriptor desc = PluginDescriptor.fromFile(f);
        if (desc != null) {
            pluginLoader.addJar(f);
            loadPlugin(desc);
        }
    }


    class PluginLoader extends URLClassLoader {
        PluginLoader(URL[] urls) {
            super(urls);
        }

        void addJar(File f) {
            try {
                addURL(f.toURI().toURL());
            } catch (MalformedURLException ignored) {
            }
        }
    }
}


