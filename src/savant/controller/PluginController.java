package savant.controller;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

    private static PluginController instance;
    private static PluginManager pluginManager;
    //private Set<SuperPluginDescriptor> plugins = new HashSet<SuperPluginDescriptor>();
    private Map<SuperPluginDescriptor,Plugin> pluginMap = new HashMap<SuperPluginDescriptor,Plugin>();

    //private List<Plugin> plugins;
    private ExtensionPoint coreExtPt;
    private final String PLUGINS_DIR = "plugins";

    public static synchronized PluginController getInstance() {
        if (instance == null) {
            instance = new PluginController();
        }
        return instance;
    }

    public PluginController() {
        try {
            pluginManager = ObjectFactory.newInstance().createManager();
            loadCorePlugin();
            loadPlugins(new File(PLUGINS_DIR));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadPlugins(File pluginsDir) throws MalformedURLException, JpfException, InstantiationException, IllegalAccessException {
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".gz");
            }
        });
        for (int i = 0; i < plugins.length; i++) {
            loadPlugin(plugins[i]);
        }
    }

    public void loadPlugin(File pluginLocation) throws JpfException, MalformedURLException, InstantiationException, IllegalAccessException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        locs[0] = StandardPluginLocation.create(pluginLocation);
        pluginManager.publishPlugins(locs);
        Set<SuperPluginDescriptor> spds = getInactivePluginInfo();
        activatePlugins(spds);
    }

    private class SuperPluginDescriptor {

        private PluginDescriptor pd;
        private Extension e;

        public SuperPluginDescriptor(PluginDescriptor pd, Extension e) {
            this.pd = pd;
            this.e = e;
        }

        public PluginDescriptor getPD() {
            return pd;
        }

        public Extension getExtension() {
            return e;
        }
    }

    private Set<SuperPluginDescriptor> getInactivePluginInfo() {
        Set<SuperPluginDescriptor> result = new HashSet<SuperPluginDescriptor>();
        Iterator it = coreExtPt.getConnectedExtensions().iterator();
        while (it.hasNext()) {
            Extension ext = (Extension) it.next();
            PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
            SuperPluginDescriptor s = new SuperPluginDescriptor(descr, ext);
            if (!pluginMap.containsKey(s)) {
                result.add(s);
            }
        }
        return result;
    }

    public void activatePlugins(Set<SuperPluginDescriptor> spds) {

        for (SuperPluginDescriptor spd : spds) {
            try {
                pluginManager.activatePlugin(spd.getPD().getId());
                ClassLoader classLoader = pluginManager.getPluginClassLoader(spd.getPD());

                Plugin plugininstance = (Plugin) (
                        classLoader.loadClass(
                            spd.getExtension().getParameter("class").valueAsString()
                        )).newInstance();

                // init the plugin based on its type
                if (plugininstance instanceof GUIPlugin) {
                    initGUIPlugin(plugininstance);
                } else if (plugininstance instanceof PluginTool) {
                    initPluginTool(plugininstance);
                }

                pluginMap.put(spd, (Plugin) plugininstance);
            } catch (Exception ex) {}
        }
    }

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

    private void loadCorePlugin() throws MalformedURLException, JpfException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        java.net.URL[] mans = new java.net.URL[1];
        locs[0] = StandardPluginLocation.create(new File(PLUGINS_DIR + System.getProperty("file.separator") + "SavantCore.jar"));

        pluginManager.publishPlugins(locs);

        PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
        ExtensionPoint corePt = (ExtensionPoint) (core.getExtensionPoints().toArray()[0]);
        coreExtPt = pluginManager.getRegistry().getExtensionPoint(core.getId(), corePt.getId());
    }
}
