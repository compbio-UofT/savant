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
    UNMAPPED_MATE,

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
            case UNMAPPED_MATE:
                return "Unmapped Mate";
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
            case UNMAPPED_MATE:
                return "Colour of read whose mate is unmapped";

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
