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
package savant.view.swing;

import java.awt.Color;
import javax.swing.UIManager;

/**
 *
 * @author mfiume
 */
public class BrowserDefaults {

    /*
     * Website
     */
    public static String url = "http://compbio.cs.toronto.edu/savant";
    public static String url_preformatteddata = "http://www.cs.toronto.edu/~mfiume/projects/savant/data.html";
    public static String url_ucsctablebrowser = "http://genome.ucsc.edu/cgi-bin/hgTables?command=start";
    public static String url_thousandgenomes = "http://www.1000genomes.org/page.php?page=data";

    /**
     * Look and Feel
     */
    public static String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
    //public static Class LookAndFeelAddon = MetalLookAndFeelAddons.class;
    /**
     * Colors
     */

    public static Color colorGlassPaneBackground = Color.darkGray;
    public static Color colorGraphPaneBackgroundTop = Color.white;
    public static Color colorGraphPaneBackgroundBottom = new Color(210,210,210);
    public static Color colorGraphPaneSelectionFill = new Color(150,150,150,100);
    public static Color colorGraphPaneSelectionBorder = new Color(30,30,30);


    //public static Color colorFrameBackground = Color.darkGray;
    //public static Color colorBrowseBackground = new Color(60,60,60);
    public static Color colorFrameBackground = Color.lightGray;
    public static Color colorBrowseBackground = new Color(60,60,60,100);
//    public static Color colorBrowseBackground = /*new Color(0, 174, 255, 150);*/ new Color(171, 207, 59);
    public static Color colorBrowseAuxiliaryBackground = new Color(160,160,160);
    public static Color colorTabBackground = new Color(240,240,240);
    public static Color colorFormatBackground = new Color(123, 185, 233);
    //public static Color ColorMain = new Color(0, 174, 255, 150);
    public static Color colorAccent = Color.black;
    public static Color colorAxisGrid = Color.lightGray;
    public static Color colorGraphMain = Color.red;

    public static Color A_COLOR = new Color(27, 97, 97);  // blue
    public static Color C_COLOR = new Color(162, 45, 45); // red
    public static Color G_COLOR = new Color(36, 130, 36); // green
    public static Color T_COLOR = new Color(162,98, 45);  // yellow

    /**
     * padding
     */
    public static int padding = 10;

    /**
     * Zooming
     */
    public static int zoomAmount = 2;

    /**
     * Fonts
     */
    public static String fontName = "Verdana";
}
