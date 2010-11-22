/*
 *    Copyright 2009-2010 University of Toronto
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

package savant.util;

import java.awt.*;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 *
 * @author mfiume
 */
public class ColorScheme {

    private Color defaultColor = Color.PINK;
    private Dictionary<String, Color> colorSettings;

    public ColorScheme()
    {
        colorSettings = new Hashtable<String, Color>();
    }

    public void addColorSetting(String name, Color color)
    {
        colorSettings.put(name, color);
    }

    public Color getColor(String colorSettingName)
    {
        if (colorSettings.get(colorSettingName) != null)
        {
            return colorSettings.get(colorSettingName);
        }
        else
        {
            return defaultColor;
        }
    }

    public void setColorSettings(Dictionary<String, Color> cs) { colorSettings = cs; }
    public Dictionary<String, Color> getColorSettings() { return this.colorSettings; }
}
