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
package savant.data.types;

import java.util.Collections;
import java.util.List;

import savant.api.data.Block;
import savant.api.data.Interval;
import savant.api.data.RichIntervalRecord;
import savant.api.data.Strand;


/**
 * Immutable class to represent one line of a BED file and all the fields therein.
 * 
 * @author mfiume, vwilliams
 */
public class BEDIntervalRecord implements RichIntervalRecord {

    private final Interval interval;
    private final List<Block> blocks;
    private final String chrom;
    private final String name;
    private final float score;
    private final Strand strand;
    private final int thickStart;
    private final int thickEnd;
    private final ItemRGB itemRGB;

    /**
     * Constructor. Clients should use static factory method valueOf() instead.
     *
     * @param chrom chromosome; may not be null
     * @param start start of the interval
     * @param end of the interval
     * @param name
     * @param score
     * @param strand
     * @param thickStart
     * @param thickEnd
     * @param rgb
     * @param blocks
     */
    protected BEDIntervalRecord(String chrom, int start, int end, String name, float score, Strand strand, int thickStart, int thickEnd, ItemRGB rgb, List<Block> blocks) {

        if (chrom == null) throw new IllegalArgumentException("Invalid argument: chrom may not be null");
 
        this.interval = Interval.valueOf(start,end-1);
        this.chrom = chrom;
        this.name = name;
        this.score = score;
        this.strand = strand;
        this.thickStart = thickStart;
        this.thickEnd = thickEnd;
        this.itemRGB = rgb;
        this.blocks = blocks;
    }

    /**
     * Static factory method to construct a BEDIntervalRecord
     *
     * @param chrom chromosome; may not be null
     * @param name
     * @param score
     * @param strand
     * @param thickStart
     * @param thickEnd
     * @param rgb
     * @param blocks
     */
    public static BEDIntervalRecord valueOf(String chrom, int start, int end, String name, float score, Strand strand, int thickStart, int thickEnd, ItemRGB rgb, List<Block> blocks) {
        return new BEDIntervalRecord(chrom, start, end, name, score, strand, thickStart, thickEnd, rgb, blocks);
    }

    @Override
    public Interval getInterval() {
        return this.interval;
    }


    /**
     * To support alternate names, we would have to change our Bed format.
     * @return null
     */
    @Override
    public String getAlternateName() {
        return null;
    }

    /**
     * In Bed files, we believe all blocks to be relative to the start of the chromosome,
     * not relative to the start of the feature.
     */
    @Override
    public List<Block> getBlocks() {
        return blocks != null ? Collections.unmodifiableList(blocks) : null;
    }

    public String getChrom() {
        return chrom;
    }

    @Override
    public String getReference() {
        return getChrom();
    }
    
    @Override
    public String getName() {
        return name;
    }

    public int getChromStart() {
        return getInterval().getStart();
    }

    public int getChromEnd() {
        return getInterval().getEnd();
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public int getThickStart() {
        return thickStart;
    }

    @Override
    public int getThickEnd() {
        return thickEnd;
    }

    @Override
    public ItemRGB getItemRGB() {
        return itemRGB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BEDIntervalRecord that = (BEDIntervalRecord) o;

        if (score != that.score) return false;
        if (thickEnd != that.thickEnd) return false;
        if (thickStart != that.thickStart) return false;
        if (blocks != null ? !blocks.equals(that.blocks) : that.blocks != null) return false;
        if (!chrom.equals(that.chrom)) return false;
        if (!interval.equals(that.interval)) return false;
        if (itemRGB != null ? !itemRGB.equals(that.itemRGB) : that.itemRGB != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (strand != that.strand) return false;

        return true;
    }

    @Override
    public int compareTo(Object o) {

        BEDIntervalRecord other = (BEDIntervalRecord) o;

        //compare ref
        if (!this.getChrom().equals(other.getChrom())){
            String a1 = this.getChrom();
            String a2 = other.getChrom();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
        }

        //compare point
        int a = this.getChromStart();
        int b = other.getChromStart();

        if(a == b){
            String a1 = this.getName();
            String a2 = other.getName();
            for(int i = 0; i < Math.min(a1.length(), a2.length()); i++){
                if((int)a1.charAt(i) < (int)a2.charAt(i)) return -1;
                else if ((int)a1.charAt(i) > (int)a2.charAt(i)) return 1;
            }
            if(a1.length() < a2.length()) return -1;
            if(a1.length() > a2.length()) return 1;
            return 0;
        } else if(a < b) return -1;
        else return 1;
    }

    @Override
    public int hashCode() {
        int result = interval != null ? interval.hashCode() : 0;
        result = 31 * result + (blocks != null ? blocks.hashCode() : 0);
        result = 31 * result + (chrom != null ? chrom.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (score != +0.0f ? Float.floatToIntBits(score) : 0);
        result = 31 * result + (strand != null ? strand.hashCode() : 0);
        result = 31 * result + (int)thickStart;
        result = 31 * result + (int)thickEnd;
        result = 31 * result + (itemRGB != null ? itemRGB.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BEDIntervalRecord");
        sb.append("{interval=").append(interval);
        sb.append(", blocks=").append(blocks);
        sb.append(", chrom='").append(chrom).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", score=").append(score);
        sb.append(", strand=").append(strand);
        sb.append(", thickStart=").append(thickStart);
        sb.append(", thickEnd=").append(thickEnd);
        sb.append(", itemRGB=").append(itemRGB);
        sb.append('}');
        return sb.toString();
    }
}
