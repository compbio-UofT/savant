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

import java.awt.Color;

/**
 *
 * @author AndrewBrook
 */
public class ColourSettings {


    //Nucleotides
    public static Color A_COLOR = new Color(27, 97, 97);  // blue
    public static Color C_COLOR = new Color(162, 45, 45); // red
    public static Color G_COLOR = new Color(36, 130, 36); // green
    public static Color T_COLOR = new Color(162,98, 45);  // yellow


    //BAM, BED
    public static Color forwardStrand = new Color(0,131,192);
    public static Color reverseStrand = new Color(0,174,255);
    public static Color invertedRead = Color.yellow;
    public static Color invertedMate = Color.magenta;
    public static Color evertedPair = Color.green;
    public static Color discordantLength = Color.blue;
    public static Color line = new Color(128,128,128);


    //Continuous
    public static Color continuousLine = new Color(0, 174, 255, 200);

    
    //Interval
    public static Color opaqueGraph = new Color(0,174,255);
    public static Color translucentGraph = new Color(0, 174, 255, 100);


    //Misc.
    public static Color colorSplitter = new Color(210,210,210);//Color.lightGray;
    public static Color colorGlassPaneBackground = Color.darkGray;
    public static Color colorGraphPaneBackgroundTop = Color.white;
    public static Color colorGraphPaneBackgroundBottom = new Color(210,210,210);
    public static Color colorGraphPaneZoomFill = new Color(0,0,255,100);
    public static Color colorGraphPaneSelectionFill = new Color(120,70,10,100);
    public static Color colorGraphPaneSelectionBorder = new Color(30,30,30);
    public static Color colorRangeSelectionTop = new Color(95, 161, 241);
    public static Color colorRangeSelectionBottom = new Color(75, 144, 228);
    public static Color colorFrameBackground = Color.lightGray;
    public static Color colorBrowseBackground = new Color(60,60,60,100);
    public static Color colorBrowseAuxiliaryBackground = new Color(160,160,160);
    public static Color colorTabBackground = new Color(240,240,240);
    public static Color colorToolsParameterMarginsBackground = Color.white; //new Color(230,230,230);
    public static Color colorToolsParametersBackground = Color.white;
    public static Color colorToolsListBackground = Color.white; //new Color(255,255,255); //new Color(245,250,255);
    public static Color colorToolsBackground = Color.white; //new Color(200,200,200);   
    public static Color colorAccent = Color.black;
    public static Color colorAxisGrid = Color.lightGray;    
    public static Color colorGraphMain = new Color(0, 174, 255, 150);
    //public static Color colorFrameBackground = Color.darkGray;
    //public static Color colorBrowseBackground = new Color(60,60,60);
    //public static Color colorBrowseBackground = /*new Color(0, 174, 255, 150);*/ new Color(171, 207, 59);
    //public static Color colorGraphMain = Color.red;
    //public static Color ColorMain = new Color(0, 174, 255, 150);

}
