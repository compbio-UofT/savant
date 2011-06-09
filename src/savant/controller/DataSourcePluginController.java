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

import java.util.ArrayList;
import java.util.List;
import savant.plugin.SavantDataSourcePlugin;
import savant.plugin.builtin.SavantFileRepositoryDataSourcePlugin;

/**
 * Keeps track of DataSource plugins.
 * TODO: Does this need to be a separate class, or can it be subsumed by PluginController.
 *
 * @author mfiume
 */
public class DataSourcePluginController {

    private static DataSourcePluginController instance;
    
    private List<SavantDataSourcePlugin> plugins = new ArrayList<SavantDataSourcePlugin>();

    public static DataSourcePluginController getInstance() {
        if (instance == null) {
            instance = new DataSourcePluginController();
        }
        return instance;
    }

    public boolean hasOnlySavantRepoDataSource() {
        return plugins.size() == 1 && plugins.get(0) instanceof SavantFileRepositoryDataSourcePlugin;
    }

    public List<SavantDataSourcePlugin> getPlugins() {
        return plugins;
    }

    public void addDataSourcePlugin(SavantDataSourcePlugin p) {
        plugins.add(p);
    }
}
