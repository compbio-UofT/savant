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
package savant.settings;

import javax.swing.*;

/**
 *
 * @author mfiume
 */
public class BrowserSettings {

    /*
     * Website
     */
    public static String url = "http://compbio.cs.toronto.edu/savant";
    public static String url_version = "http://compbio.cs.toronto.edu/savant/serve/version/version.xml";
    public static String url_tutorials = "http://compbio.cs.toronto.edu/savant/media.html";
    public static String url_manuals = "http://compbio.cs.toronto.edu/savant/documentation.html";
    public static String url_preformatteddata = "http://compbio.cs.toronto.edu/savant/data.html";
    public static String url_ucsctablebrowser = "http://genome.ucsc.edu/cgi-bin/hgTables?command=start";
    public static String url_thousandgenomes = "http://www.1000genomes.org/page.php?page=data";

    public static String version = "1.3.2";

    /**
     * Look and Feel
     */
    public static String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
    //public static Class LookAndFeelAddon = MetalLookAndFeelAddons.class;

    /**
     * padding
     */
    public static int padding = 7;

    /**
     * Zooming
     */
    public static int zoomAmount = 2;

    /**
     * Fonts
     */
    public static String fontName = "Verdana";

    
}
