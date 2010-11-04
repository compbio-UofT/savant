package savant.controller;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginManager;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;
import savant.view.swing.DockableFrameFactory;
import savant.plugin.GUIPlugin;
import savant.plugin.PluginAdapter;
import savant.plugin.PluginTool;
import savant.util.MiscUtils;
import savant.view.swing.Savant;
import savant.view.tools.ToolsModule;

/**
 *
 * @author mfiume
 */
public class PluginController {

    /** VARIABLES **/

    private static PluginController instance;
    private static PluginManager pluginManager;

    //private Map<SuperPluginDescriptor,Plugin> pluginMap = new HashMap<SuperPluginDescriptor,Plugin>();

    private Map<String,PluginDescriptor> pluginIdToDescriptorMap = new HashMap<String,PluginDescriptor>();
    private Map<String,Extension> pluginIdToExtensionMap = new HashMap<String,Extension>();
    private Map<String,Plugin> pluginIdToPluginMap = new HashMap<String,Plugin>();

    private ExtensionPoint coreExtPt;
    private final String PLUGINS_DIR = "plugins";

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
            loadCorePlugin();
            loadPlugins(new File(PLUGINS_DIR));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** PLUGIN LOADING **/

    private void loadPlugins(File pluginsDir) throws MalformedURLException, JpfException, InstantiationException, IllegalAccessException {
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".gz");
            }
        });
        for (int i = 0; i < plugins.length; i++) {
            loadPlugin(plugins[i]);
        }
    }

    private void loadPlugin(File pluginLocation) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        locs[0] = StandardPluginLocation.create(pluginLocation);
        pluginManager.publishPlugins(locs);
        activateNewPlugins();
    }

    /** PLUGIN ACTIVATION **/

    private void deactivatePlugin(String id) {
        pluginManager.deactivatePlugin(id);
    }

    public boolean uninstallPlugin(String id) {
        deactivatePlugin(id);
        String s = this.pluginIdToDescriptorMap.get(id).getLocation().getPath();
        s = s.replaceFirst("file:/", "");
        s = s.substring(0,s.lastIndexOf("!"));
        //System.out.println("uninstalling: " + s);
        boolean result = (new File(s)).delete();
        if (!result) {
            (new File(s)).deleteOnExit();
        }
        //System.out.println("Deleted? " + result);
        return result;
    }

    private boolean activatePlugin(PluginDescriptor d, Extension e) {

            try {
                pluginManager.activatePlugin(d.getId());
                ClassLoader classLoader = pluginManager.getPluginClassLoader(d);

                Plugin plugininstance = (Plugin) (
                        classLoader.loadClass(
                            e.getParameter("class").valueAsString()
                        )).newInstance();

                // init the plugin based on its type
                if (plugininstance instanceof GUIPlugin) {
                    initGUIPlugin(plugininstance);
                } else if (plugininstance instanceof PluginTool) {
                    initPluginTool(plugininstance);
                }

                addToPluginMaps(d.getUniqueId(), d, e, plugininstance);

                return true;

            } catch (Exception ex) {}

            return false;
        }

    /** ACTIVATE NEW **/
        private void activateNewPlugins() {
        Iterator it = coreExtPt.getConnectedExtensions().iterator();
        while (it.hasNext()) {
            Extension ext = (Extension) it.next();
            PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
            this.activatePlugin(descr, ext);
        }
    }

    /** ADD AND REMOVE PLUGINS **/

    public void installPlugin(String path) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        loadPlugin(new File(path));
    }

    private void addToPluginMaps(String id, PluginDescriptor d, Extension e, Plugin p) {
        this.pluginIdToDescriptorMap.put(id, d);
        this.pluginIdToExtensionMap.put(id, e);
        this.pluginIdToPluginMap.put(id, p);
    }

    private void removeFromPluginMaps(String id) {
        this.pluginIdToDescriptorMap.remove(id);
        this.pluginIdToExtensionMap.remove(id);
        this.pluginIdToPluginMap.remove(id);
    }

    /** CORE PLUGIN **/

    private void loadCorePlugin() throws MalformedURLException, JpfException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        java.net.URL[] mans = new java.net.URL[1];

        File location = new File(PLUGINS_DIR + System.getProperty("file.separator") + "SavantCore.jar");

        if (!location.exists()) {
            System.err.println("Loading of core plugin failed.");
            return;
        }

        locs[0] = StandardPluginLocation.create(location);

        pluginManager.publishPlugins(locs);

        PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
        ExtensionPoint corePt = (ExtensionPoint) (core.getExtensionPoints().toArray()[0]);
        coreExtPt = pluginManager.getRegistry().getExtensionPoint(core.getId(), corePt.getId());
    }


    /** GETTERS **/
    public List<PluginDescriptor> getPluginDescriptors() {
        return new ArrayList<PluginDescriptor>(this.pluginIdToDescriptorMap.values());
    }

    public List<Extension> getExtensions() {
        return new ArrayList<Extension>(this.pluginIdToExtensionMap.values());
    }

    public List<Plugin> getPlugins() {
        return new ArrayList<Plugin>(this.pluginIdToPluginMap.values());
    }

    /** INIT PLUGIN TYPES **/

    private void initPluginTool(Object plugininstance) {
        PluginTool p = (PluginTool) plugininstance;
        p.init(new PluginAdapter());
        ToolsModule.addTool(p);
    }

    private void initGUIPlugin(Object plugininstance) {
        GUIPlugin plugin = (GUIPlugin) plugininstance;
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
}
