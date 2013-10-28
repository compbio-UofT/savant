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
package savant.snp;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import javax.swing.JPanel;
import savant.controller.LocationController;
import savant.snp.Pileup.Nucleotide;
import savant.util.MiscUtils;


/**
 *
 * @author mfiume
 */
class PileupPanel extends JPanel {

    private boolean isOn = true;
    private List<Pileup> pileups;
    //private int startPosition;
    private int transparency;
    private Mode mode;

    private static int defaultTransparency = 80;

    public void setPileups(List<Pileup> pile) {
        this.pileups = pile;
    }

    public List<Pileup> getPileups() {
        return this.pileups;
    }

    public final void setTransparency(int percent) {
        percent = 100-percent;
        this.transparency = ((int) ((((double) percent) / 100)*255));
        //System.out.println("Setting transparency to " + transparency);
    }

    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }

    public boolean getIsOn() {
        return this.isOn;
    }

    public void setMode(Mode m) {
        this.mode = m;
    }

    public Mode getMode() {
        return this.mode;
    }

    public int getTransparency() {
        return (transparency * 100)/255;
    }

    private void renderAsSNPs(Graphics g) {

        // need to somehow clear the background...

        for (Pileup p : pileups) {

            int startpos = MiscUtils.transformPositionToPixel((int) p.getPosition(), this.getWidth(), LocationController.getInstance().getRange());
            int endpos = MiscUtils.transformPositionToPixel((int) p.getPosition()+1, this.getWidth(), LocationController.getInstance().getRange());

            Nucleotide snpNuc = p.getSNPNucleotide();

            //System.out.println("Position " + p.getPosition() + " has SNP : " + snpNuc + " / " + p.getReferenceNucleotide());

            switch(snpNuc) {
                case A:
                    g.setColor(new Color(27, 97, 97, transparency));
                    break;
                case C:
                    g.setColor(new Color(162, 45, 45, transparency));
                    break;
                case G:
                    g.setColor(new Color(36, 130, 36, transparency));
                    break;
                case T:
                    g.setColor(new Color(162,98, 45, transparency));
                    break;
                default:
                    g.setColor(new Color(50, 50, 50, transparency));
                    break;

            }

            g.fillRect(startpos, 0, endpos-startpos, this.getHeight());
            //g.setColor(new Color(70,70,70,this.transparency));
            //g.drawRect(startpos, 0, endpos-startpos, this.getHeight());
        }
    }

    private void renderLogos(Graphics g) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static enum Mode { SNP, Logo }

    public PileupPanel(List<Pileup> piles) {
        this(piles,defaultTransparency);
    }

    public PileupPanel(List<Pileup> piles, int transparency) {
        this(piles,Mode.SNP,transparency);
    }

    PileupPanel(List<Pileup> piles, Mode mode, int transparency) {
        this.setOpaque(false);
        this.pileups = piles;
        this.mode = mode;
        setTransparency(transparency);

        //System.out.println("Drawing " + piles.size() + " SNPs");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //System.out.println("Repainting Pileup Panel with transparency=" + this.transparency);

        //g.clearRect(0, 0, this.getWidth(), this.getHeight());

        if (!isOn) { return; }

        switch(mode) {
            case SNP:
                renderAsSNPs(g);
                break;
            case Logo:
                renderLogos(g);
                break;
            default:
                renderAsSNPs(g);
                break;
        }
    }
}