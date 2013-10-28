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
package savant.view.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBoxMenuItem;

import savant.api.util.Listener;
import savant.api.event.PluginEvent;
import savant.plugin.PluginController;
import savant.plugin.SavantPanelPlugin;


/**
 * Menu-item which controls the visibility of a Savant GUI plugin.
 *
 * @author tarkvara
 */
public class PluginMenuItem extends JCheckBoxMenuItem {
    private final SavantPanelPlugin plugin;

    /**
     * Create a menu-item which will show/hide the given plugin.
     */
    public PluginMenuItem(SavantPanelPlugin p) {
        super(p.getTitle());
        plugin = p;
        setSelected(true);

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                plugin.setVisible(isSelected());
            }
        });

        PluginController.getInstance().addListener(new Listener<PluginEvent>() {
            @Override
            public void handleEvent(PluginEvent event) {
                if (event.getType() == PluginEvent.Type.QUEUED_FOR_REMOVAL && event.getPlugin() == plugin) {
                    // Our plugin is being uninstalled.  Hide the plugin and disable the menu-item.
                    plugin.setVisible(false);
                    setSelected(false);
                    setEnabled(false);
                }
            }
        });
    }
}
