/*
 *    Copyright 2010-2011 University of Toronto
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
package savant.data.types;

import java.util.Collections;
import java.util.List;


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
     * @param interval interval including chromStart and chromEnd; may not be null
     * @param name
     * @param score
     * @param strand
     * @param thickStart
     * @param thickEnd
     * @param rgb
     * @param blocks
     */
    private BEDIntervalRecord(String chrom, int start, int end, String name, float score, Strand strand, int thickStart, int thickEnd, ItemRGB rgb, List<Block> blocks) {

        if (chrom == null) throw new IllegalArgumentException("Invalid argument: chrom may not be null");
        //if (interval == null) throw new IllegalArgumentException("Invalid argument. Interval must not be null");
 
        this.interval = new Interval(start,end-1);
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
     * @param interval interval interval including chromStart and chromEnd; may not be null
     * @param name
     * @param score
     * @param strand
     * @param thickStart
     * @param thickEnd
     * @param rgb
     * @param blocks
     * @return
     */
    public static BEDIntervalRecord valueOf(String chrom, int start, int end, String name, float score, Strand strand, int thickStart, int thickEnd, ItemRGB rgb, List<Block> blocks) {
        return new BEDIntervalRecord(chrom, start, end, name, score, strand, thickStart, thickEnd, rgb, blocks);
    }

    @Override
    public Interval getInterval() {
        return this.interval;
    }


    /**
     * In Bed files, we believe all blocks to be relative to the start of the chromosome,
     * not relative to the start of the feature.  Equivalent to calling getBlocks(0).
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
