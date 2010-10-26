/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.controller;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockableFrameFactory;
import com.jidesoft.docking.DockingManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;
import savant.plugin.GUIPlugin;
import savant.plugin.PluginAdapter;
import savant.plugin.PluginTool;
import savant.view.tools.ToolsModule;

/**
 *
 * @author mfiume
 */
public class PluginController {

    private static PluginController instance;
    private static PluginManager pluginManager;
    private List<Plugin> plugins;

    public static synchronized PluginController getInstance() {
        if (instance == null) {
            instance = new PluginController();
        }
        return instance;
    }

    /*
    public static void loadPlugin(File pluginLocation) throws JpfException, MalformedURLException {
        PluginManager.PluginLocation[] locs = new PluginManager.PluginLocation[1];
        locs[0] = StandardPluginLocation.create(pluginLocation);
        pluginManager.publishPlugins(locs);
        pluginManager.getRegistry().getExtensionPoint(null)
    }

    public static void loadPlugins(File pluginsDir) throws MalformedURLException, JpfException {
        pluginManager = ObjectFactory.newInstance().createManager();

        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".gz");
            }
        });
        for (int i = 0; i < plugins.length; i++) {
            loadPlugin(plugins[i]);
        }
    }

    private static void initPlugins() {
        PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
        ExtensionPoint point = pluginManager.getRegistry().getExtensionPoint(core.getId(), "AuxData");

        Iterator it = point.getConnectedExtensions().iterator();
        while (it.hasNext()) {
            try {
                Extension ext = (Extension) it.next();
                PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
                pluginManager.activatePlugin(descr.getId());
                ClassLoader classLoader = pluginManager.getPluginClassLoader(descr);
                Class pluginCls = classLoader.loadClass(ext.getParameter("class").valueAsString());
                Object plugininstance = pluginCls.newInstance();
                if (plugininstance instanceof GUIPlugin) {
                    GUIPlugin plugin = (GUIPlugin) plugininstance;
                    final DockableFrame f = DockableFrameFactory.createGUIPluginFrame(plugin.getTitle());
                    JPanel p = (JPanel) f.getContentPane();
                    p.setLayout(new BorderLayout());
                    JPanel canvas = new JPanel();
                    p.add(canvas, BorderLayout.CENTER);
                    canvas.setLayout(new BorderLayout());
                    plugin.init(canvas, new PluginAdapter());
                    this.getAuxDockingManager().addFrame(f);
                    boolean isIntiallyVisible = false;
                    setFrameVisibility(f.getTitle(), isIntiallyVisible, auxDockingManager);
                    final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(plugin.getTitle());
                    cb.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            DockingManager m = auxDockingManager;
                            String frameKey = f.getTitle();
                            boolean isVisible = m.getFrame(frameKey).isHidden();
                            setFrameVisibility(frameKey, isVisible, m);
                            cb.setSelected(isVisible);
                        }
                    });
                    cb.setSelected(!auxDockingManager.getFrame(f.getTitle()).isHidden());
                    menu_window.add(cb);
                } else if (plugininstance instanceof PluginTool) {
                    PluginTool p = (PluginTool) plugininstance;
                    p.init(new PluginAdapter());
                    ToolsModule.addTool(p);
                }
            } catch (PluginLifecycleException ex) {
                Logger.getLogger(PluginController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(PluginController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(PluginController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(PluginController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
     */
}
