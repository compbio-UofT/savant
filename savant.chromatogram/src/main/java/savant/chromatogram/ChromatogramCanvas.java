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
package savant.chromatogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.JPanel;

import org.biojava.bio.chromatogram.Chromatogram;
import org.biojava.bio.chromatogram.graphic.ChromatogramGraphic;
import org.biojava.bio.chromatogram.graphic.FixedBaseWidthScaler;
import org.biojava.bio.seq.DNATools;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.util.NavigationUtils;
import savant.api.util.RangeUtils;
import savant.settings.ColourSettings;
import savant.util.ColourKey;


/**
 * Translucent panel which renders the chromatogram on top of a sequence track.
 *
 * @author mfiume
 */
class ChromatogramCanvas extends JPanel {

    Chromatogram chromatogram;
    int startBase;
    FixedBaseWidthScaler scaler;
    TrackAdapter track;
    boolean fillBackground;

    public ChromatogramCanvas(Chromatogram c, TrackAdapter t) {
        chromatogram = c;
        track = t;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double startX = track.transformXPos(startBase);
        g2.translate(startX, 0.0);
        
        float baseWidth = (float)(track.transformXPos(1) - track.transformXPos(0));
        scaler = new FixedBaseWidthScaler(baseWidth);

        ChromatogramGraphic cg = new ChromatogramGraphic(chromatogram);
        cg.setOption(ChromatogramGraphic.Option.HORIZONTAL_NONLINEAR_SCALER, scaler);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_A, false);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_C, false);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_G, false);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_T, false);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_OTHER, false);
        cg.setOption(ChromatogramGraphic.Option.DRAW_CALL_SEPARATORS, false);
        if (baseWidth > 4.0f) {
            // If we're zoomed in close enough, use a thicker pen.
            Stroke thickStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            cg.setOption(ChromatogramGraphic.Option.TRACE_STROKE, thickStroke);
        }

        // Trace colours are derived from Savant colour settings.
        cg.setBaseColor(DNATools.a(), calculateTraceColor(ColourSettings.getColor(ColourKey.A)));
        cg.setBaseColor(DNATools.c(), calculateTraceColor(ColourSettings.getColor(ColourKey.C)));
        cg.setBaseColor(DNATools.g(), calculateTraceColor(ColourSettings.getColor(ColourKey.G)));
        cg.setBaseColor(DNATools.t(), calculateTraceColor(ColourSettings.getColor(ColourKey.T)));

        int w = (int)(track.transformXPos(startBase + chromatogram.getSequenceLength()) - startX);

        if (fillBackground) {
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillRect(0, 0, w, getHeight());
        }
        cg.setHeight(getHeight());
        cg.setWidth(w);
        cg.drawTo(g2);
    }
    
    /**
     * Change the start base.  If the resulting range would put the chromatogram completely off-screen,
     * change the range so the chromatogram is on-screen and centred.
     */
    public void updatePos(int start) {
        if (startBase != start) {
            startBase = start;
            RangeAdapter r = RangeUtils.createRange(startBase, startBase + chromatogram.getSequenceLength());
            if (RangeUtils.intersects(r, NavigationUtils.getCurrentRange())) {
                // Already onscreen, so just repaint.
                repaint();
            } else {
                // Centre the chromatogram using our bookmark-centring logic.
                int buffer = Math.max(250, r.getLength() / 4);
                int newStart = Math.max(1, r.getFrom() - buffer);
                NavigationUtils.navigateTo(RangeUtils.createRange(newStart, r.getTo() + buffer));
            }
        }
    }

    public void updateFillbackground(boolean value) {
        if (value != fillBackground) {
            fillBackground = value;
            repaint();
        }
    }

    /**
     * Our trace colours are more-saturated versions of the base colours used by Savant.
     * If we wanted to provide better contrast, we could write something more elaborate here.
     */
    private static Color calculateTraceColor(Color base) {
        return base.brighter();
    }
}
