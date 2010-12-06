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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import java.net.URI;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginManager;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;
import savant.api.util.DialogUtils;

import savant.experimental.PluginTool;
import savant.plugin.SavantPanelPlugin;
import savant.plugin.PluginAdapter;
import savant.plugin.SavantDataSourcePlugin;
import savant.settings.DirectorySettings;
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
    private final String FILENAME = ".uninstall_plugins";
    private Set<String> pluginsToUnInstall = new HashSet<String>();
    /** VARIABLES **/
    private static PluginController instance;
    private static PluginManager pluginManager;
    //private Map<SuperPluginDescriptor,Plugin> pluginMap = new HashMap<SuperPluginDescriptor,Plugin>();
    private Map<String, PluginDescriptor> pluginIDToDescriptorMap = new HashMap<String, PluginDescriptor>();
    private Map<String, Extension> pluginIDToExtensionMap = new HashMap<String, Extension>();
    private Map<String, Plugin> pluginIDToPluginMap = new HashMap<String, Plugin>();
    //private Map<String,String> pluginIdToPathMap = new HashMap<String,String>();
    private ExtensionPoint coreExtPt;
    private final String PLUGINS_DIR = "plugins";
    File coreLocation = new File(PLUGINS_DIR + System.getProperty("file.separator") + "SavantCore.jar");


    /** SINGLETON **/
    public static synchronized PluginController getInstance() {
        if (instance == null) {
            instance = new PluginController();
        }
        return instance;
    }
    private File uninstallFile;

    /** CONSTRUCTOR **/
    public PluginController() {
        try {

            pluginManager = ObjectFactory.newInstance().createManager();
            uninstallFile = new File(DirectorySettings.getSavantDirectory(), FILENAME);
            if (uninstallFile.exists()) {
                deleteFileList(uninstallFile);
            }
            loadCorePlugin();
            loadPlugins(new File(PLUGINS_DIR));
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
                int result = DialogUtils.askYesNo("Unable to load plugin at " + pluginLocation.getAbsolutePath() + ". Please check if it is compatible with this version of Savant. Uninstall it?");
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
            pluginManager.activatePlugin(desc.getId());
            ClassLoader classLoader = pluginManager.getPluginClassLoader(desc);

            Plugin plugininstance = (Plugin) (classLoader.loadClass(ext.getParameter("class").valueAsString())).newInstance();

            // init the plugin based on its type
            if (plugininstance instanceof SavantPanelPlugin) {
                initGUIPlugin(plugininstance);
            } else if (plugininstance instanceof PluginTool) {
                initPluginTool(plugininstance);
            } else if (plugininstance instanceof SavantDataSourcePlugin) {
                initSavantDataSourcePlugin(plugininstance);
            }

            addToPluginMaps(desc.getUniqueId(), desc, ext, plugininstance);

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
            if (!pluginIDToExtensionMap.containsValue(ext)
                    && !this.isPluginQueuedForDeletion(ext.getDeclaringPluginDescriptor().getId())
                    && !this.isIgnoredBadPlugin(ext.getDeclaringPluginDescriptor())
                    ) {

                try {
                    LOG.info("Activating new plugin: " + ext);
                    activatePlugin(ext.getDeclaringPluginDescriptor(), ext);
                    oneLoaded = true;
                } catch (java.lang.Error e) {
                    this.addToIgnoredBadPlugin(ext.getDeclaringPluginDescriptor());
                    error = true;
                } catch (Exception e) {
                    this.addToIgnoredBadPlugin(ext.getDeclaringPluginDescriptor());
                    error = true;
                }
            }
        }

        return oneLoaded && !error;
    }

    /** ADD AND REMOVE PLUGINS **/
    public void installPlugin(String path) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        loadPlugin(new File(path));
    }

    private void addToPluginMaps(String id, PluginDescriptor d, Extension e, Plugin p) {
        this.pluginIDToDescriptorMap.put(id, d);
        this.pluginIDToExtensionMap.put(id, e);
        this.pluginIDToPluginMap.put(id, p);
    }

    private void removeFromPluginMaps(String id) {
        this.pluginIDToDescriptorMap.remove(id);
        this.pluginIDToExtensionMap.remove(id);
        this.pluginIDToPluginMap.remove(id);
    }

    /*
    public String getPluginPath(String id) {
        System.out.println("Getting path for " + id);
        String rawLocation = this.pluginIDToDescriptorMap.get(id).getLocation().getPath();
        return getFilePathOfPlugin(rawLocation);
    }
     * 
     */

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
            return;
        }

        locs[0] = StandardPluginLocation.create(coreLocation);

        pluginManager.publishPlugins(locs);

        PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
        ExtensionPoint corePt = (ExtensionPoint) (core.getExtensionPoints().toArray()[0]);
        coreExtPt = pluginManager.getRegistry().getExtensionPoint(core.getId(), corePt.getId());
    }

    /** GETTERS **/
    public List<PluginDescriptor> getPluginDescriptors() {
        return new ArrayList<PluginDescriptor>(this.pluginIDToDescriptorMap.values());
    }

    public List<Extension> getExtensions() {
        return new ArrayList<Extension>(this.pluginIDToExtensionMap.values());
    }

    public List<Plugin> getPlugins() {
        return new ArrayList<Plugin>(this.pluginIDToPluginMap.values());
    }

    /** INIT PLUGIN TYPES **/
    private void initPluginTool(Object plugininstance) {
        PluginTool p = (PluginTool) plugininstance;
        p.init(new PluginAdapter());
        ToolsModule.addTool(p);
    }

    private void initGUIPlugin(Object plugininstance) {
        SavantPanelPlugin plugin = (SavantPanelPlugin) plugininstance;
        final DockableFrame f = DockableFrameFactory.createGUIPluginFrame(plugin.getTitle());
        JPanel p = (JPanel) f.getContentPane();
        p.setLayout(new BorderLayout());
        JPanel canvas = new JPanel();
        p.add(canvas, BorderLayout.CENTER);
        canvas.setLayout(new BorderLayout());
        plugin.init(canvas, new PluginAdapter());
        Savant.getInstance().getAuxDockingManager().addFrame(f);
        boolean isIntiallyVisible = false;
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
        Plugin p = this.pluginIDToPluginMap.get(id);
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
        return this.pluginIDToDescriptorMap.get(id);
    }

    private void initSavantDataSourcePlugin(Plugin plugininstance) {
        SavantDataSourcePlugin plugin = (SavantDataSourcePlugin) plugininstance;
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

}
