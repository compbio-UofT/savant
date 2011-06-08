/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.snp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;
import savant.controller.RangeController;
import savant.snp.Pileup.Nucleotide;
import savant.util.MiscUtils;
import savant.settings.ColourSettings;

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

    public void setTransparency(int percent) {
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



            int startpos = MiscUtils.transformPositionToPixel((int) p.getPosition(), this.getWidth(), RangeController.getInstance().getRange());
            int endpos = MiscUtils.transformPositionToPixel((int) p.getPosition()+1, this.getWidth(), RangeController.getInstance().getRange());

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

            

            if (p.isHeterozygous()) {

                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
                    RenderingHints.VALUE_ANTIALIAS_ON);

                int step = 10;
                int[] xpoints = new int[] { startpos, endpos, endpos, startpos };
                int[] ypoints = new int[] { step, 0, step, step*2 };

                for (int i = 0; i < this.getHeight(); i+= step) {
                    g.fillPolygon(xpoints, ypoints, xpoints.length);
                    for (int j = 0; j < xpoints.length; j++) {
                        ypoints[j] += 2*step;
                    }
                }
            } else {
                g.fillRect(startpos, 0, endpos-startpos, this.getHeight());
            }

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