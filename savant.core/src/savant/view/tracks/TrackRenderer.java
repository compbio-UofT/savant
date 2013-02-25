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

package savant.view.tracks;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.event.DataRetrievalEvent;
import savant.api.util.Listener;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.exception.RenderingException;
import savant.selection.SelectionController;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;
import savant.util.MiscUtils;


/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer implements Listener<DataRetrievalEvent> {
    private static final Log LOG = LogFactory.getLog(TrackRenderer.class);

    private static final int MIN_TRANSPARENCY = 20;
    private static final int MAX_TRANSPARENCY = 255;

    protected static final Font LEGEND_FONT = new Font("Sans-Serif", Font.PLAIN, 10);
    protected static final Stroke ONE_STROKE = new BasicStroke(1.0f);
    protected static final Stroke TWO_STROKE = new BasicStroke(2.0f);
    
    /** Size of colour swatch used in legend for bases. */
    protected static final Dimension SWATCH_SIZE = new Dimension(6, 13);
    
    /** Notional line-height in legends. */
    protected static final int LEGEND_LINE_HEIGHT = 18;

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected String trackName;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();
    protected Map<Record, Shape> artifactMap = new HashMap<Record, Shape>(); //meta info pointing to reads (ie. lines in read pair mode)

    protected TrackRenderer() {
    }

    /**
     * Set the track's name.  This is used only to provide a key when dealing with the SelectionController.
     *
     * @param name 
     */
    public void setTrackName(String name) {
        trackName = name;
    }

    /**
     * Sets the data to null so we know that there's nothing to render.
     *
     * @param evt describes the data being received
     */
    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case STARTED:
                // Sets the data to null so we know that there's nothing to render.
                data = null;
                break;
            case COMPLETED:
                // Default handler just sets the renderer to have the newly-received data.
                LOG.debug("TrackRenderer received dataRetrievalCompleted, removing PROGRESS.");
                instructions.remove(DrawingInstruction.PROGRESS);
                data = evt.getData();
                break;
            case FAILED:
                // Data retrieval has failed for some reason.
                instructions.remove(DrawingInstruction.PROGRESS);
                instructions.put(DrawingInstruction.ERROR, new RenderingException(MiscUtils.getMessage(evt.getError()), RenderingException.ERROR_PRIORITY));
                break;
        }
    }

    public boolean isWaitingForData() {
        return data == null && instructions.containsKey(DrawingInstruction.PROGRESS);
    }

    public void addInstruction(DrawingInstruction key, Object value) {
        instructions.put(key, value);
    }

    /**
     * Retrieve the value of a specific drawing instruction for this renderer.
     *
     * @param key key identifying the drawing instruction to be retrieved
     * @return the value of that drawing instruction
     */
    public Object getInstruction(DrawingInstruction key) {
        return instructions.get(key);
    }

    /**
     * Remove all instructions to allow for a fresh render.
     */
    public void clearInstructions() {
        instructions.clear();
    }

    public abstract void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException;

    /**
     * Before doing any actual rendering, derived classes should call this in their
     * render() methods in case we want to display a message instead.
     *
     * If graphPane is given and error is thrown, make sure graphPane is resized if necessary.
     *
     * @throws RenderingException
     */
    protected void renderPreCheck() throws RenderingException {

        // Clear away any shapes.
        recordToShapeMap.clear();
        artifactMap.clear();

        Boolean refExists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (refExists == null || !refExists) {
            throw new RenderingException("No data for " + LocationController.getInstance().getReferenceName(), RenderingException.INFO_PRIORITY);
        }
        RenderingException error = (RenderingException)instructions.get(DrawingInstruction.ERROR);
        if (error != null) {
            throw error;
        }
        if (data == null || data.isEmpty()) {
            throw new RenderingException("No data in range", RenderingException.INFO_PRIORITY);
        }
    }

    /**
     * Check whether to perform selection for this track.
     *
     * @param checkRes if true, return true only if resolution is HIGH
     * @return whether or not to allow selection at this time
     */
    public boolean selectionAllowed(boolean checkRes) {
        Object instr_select = instructions.get(DrawingInstruction.SELECTION_ALLOWED);
        if (instr_select == null || instr_select.equals(false)) return false;
        if (checkRes) {
            Object instr_res = instructions.get(DrawingInstruction.RESOLUTION);
            if (instr_res == null || !instr_res.equals(Resolution.HIGH))
                 return false;
        }
        return true;
    }

    public boolean hasMappedValues() {
        return !recordToShapeMap.isEmpty();
    }

    public Map<Record, Shape> searchPoint(Point p) {

        if (!hasMappedValues() || data == null) return null;
        
        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        
        Map<Record, Shape> map = new HashMap<Record, Shape>();
        boolean allowFuzzySNPs = true;
               
        Rectangle2D testIntersection = new Rectangle2D.Double(p.x-3, p.y-3, 7, 7);
        for (Record rec: recordToShapeMap.keySet()) {
            Shape s = recordToShapeMap.get(rec);

            if (s != null) {
                //if (contains AND (notArc OR (isEdge...))
                boolean hit = false;
                if (mode == DrawingMode.ARC || mode == DrawingMode.ARC_PAIRED) {
                    hit = s.intersects(testIntersection) && (!s.contains(p.x-3, p.y-3) || !s.contains(p.x+3, p.y-3));
                } else {
                    hit = s.contains(p);
                }
                // At low resolutions, SNPs can be hard to hit with the mouse, so give a second chance with a fuzzier check.
                if (mode == DrawingMode.SNP || mode == DrawingMode.STRAND_SNP || mode == DrawingMode.MATRIX) {
                    if (hit) {
                        if (allowFuzzySNPs) {
                            // We may have accumulated some fuzzy SNP hits.  We now have an exact one, so dump the fuzzies.
                            map.clear();
                            allowFuzzySNPs = false;
                        }
                    } else {
                        if (allowFuzzySNPs) {
                            hit = s.intersects(testIntersection);
                        }
                    }
                }
                if (hit) {
                    map.put(rec, s);
                    continue;
                }
            } else {
                LOG.info("Why is shape null for " + rec);
            }
            
            //check other artifacts
            Shape artifact = artifactMap.get(rec);
            if (artifact != null && artifact.contains(p.x, p.y)) {
                map.put(rec, s);
            }
        }
        return map.isEmpty() ? null : map;
    }

    public boolean rectangleSelect(Rectangle2D rect) {

        if (!selectionAllowed(false) || !hasMappedValues()) return false;

        boolean repaint = false;
        List<Record> toAdd = new ArrayList<Record>();

        for (Record o: recordToShapeMap.keySet()) {
            Shape s = recordToShapeMap.get(o);
            if (s == null) continue;
            if (s.intersects(rect)) {
                toAdd.add(o);
                repaint = true;
            }
        }

        if (repaint) {
            SelectionController.getInstance().addMultipleSelections(trackName, toAdd);
        }

        return repaint;
    }


    public void addToSelected(Record rec) {
        if (selectionAllowed(false)) {
            SelectionController.getInstance().toggleSelection(trackName, rec);
        }
    }

    /**
     * Invoked when the user chooses "Select Pair" from the Bam popup menu.
     * @param i record to be selected
     */
    public void forceAddToSelected(Record i) {
        SelectionController.getInstance().addSelection(trackName, i);
    }


    /**
     * Current selected shapes.
     */
    public List<Shape> getCurrentSelectedShapes(GraphPaneAdapter gp) {
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(trackName, LocationController.getInstance().getRange(), data);
        for (int i = 0; i < currentSelected.size(); i++) {
            Shape s = recordToShapeMap.get(currentSelected.get(i));
            if (s != null) {
                shapes.add(s);
            }
        }
        return shapes;
    }

    /**
     * For practical reasons, we never want alpha to be less than 20 or more than 255.
     */
    public static int getConstrainedAlpha(int alpha) {
        return alpha < MIN_TRANSPARENCY ? MIN_TRANSPARENCY : (alpha > MAX_TRANSPARENCY ? MAX_TRANSPARENCY : alpha);
    }
    
    public void toggleGroup(ArrayList<Record> recs) {
        if (selectionAllowed(false)) {
            SelectionController.getInstance().toggleGroup(trackName, recs);
        }
    }

    public void drawFeatureLabel(Graphics2D g2, String geneName, double startXPos, double y) {
        FontMetrics fm = g2.getFontMetrics();
        double stringstartx = startXPos - fm.stringWidth(geneName) - 5;

        if (stringstartx <= 0) {
            Rectangle2D r = fm.getStringBounds(geneName, g2);

            int b = 2;
            Color textColor = g2.getColor();
            g2.setColor(new Color(255,255,255,200));
            g2.fill(new RoundRectangle2D.Double(3.0, y - (fm.getHeight() - fm.getDescent()) - b, r.getWidth() + 2 * b, r.getHeight() + 2 * b, 8.0, 8.0));
            g2.setColor(textColor);
            g2.drawString(geneName, 5.0F, (float)y);
        } else {
            g2.drawString(geneName, (float)stringstartx, (float)y);
        }
    }

    /**
     * Shared by BAMTrackRenderer and RichIntervalTrackRenderer to draw the white diamond
     * which indicates an insertion.
     */
    public Shape drawInsertion(Graphics2D g2, double x, double y, double unitWidth, double unitHeight) {

        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        g2.setColor(cs.getColor(ColourKey.INSERTED_BASE));
        double w = unitWidth * 0.5;

        Path2D.Double rhombus = new Path2D.Double();
        rhombus.moveTo(x, y);
        rhombus.lineTo(x + w, y + unitHeight * 0.5);
        rhombus.lineTo(x, y + unitHeight);
        rhombus.lineTo(x - w, y + unitHeight * 0.5);
        rhombus.closePath();
        g2.fill(rhombus);

        return rhombus;
    }
    
    
    /**
     * Return the dimensions of the legend which this renderer currently requires.
     * If the derived renderer doesn't have a legend, it should return null.
     */
    public Dimension getLegendSize(DrawingMode mode) {
        return null;
    }
    
    /**
     * Draw the actual legend.
     */
    public void drawLegend(Graphics2D g2, DrawingMode mode) {
    }

    /**
     * Simplest kind of legend is just a list of coloured lines with names next to them.
     */
    protected void drawSimpleLegend(Graphics2D g2, int x, int y, ColourKey... keys) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);

        g2.setFont(LEGEND_FONT);

        for (ColourKey k: keys) {
            String legendString = k.getName();
            g2.setColor(cs.getColor(k));
            g2.setStroke(TWO_STROKE );
            Rectangle2D stringRect = LEGEND_FONT.getStringBounds(legendString, g2.getFontRenderContext());
            g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
            g2.setStroke(ONE_STROKE);
            g2.drawString(legendString, x, y);

            y += LEGEND_LINE_HEIGHT;
        }
    }
    
    /**
     * Draw a legend which consists of the given bases arranged horizontally
     */
    protected void drawBaseLegend(Graphics2D g2, int x, int y, ColourKey... keys) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        g2.setFont(LEGEND_FONT);

        for (ColourKey k: keys) {
            g2.setColor(cs.getColor(k));
            g2.fillRect(x, y - SWATCH_SIZE.height + 2, SWATCH_SIZE.width, SWATCH_SIZE.height);
            g2.setColor(Color.BLACK);
            g2.drawString(k.getName(), x + SWATCH_SIZE.width + 3, y);
            x += 27;
        }
    }
}
