/*
 *    Copyright 2011 University of Toronto
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
package savant.view.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBoxMenuItem;

import savant.controller.PluginController;
import savant.controller.event.PluginEvent;
import savant.plugin.SavantPanelPlugin;
import savant.util.Listener;


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
