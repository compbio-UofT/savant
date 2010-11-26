/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin;

import org.java.plugin.Plugin;
import savant.data.sources.DataSource;

/**
 *
 * @author mfiume
 */
public abstract class SavantDataSourcePlugin extends Plugin {

    /**
     * This method is called once during application life cycle to allow a third-party
     * plugin to initialize and show itself.
     *
     * @param panel parent panel for auxiliary data components
     * @param adapter gives access to functionality provided by Savant
     */
    public abstract void init(PluginAdapter pluginAdapter);

    /**
     * @return title to be used in Plugins menu and for frame in which plugin is rendered
     */
    public abstract String getTitle();


    public abstract DataSource getDataSource();
}
