/*
 *    Copyright 2009-2012 University of Toronto
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

/**
 * Enum which identifies all standard colours used by Savant.
 *
 * @author tarkvara
 */
public enum ColourKey {
    A,
    C,
    G,
    T,
    N,                  // Grey used for missing bases
    DELETED_BASE,       // Black for deletions
    INSERTED_BASE,      // White for insertions   
    SKIPPED,            // Grey for skipped regions in reads

    FORWARD_STRAND,
    REVERSE_STRAND,
    CONCORDANT_LENGTH,
    DISCORDANT_LENGTH,
    ONE_READ_INVERTED,
    EVERTED_PAIR,

    INTERVAL_LINE,          // Grey lines on for interval tracks.
    INTERVAL_TEXT,          // Dark grey text for interval tracks.

    CONTINUOUS_FILL,        // Blue fill colour for continuous tracks
    CONTINUOUS_LINE,        // Bluish translucent outline colour for continuous tracks

    POINT_FILL,             // Fill colour for point tracks
    POINT_LINE,             // Line colour for point tracks

    OPAQUE_GRAPH,           // Background for (non-rich) interval tracks in pack and arc modes
    TRANSLUCENT_GRAPH,      // Background for (non-rich) interval tracks in squish mode

    GRAPH_PANE_MESSAGE,             // Dark grey text for drawing messages on the GraphPane
    GRAPH_PANE_BACKGROUND_TOP,      // Grey at top of GraphPane gradient
    GRAPH_PANE_BACKGROUND_BOTTOM,   // Grey at bottom of GraphPane gradient
    GRAPH_PANE_ZOOM_FILL,           // Purplish rectangle when command-dragging on GraphPane
    GRAPH_PANE_SELECTION_FILL,

    SPLITTER,                       // Used in various DockingFrames

    AXIS_GRID,                      // Medium grey for axis gridlines
    
    HEATMAP_LOW,                    // Blue for low heat
    HEATMAP_MEDIUM,                 // Mauve for medium heat
    HEATMAP_HIGH;                   // Red for high heat

    /**
     * Provide a human-friendly name for the given colour settings.  These are currently
     * only provided for colours which are exposed through our user-interface.
     */
    public String getName() {
        switch (this) {
            case SKIPPED:
                return "Skipped";
            case FORWARD_STRAND:
                return "Forward Strand";
            case REVERSE_STRAND:
                return "Reverse Strand";
            case CONCORDANT_LENGTH:
                return "Concordant Length";
            case DISCORDANT_LENGTH:
                return "Discordant Length";
            case ONE_READ_INVERTED:
                return "One Read Inverted";
            case EVERTED_PAIR:
                return "Everted Pair";
            case INTERVAL_LINE:
                return "Interval Line";
            case INTERVAL_TEXT:
                return "Interval Text";
            case CONTINUOUS_FILL:
                return "Continuous Fill";
            case CONTINUOUS_LINE:
                return "Continuous Line";
            case POINT_LINE:
                return "Point Line";
            case POINT_FILL:
                return "Point Fill";
            default:
                return toString();
        }
    }
    
    /**
     * Provide a human-friendly explanation of where this colour is used.  These are currently
     * only provided for colours which are exposed through our user-interface.
     */
    public String getDescription() {
        switch (this) {
            case A:
                return "Nucleotide A";
            case C:
                return "Nucleotide C";
            case G:
                return "Nucleotide G";
            case T:
                return "Nucleotide T";
            case N:
                return "Missing/unknown nucleotides";

            case FORWARD_STRAND:
                return "Colour of forward strands";
            case REVERSE_STRAND:
                return "Colour of reverse strands";
            case CONCORDANT_LENGTH:
                return "Colour of concordant lengths";
            case DISCORDANT_LENGTH:
                return "Colour of discordant lengths";
            case ONE_READ_INVERTED:
                return "Colour of inverted reads";
            case EVERTED_PAIR:
                return "Colour of everted pairs";

            case INTERVAL_LINE:
                return "Line colour for interval tracks";
            case INTERVAL_TEXT:
                return "Text colour for interval tracks";

            case CONTINUOUS_FILL:
                return "Fill colour for continuous tracks";
            case CONTINUOUS_LINE:
                return "Outline colour for continuous tracks";

            case POINT_FILL:
                return "Fill colour for point tracks";
            case POINT_LINE:
                return "Line colour for point tracks";

            default:
                return null;
        }
    }
}
