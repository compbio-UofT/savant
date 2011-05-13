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

import java.awt.*;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.GraphPaneController;
import savant.controller.RangeController;
import savant.controller.event.GraphPaneChangedEvent;
import savant.controller.event.GraphPaneChangedListener;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.util.MiscUtils;


/**
 *
 * @author mfiume
 */
public class Ruler extends JPanel {
    private static final Log LOG = LogFactory.getLog(Ruler.class);

    private int minimum = 0;
    private int maximum = 100;
    private static final long serialVersionUID = 1L;
    private final JLabel mousePosition;
    private final JLabel recStart;
    private final JLabel recStop;
    private final JLabel cords; // set up GUI and register mouse event handlers
    boolean isNewRect = true;

    public Ruler() {

        this.mousePosition = new JLabel();
        this.mousePosition.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(this.mousePosition, BorderLayout.CENTER);

        this.recStart = new JLabel();
        this.add(this.recStart, BorderLayout.WEST);

        this.recStop = new JLabel();
        this.add(this.recStop, BorderLayout.EAST);

        this.cords = new JLabel();
        this.add(this.cords, BorderLayout.NORTH);

        GraphPaneController.getInstance().addGraphPaneChangedListener(new GraphPaneChangedListener() {
           @Override
            public void graphPaneChanged(GraphPaneChangedEvent event) {
                if (event.getSource().isPanning()) {
                    repaint();
                }
            }
        });

        RangeController.getInstance().addRangeChangedListener(new RangeChangedListener() {
            @Override
            public void rangeChanged(RangeChangedEvent event) {
                repaint();
            }
        });
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        GraphPaneController gpc = GraphPaneController.getInstance();
        if (gpc.isPanning()) {
            int fromx = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getFrom(), this.getWidth(), RangeController.getInstance().getRange());
            int tox = MiscUtils.transformPositionToPixel(gpc.getMouseDragRange().getTo(), this.getWidth(), RangeController.getInstance().getRange());

            double shiftamount = tox-fromx;
            g.translate((int) shiftamount, 0);
        }

        renderBackground(g);
    }

    /**
     * Render the background of this graphpane
     * @param g The graphics object to use
     */
    private void renderBackground(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        try {

            Image image = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/bar_selected_glossy.png"));
            Composite originalComposite = ((Graphics2D) g).getComposite();
            ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85F));
            g.drawImage(image, 0-this.getWidth(),0,this.getWidth()*3,this.getHeight(),this);
            ((Graphics2D) g).setComposite(originalComposite);
        } catch (Exception e) {
            LOG.error("Error drawing image background");
        }

        int numseparators = (int) Math.max(Math.ceil(Math.log(this.maximum-this.minimum)),2);
        int genomicSeparation = (maximum -  minimum) / Math.max(1, numseparators);

        if (numseparators != 0) {
            int width = this.getWidth();
            double barseparation = width / numseparators;

            int minstringseparation = 200;
            int skipstring = (int) Math.round(minstringseparation / barseparation);

            int startbarsfrom = MiscUtils.transformPositionToPixel(
                    (int) (Math.floor((RangeController.getInstance().getRange().getFrom()/Math.max(1, genomicSeparation)))*genomicSeparation),
                    width, (RangeController.getInstance()).getRange());

            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i <= numseparators*3; i++) {
                g2.setColor(new Color(50,50,50,50)); //BrowserDefaults.colorAxisGrid);
                int xOne = startbarsfrom + (int)Math.ceil(i*barseparation)+1 - this.getWidth();
                int xTwo = xOne;
                int yOne = this.getHeight();
                int yTwo = 0;
                g2.drawLine(xOne,yOne,xTwo,yTwo);

                if (skipstring != 0) {
                    if (i % skipstring == 0) {
                        g2.setColor(Color.black);
                        int a = MiscUtils.transformPixelToPosition(xOne, width, (RangeController.getInstance()).getRange());
                        if (a >= 1 && a <= (RangeController.getInstance()).getMaxRangeEnd()) {
                            String numstr = MiscUtils.posToShortStringWithSeparation(a, genomicSeparation);
                            g2.setColor(Color.black);
                            g2.drawString(numstr, (float) (xOne + 3), (float) ((this.getHeight()*0.5)+3));
                        }
                    }
                } else {
                    int a = MiscUtils.transformPixelToPosition(xOne, width, (RangeController.getInstance()).getRange());
                    String numstr = MiscUtils.numToString(a);
                    g2.setColor(Color.black);
                    g2.drawString(numstr, (float) (xOne + 3), (float) ((this.getHeight()*0.5)+3));
                }
            }

            if (RangeController.getInstance().getRange().getLength() >= RangeController.getInstance().getRangeStart()) {
                try {
                    Image image_left_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_left_bordered.png"));
                    int capwidth = 20;
                    int pos = MiscUtils.transformPositionToPixel(1, this.getWidth(), RangeController.getInstance().getRange());
                    g.drawImage(image_left_cap, pos,0,capwidth,23,this);
                    g.setColor(Savant.getInstance().getBackground());
                    g.fillRect(pos,0, -this.getWidth(), this.getHeight());
                } catch (IOException ex) {
                    LOG.error("Drawing failed.", ex);
                }
            }

            if (RangeController.getInstance().getRange().getLength() >= RangeController.getInstance().getMaxRangeEnd() - RangeController.getInstance().getRangeEnd()) {
                try {
                    Image image_right_cap = javax.imageio.ImageIO.read(getClass().getResource("/savant/images/round_cap_right_bordered.png"));
                    int capwidth = 20;
                    int pos = MiscUtils.transformPositionToPixel(RangeController.getInstance().getMaxRangeEnd(), this.getWidth(), RangeController.getInstance().getRange());
                    g.drawImage(image_right_cap, pos-capwidth,0,capwidth,23,this);
                    g.setColor(Savant.getInstance().getBackground());
                    g.fillRect(pos,0, this.getWidth(), this.getHeight());
                    
                } catch (IOException ex) {
                    LOG.error("Drawing failed.", ex);
                } 
            }
        }
    }
}


