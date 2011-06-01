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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginManager;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginAttribute;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;

import savant.api.util.DialogUtils;
import savant.experimental.PluginTool;
import savant.plugin.*;
import savant.settings.BrowserSettings;
import savant.settings.DirectorySettings;
import savant.util.FileUtils;
import savant.util.MiscUtils;
import savant.view.swing.DockableFrameFactory;
import savant.view.swing.Savant;
import savant.view.tools.ToolsModule;

/**
 *
 * @author mfiume
 */
public class PluginController {

    private static final Log LOG = LogFactory.getLog(PluginController.class);
    private static final String UNINSTALL_FILENAME = ".uninstall_plugins";

    private static PluginController instance;
    private static PluginManager pluginManager;
    private ExtensionPoint coreExtPt;
    private File coreLocation;
    private File uninstallFile;

    private Set<String> pluginsToUnInstall = new HashSet<String>();

    private Map<String, Extension> pluginIDToExtensionMap = new HashMap<String, Extension>();
    private Map<String, Plugin> pluginIDToPluginMap = new HashMap<String, Plugin>();


    /** SINGLETON **/
    public static synchronized PluginController getInstance() {
        if (instance == null) {
            instance = new PluginController();
        }
        return instance;
    }

    /** CONSTRUCTOR **/
    public PluginController() {
        try {
            pluginManager = ObjectFactory.newInstance().createManager();
            uninstallFile = new File(DirectorySettings.getSavantDirectory(), UNINSTALL_FILENAME);

            LOG.info("Uninstall list " + UNINSTALL_FILENAME);
            if (uninstallFile.exists()) {
                deleteFileList(uninstallFile);
            }
            File libDir = DirectorySettings.getLibsDirectory();
            coreLocation = new File(libDir, "SavantCore.jar");
            File pluginsDir = DirectorySettings.getPluginsDirectory();

            copyBuiltInPlugins();

            /*
            // On a fresh install, the Mac version may want to copy its default plugins
            // from within the application bundle to the .savant/plugins directory.
            if (MiscUtils.MAC && !coreLocation.exists()) {
                pluginsDir.mkdirs();
                FileUtils.copyDir(new File("Savant.app/Contents/Plugins"), pluginsDir);
            }
             * 
             */

            LOG.info("coreLocation=" + coreLocation.getAbsolutePath());
            loadCorePlugin();
            loadPlugins(pluginsDir);
        } catch (Exception ex) {
            LOG.error("Error loading plugins.", ex);
        }
    }

    /** PLUGIN LOADING **/
    private void loadPlugins(File pluginsDir) throws MalformedURLException, JpfException, InstantiationException, IllegalAccessException {
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".gz");
            }
        });
        for (int i = 0; i < plugins.length; i++) {
            if (!plugins[i].getAbsolutePath().equals(coreLocation.getAbsolutePath())) {
                loadPlugin(plugins[i]);
            }
        }
    }

    private void loadPlugin(File pluginLocation) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        locs[0] = StandardPluginLocation.create(pluginLocation);
        if (locs[0] != null) {
            pluginManager.publishPlugins(locs);
            if (!activatePublishedPlugin()) {
                LOG.warn("Unable to load plugin: " + pluginLocation.getAbsolutePath());
                int result = DialogUtils.askYesNo("Unable to load plugin at " + pluginLocation.getAbsolutePath() + ".\nPlease check if it is compatible with this version of Savant.\nUninstall it?");
                if (result == DialogUtils.YES) {
                    this.queuePluginForUnInstallation(pluginLocation);
                }
            }
        } else {
            LOG.warn("Unable to load plugin: " + pluginLocation);
        }
    }

    /** PLUGIN ACTIVATION **/
    private void deactivatePlugin(String path) {
        pluginManager.deactivatePlugin(path);
    }

    public void queuePluginForUnInstallation(File path) {
        FileWriter fstream = null;
        try {
            LOG.info("Adding plugin " + path.getAbsolutePath() + " to uninstall list " + uninstallFile.getPath());

            if (!uninstallFile.exists()) {
                uninstallFile.createNewFile();
            }
            fstream = new FileWriter(uninstallFile, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(path.getAbsolutePath() + "\n");
            out.close();

            DialogUtils.displayMessage("Uninstallation Complete", "Please restart Savant for changes to take effect.");

        } catch (IOException ex) {
            LOG.error("Error uninstalling plugin: " + uninstallFile, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void queuePluginForUnInstallation(PluginDescriptor pd) {
        try {
            String foo = pd.getLocation().toString().replace("jar:", "").replace("!/plugin.xml", "");
            URI jpfURI = new URI(foo);

            queuePluginForUnInstallation(new File(jpfURI));
        } catch (URISyntaxException ex) {
            LOG.error("Bogus URI from Java Plugin Framework.", ex);
        }
    }

    private boolean activatePlugin(PluginDescriptor desc, Extension ext) {

        try {
            Plugin pluginInstance = pluginManager.getPlugin(desc.getId());

            // init the plugin based on its type
            if (pluginInstance instanceof SavantPanelPlugin) {
                initGUIPlugin((SavantPanelPlugin)pluginInstance);
            } else if (pluginInstance instanceof PluginTool) {
                initPluginTool((PluginTool)pluginInstance);
            } else if (pluginInstance instanceof SavantDataSourcePlugin) {
                initSavantDataSourcePlugin((SavantDataSourcePlugin)pluginInstance);
            }
            pluginIDToPluginMap.put(pluginInstance.getDescriptor().getId(), pluginInstance);
            pluginIDToExtensionMap.put(pluginInstance.getDescriptor().getId(), ext);
            return true;

        } catch (Exception ex) {
            LOG.error("Unable to activate plugin: " + desc.getPluginClassName(), ex);
        }

        return false;
    }

    /** ACTIVATE NEW **/
    private boolean activatePublishedPlugin() {
        
        boolean oneLoaded = false;
        boolean error = false;

        for (Extension ext : coreExtPt.getConnectedExtensions()) {
            if (!pluginIDToExtensionMap.containsValue(ext) && !isPluginQueuedForDeletion(ext.getDeclaringPluginDescriptor().getId()) && !isIgnoredBadPlugin(ext.getDeclaringPluginDescriptor()) ) {

                try {
                    LOG.info("Activating new plugin: " + ext.getDeclaringPluginDescriptor());
                    PluginAttribute sdkatt = ext.getDeclaringPluginDescriptor().getAttribute("sdk-version");
                    if (sdkatt == null) {
                        error = true;
                        LOG.error("No SDK-version attribute");
                    } else {
                        String sdk = sdkatt.getValue();
                        if (isSDKCompatibleWithThisVersion(sdk)) {
                            LOG.info("Compatible with this version of Savant");
                            activatePlugin(ext.getDeclaringPluginDescriptor(), ext);
                            oneLoaded = true;
                        } else {
                            LOG.info("Compatible with SDK-version: " + sdkatt.getValue());
                            error = true;
                        }
                    }
                } catch (Throwable e) {
                    LOG.error("Unable to load plugin: " + ext, e);
                    error = true;
                }
                if (error) {
                    this.addToIgnoredBadPlugin(ext.getDeclaringPluginDescriptor());
                }
            }
        }

        return oneLoaded && !error;
    }

    /** ADD AND REMOVE PLUGINS **/
    public void installPlugin(String path) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        loadPlugin(new File(path));
    }

    public String getFilePathOfPlugin(String pluginPath) {
        pluginPath = pluginPath.replaceAll("!/plugin.xml", "");
        pluginPath = pluginPath.replaceAll("file:/", "");
        return pluginPath;
    }

    /** CORE PLUGIN **/
    private void loadCorePlugin() throws MalformedURLException, JpfException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        java.net.URL[] mans = new java.net.URL[1];


        if (!coreLocation.exists()) {
            LOG.error("Loading of core plugin failed.");
            DialogUtils.displayError("Error initializing plugin loader. Please ensure SavantCore.jar exists, or download a new copy of Savant");
            return;
        }

        locs[0] = StandardPluginLocation.create(coreLocation);

        pluginManager.publishPlugins(locs);

        PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
        ExtensionPoint corePt = (ExtensionPoint) (core.getExtensionPoints().toArray()[0]);
        coreExtPt = pluginManager.getRegistry().getExtensionPoint(core.getId(), corePt.getId());

        if (coreExtPt == null) {
            DialogUtils.displayError("Error initializing plugin loader. Please ensure SavantCore.jar exists, or download a new copy of Savant");
            return;
        }
    }

    public Collection<PluginDescriptor> getPluginDescriptors() {
        return pluginManager.getRegistry().getPluginDescriptors();
    }

    public Collection<Plugin> getPlugins() {
        return pluginIDToPluginMap.values();
    }

    public Collection<SavantDataSourcePlugin> getDataSourcePlugins() {
        Set<SavantDataSourcePlugin> result = new HashSet<SavantDataSourcePlugin>();
        for (Plugin p: pluginIDToPluginMap.values()) {
            if (p instanceof SavantDataSourcePlugin) {
                result.add((SavantDataSourcePlugin)p);
            }
        }
        return result;
    }

    public Plugin getPluginByID(String id) {
        return pluginIDToPluginMap.get(id);
    }

    /** INIT PLUGIN TYPES **/
    private void initPluginTool(PluginTool plugin) {
        plugin.init(new PluginAdapter());
        ToolsModule.addTool(plugin);
    }

    private void initGUIPlugin(SavantPanelPlugin plugin) {
        final DockableFrame f = DockableFrameFactory.createGUIPluginFrame(plugin.getTitle());
        JPanel p = (JPanel) f.getContentPane();
        p.setLayout(new BorderLayout());
        JPanel canvas = new JPanel();
        p.add(canvas, BorderLayout.CENTER);
        canvas.setLayout(new BorderLayout());
        plugin.init(canvas, new PluginAdapter());
        Savant.getInstance().getAuxDockingManager().addFrame(f);
        boolean isIntiallyVisible = true;
        MiscUtils.setFrameVisibility(f.getTitle(), isIntiallyVisible, Savant.getInstance().getAuxDockingManager());
        final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(plugin.getTitle());
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DockingManager m = Savant.getInstance().getAuxDockingManager();
                String frameKey = f.getTitle();
                boolean isVisible = m.getFrame(frameKey).isHidden();
                MiscUtils.setFrameVisibility(frameKey, isVisible, m);
                cb.setSelected(isVisible);
            }
        });
        cb.setSelected(!Savant.getInstance().getAuxDockingManager().getFrame(f.getTitle()).isHidden());
        // move this to a menu controller!
        Savant.getInstance().addPluginToMenu(cb);
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

    public boolean isPluginQueuedForDeletion(String id) {
        return this.pluginsToUnInstall.contains(id);
    }

    public String getPluginName(String id) {
        Plugin p = pluginIDToPluginMap.get(id);
        if (p instanceof SavantPanelPlugin) {
            SavantPanelPlugin pp = (SavantPanelPlugin) p;
            return pp.getTitle();
        } else if (p instanceof PluginTool) {
            PluginTool pp = (PluginTool) p;
            return pp.getToolInformation().getName();
        }
        return id;
    }

    public PluginDescriptor getPluginDescriptor(String id) {
        return pluginManager.getRegistry().getPluginDescriptor(id);
    }

    private void initSavantDataSourcePlugin(SavantDataSourcePlugin plugin) {
        DataSourcePluginController.getInstance().addDataSourcePlugin(plugin);
        plugin.init(new PluginAdapter());
    }

    private Set<PluginDescriptor> ignoredbadplugins = new HashSet<PluginDescriptor>();

    private void addToIgnoredBadPlugin(PluginDescriptor d) {
        ignoredbadplugins.add(d);
    }

    private boolean isIgnoredBadPlugin(PluginDescriptor d) {
        return ignoredbadplugins.contains(d);
    }


    Map<String, String[]> sdkToSavantVersionsMap;
    
    private boolean isSDKCompatibleWithThisVersion(String sdk) {

        if (sdkToSavantVersionsMap == null) {
            sdkToSavantVersionsMap = new HashMap<String, String[]>();

            // List of Savant versions compatible with given SDK version.
            // Savant 1.5 broke compatibility with earlier SDK versions.
            sdkToSavantVersionsMap.put("1.4.3", new String[] { "1.4.3", "1.4.4" });
            sdkToSavantVersionsMap.put("1.4.4", new String[] { "1.4.4" });
            sdkToSavantVersionsMap.put("1.5.0", new String[] { "1.5.0" });
        }

        String[] acceptableSavantVersions = sdkToSavantVersionsMap.get(sdk);

        if (acceptableSavantVersions != null && Arrays.asList(acceptableSavantVersions).contains(BrowserSettings.version)) {
            return true;
        }
        
        return false;
    }

    private void copyBuiltInPlugins() {
        File destDir = DirectorySettings.getPluginsDirectory();
        File srcDir = null;
        if (MiscUtils.MAC) {
            srcDir = new File(com.apple.eio.FileManager.getPathToApplicationBundle() + "/Contents/Plugins");
            if (srcDir.exists()) {
                try {
                    FileUtils.copyDir(srcDir, destDir);
                    return;
                } catch (Exception ignored) {
                    // We should expect to see this when running in the debugger.
                }
            }
        }
        try {
            srcDir = new File("plugins");
            FileUtils.copyDir(srcDir, destDir);
        } catch (Exception x) {
            LOG.error("Unable to copy builtin plugins from " + srcDir.getAbsolutePath() + " to " + destDir, x);
        }
    }

}
