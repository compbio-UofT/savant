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
 * Class which keeps track of global colour-scheme settings.
 *
 * @author AndrewBrook, tarkvara
 */
public class ColourSettings {
    private static PersistentSettings settings = PersistentSettings.getInstance();

    private static final String A_KEY = "A";
    private static final String C_KEY = "C";
    private static final String G_KEY = "G";
    private static final String T_KEY = "T";
    private static final String FORWARD_STRAND_KEY = "ForwardStrand";
    private static final String REVERSE_STRAND_KEY = "ReverseStrand";
    private static final String INVERTED_READ_KEY = "InvertedRead";
    private static final String INVERTED_MATE_KEY = "InvertedMate";
    private static final String EVERTED_PAIR_KEY = "EvertedPair";
    private static final String DISCORDANT_LENGTH_KEY = "DiscordantLength";
    private static final String LINE_KEY = "Line";
    private static final String CONTINUOUS_LINE_KEY = "ContinuousLine";
    private static final String OPAQUE_GRAPH_KEY = "OpaqueGraph";
    private static final String TRANSLUCENT_GRAPH_KEY = "TranslucentGraph";
    private static final String SPLITTER_KEY = "Splitter";
    private static final String GLASS_PANE_BACKGROUND_KEY = "GlassPaneBackground";
    private static final String GRAPH_PANE_BACKGROUND_TOP_KEY = "GraphPaneBackgroundTop";
    private static final String GRAPH_PANE_BACKGROUND_BOTTOM_KEY = "GraphPaneBackgroundBottom";
    private static final String GRAPH_PANE_ZOOM_FILL_KEY = "GraphPaneZoomFill";
    private static final String GRAPH_PANE_SELECTION_FILL_KEY = "GraphPaneSelectionFill";
    private static final String GRAPH_PANE_SELECTION_BORDER_KEY = "GraphPaneSelectionBorder";
    private static final String RANGE_SELECTION_TOP_KEY = "RangeSelectionTop";
    private static final String RANGE_SELECTION_BOTTOM_KEY = "RangeSelectionBottom";
    private static final String FRAME_BACKGROUND_KEY = "FrameBackground";
    private static final String BROWSE_BACKGROUND_KEY = "BrowseBackground";
    private static final String BROWSE_AUXILIARY_BACKGROUND_KEY = "BrowseAuxiliaryBackground";
    private static final String TAB_BACKGROUND_KEY = "TabBackground";
    private static final String TOOLS_MARGIN_BACKGROUND_KEY = "ToolsMarginBackground";
    private static final String TOOLS_BACKGROUND_KEY = "ToolsBackground";
    private static final String POINT_LINE_KEY = "PointLine";
    private static final String AXIS_GRID_KEY = "AxisGrid";
    private static final String POINT_FILL_KEY = "PointFill";

    /**
     * @return the colour for A bases
     */
    public static Color getA() {
        return settings.getColour(A_KEY, new Color(27, 97, 97));
    }

    /**
     * @param value the colour for A bases
     */
    public static void setA(Color value) {
        settings.setColour(A_KEY, value);
    }

    /**
     * @return the colour for C bases
     */
    public static Color getC() {
        return settings.getColour(C_KEY, new Color(162, 45, 45));
    }

    /**
     * @param value the colour for C bases
     */
    public static void setC(Color value) {
        settings.setColour(C_KEY, value);
    }

    /**
     * @return the colour for G bases
     */
    public static Color getG() {
        return settings.getColour(G_KEY, new Color(36, 130, 36));
    }

    /**
     * @param value the colour for G bases
     */
    public static void setG(Color value) {
        settings.setColour(G_KEY, value);
    }

    /**
     * @return the colour for T bases
     */
    public static Color getT() {
        return settings.getColour(T_KEY, new Color(162, 98, 45));
    }

    /**
     * @param value the colour for T bases
     */
    public static void setT(Color value) {
        settings.setColour(T_KEY, value);
    }

    /**
     * @return the colour for forward strands
     */
    public static Color getForwardStrand() {
        return settings.getColour(FORWARD_STRAND_KEY, new Color(0, 131, 192));
    }

    /**
     * @param value the colour for forward strands
     */
    public static void setForwardStrand(Color value) {
        settings.setColour(FORWARD_STRAND_KEY, value);
    }

    /**
     * @return the colour for reverse strands
     */
    public static Color getReverseStrand() {
        return settings.getColour(REVERSE_STRAND_KEY, new Color(0, 174, 255));
    }

    /**
     * @param value the colour for reverse strands
     */
    public static void setReverseStrand(Color value) {
        settings.setColour(REVERSE_STRAND_KEY, value);
    }

    /**
     * @return the colour for reverse strands
     */
    public static Color getInvertedRead() {
        return settings.getColour(INVERTED_READ_KEY, Color.yellow);
    }

    /**
     * @param value the colour for inverted reads
     */
    public static void setInvertedRead(Color value) {
        settings.setColour(INVERTED_READ_KEY, value);
    }

    /**
     * @return the colour for inverted mate pairs
     */
    public static Color getInvertedMate() {
        return settings.getColour(INVERTED_MATE_KEY, Color.magenta);
    }

    /**
     * @param value the colour for inverted mate pairs
     */
    public static void setInvertedMate(Color value) {
        settings.setColour(INVERTED_MATE_KEY, value);
    }

    /**
     * @return the colour for everted pairs
     */
    public static Color getEvertedPair() {
        return settings.getColour(EVERTED_PAIR_KEY, Color.green);
    }

    /**
     * @param value the colour for everted pairs
     */
    public static void setEvertedPair(Color value) {
        settings.setColour(EVERTED_PAIR_KEY, value);
    }

    /**
     * @return the colour for discordant lengths
     */
    public static Color getDiscordantLength() {
        return settings.getColour(DISCORDANT_LENGTH_KEY, Color.blue);
    }

    /**
     * @param value the colour for discordant lengths
     */
    public static void setDiscordantLength(Color value) {
        settings.setColour(DISCORDANT_LENGTH_KEY, value);
    }

    /**
     * @return the colour for ordinary lines
     */
    public static Color getLine() {
        return settings.getColour(LINE_KEY, new Color(128, 128, 128));
    }

    /**
     * @param value the colour for ordinary lines
     */
    public static void setLine(Color value) {
        settings.setColour(LINE_KEY, value);
    }

    /**
     * @return the colour for continuous lines
     */
    public static Color getContinuousLine() {
        return settings.getColour(CONTINUOUS_LINE_KEY, new Color(0, 174, 255, 200));
    }

    /**
     * @param value the colour for continuous lines
     */
    public static void setContinuousLine(Color value) {
        settings.setColour(CONTINUOUS_LINE_KEY, value);
    }

    /**
     * @return the colour for opaque graphs
     */
    public static Color getOpaqueGraph() {
        return settings.getColour(OPAQUE_GRAPH_KEY, new Color(0, 174, 255));
    }

    /**
     * @param value the colour for opaque graphs
     */
    public static void setOpaqueGraph(Color value) {
        settings.setColour(OPAQUE_GRAPH_KEY, value);
    }

    /**
     * @return the colour for translucent graphs
     */
    public static Color getTranslucentGraph() {
        return settings.getColour(TRANSLUCENT_GRAPH_KEY, new Color(0, 174, 255, 100));
    }

    /**
     * @param value the colour for translucent graphs
     */
    public static void setTranslucentGraph(Color value) {
        settings.setColour(TRANSLUCENT_GRAPH_KEY, value);
    }

    /**
     * @return the colour for splitters
     */
    public static Color getSplitter() {
        return settings.getColour(SPLITTER_KEY, new Color(210, 210, 210));
    }

    /**
     * @param value the colour for splitters
     */
    public static void setSplitter(Color value) {
        settings.setColour(SPLITTER_KEY, value);
    }

    /**
     * @return the background colour for the glass pane
     */
    public static Color getGlassPaneBackground() {
        return settings.getColour(GLASS_PANE_BACKGROUND_KEY, Color.darkGray);
    }

    /**
     * @param value the background colour for the glass pane
     */
    public static void setGlassPaneBackground(Color value) {
        settings.setColour(GLASS_PANE_BACKGROUND_KEY, value);
    }

    /**
     * @return the background colour for the graph pane top
     */
    public static Color getGraphPaneBackgroundTop() {
        return settings.getColour(GRAPH_PANE_BACKGROUND_TOP_KEY, Color.white);
    }

    /**
     * @param value the background colour for the graph pane top
     */
    public static void setGraphPaneBackgroundTop(Color value) {
        settings.setColour(GRAPH_PANE_BACKGROUND_TOP_KEY, value);
    }

    /**
     * @return the background colour for the graph pane bottom
     */
    public static Color getGraphPaneBackgroundBottom() {
        return settings.getColour(GRAPH_PANE_BACKGROUND_BOTTOM_KEY, new Color(210, 210, 210));
    }

    /**
     * @param value the background colour for the graph pane bottom
     */
    public static void setGraphPaneBackgroundBottom(Color value) {
        settings.setColour(GRAPH_PANE_BACKGROUND_BOTTOM_KEY, value);
    }

    /**
     * @return the fill colour for graph pane zooms (whatever those are)
     */
    public static Color getGraphPaneZoomFill() {
        return settings.getColour(GRAPH_PANE_ZOOM_FILL_KEY, new Color(0, 0, 255, 100));
    }

    /**
     * @param value the fill colour for graph pane zooms (whatever those are)
     */
    public static void setGraphPaneZoomFill(Color value) {
        settings.setColour(GRAPH_PANE_ZOOM_FILL_KEY, value);
    }

    /**
     * @return the fill colour for graph pane selections
     */
    public static Color getGraphPaneSelectionFill() {
        return settings.getColour(GRAPH_PANE_SELECTION_FILL_KEY, new Color(120, 70, 10, 100));
    }

    /**
     * @param value the fill colour for graph pane selections
     */
    public static void setGraphPaneSelectionFill(Color value) {
        settings.setColour(GRAPH_PANE_SELECTION_FILL_KEY, value);
    }

    /**
     * @return the background colour for frames
     */
    public static Color getFrameBackground() {
        return settings.getColour(FRAME_BACKGROUND_KEY, Color.lightGray);
    }

    /**
     * @param value the background colour for frames
     */
    public static void setFrameBackground(Color value) {
        settings.setColour(FRAME_BACKGROUND_KEY, value);
    }

    /**
     * @return the background colour for tabs
     */
    public static Color getTabBackground() {
        return settings.getColour(TAB_BACKGROUND_KEY, new Color(240, 240, 240));
    }

    /**
     * @param value the background colour for tabs
     */
    public static void setTabBackground(Color value) {
        settings.setColour(TAB_BACKGROUND_KEY, value);
    }

    /**
     * @return the background colour for tools margins
     */
    public static Color getToolsMarginBackground() {
        return settings.getColour(TOOLS_MARGIN_BACKGROUND_KEY, new Color(236, 236, 236));
    }

    /**
     * @param value the background colour for tools margins
     */
    public static void setToolsMarginBackground(Color value) {
        settings.setColour(TOOLS_MARGIN_BACKGROUND_KEY, value);
    }

    /**
     * @return the background colour for tools
     */
    public static Color getToolsBackground() {
        return settings.getColour(TOOLS_BACKGROUND_KEY, Color.white);
    }

    /**
     * @param value the background colour for tools
     */
    public static void setToolsBackground(Color value) {
        settings.setColour(TOOLS_BACKGROUND_KEY, value);
    }

    /**
     * @return the colour for lines on point tracks
     */
    public static Color getPointLine() {
        return settings.getColour(POINT_LINE_KEY, Color.black);
    }

    /**
     * @param value the colour for lines on point tracks
     */
    public static void setPointLine(Color value) {
        settings.setColour(POINT_LINE_KEY, value);
    }

    /**
     * @return the colour for the axis grid
     */
    public static Color getAxisGrid() {
        return settings.getColour(AXIS_GRID_KEY, Color.lightGray);
    }

    /**
     * @param value the colour for the axis grid
     */
    public static void setAxisGrid(Color value) {
        settings.setColour(AXIS_GRID_KEY, value);
    }

    /**
     * @return the colour for reverse strands
     */
    public static Color getPointFill() {
        return settings.getColour(POINT_FILL_KEY, new Color(0, 174, 255, 150));
    }

    /**
     * @param value the fill colour for point tracks
     */
    public static void setPointFill(Color value) {
        settings.setColour(POINT_FILL_KEY, value);
    }
}
