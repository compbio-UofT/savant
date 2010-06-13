/*
 *    Copyright 2010 University of Toronto
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

/*
 * GenericIntervalRecord.java
 * Created on Jan 8, 2010
 */

package savant.model;

import savant.util.Block;
import savant.util.ItemRGB;
import savant.util.Strand;

import java.util.List;

/**
 * Class to represent one line of a BED file and all the fields therein.
 * 
 * @author mfiume
 */
public class BEDIntervalRecord extends IntervalRecord {

    private List<Block> blocks;
    private String chrom;
    private String name;
    private int chromStart;
    private int chromEnd;
    private int score;
    private Strand strand;
    private int thickStart;
    private int thickEnd;
    private ItemRGB itemRGB;

    public BEDIntervalRecord(Interval interval, String name, int score, Strand strand, int thickStart, int thickEnd, ItemRGB rgb,  List<Block> blocks) {
        super(interval);
        this.name = name;
        this.score = score;
        this.strand = strand;
        this.thickStart = thickStart;
        this.thickEnd = thickEnd;
        this.itemRGB = rgb;
        this.blocks = blocks;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getChrom() {
        return chrom;
    }

    public void setChrom(String chrom) {
        this.chrom = chrom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChromStart() {
        return chromStart;
    }

    public void setChromStart(int chromStart) {
        this.chromStart = chromStart;
    }

    public int getChromEnd() {
        return chromEnd;
    }

    public void setChromEnd(int chromEnd) {
        this.chromEnd = chromEnd;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Strand getStrand() {
        return strand;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }

    public int getThickStart() {
        return thickStart;
    }

    public void setThickStart(int thickStart) {
        this.thickStart = thickStart;
    }

    public int getThickEnd() {
        return thickEnd;
    }

    public void setThickEnd(int thickEnd) {
        this.thickEnd = thickEnd;
    }

    public ItemRGB getItemRGB() {
        return itemRGB;
    }

    public void setItemRGB(ItemRGB itemRGB) {
        this.itemRGB = itemRGB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BEDIntervalRecord that = (BEDIntervalRecord) o;

        if (chromEnd != that.chromEnd) return false;
        if (chromStart != that.chromStart) return false;
        if (score != that.score) return false;
        if (thickEnd != that.thickEnd) return false;
        if (thickStart != that.thickStart) return false;
        if (blocks != null ? !blocks.equals(that.blocks) : that.blocks != null) return false;
        if (chrom != null ? !chrom.equals(that.chrom) : that.chrom != null) return false;
        if (itemRGB != null ? !itemRGB.equals(that.itemRGB) : that.itemRGB != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (strand != that.strand) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (blocks != null ? blocks.hashCode() : 0);
        result = 31 * result + (chrom != null ? chrom.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + chromStart;
        result = 31 * result + chromEnd;
        result = 31 * result + score;
        result = 31 * result + (strand != null ? strand.hashCode() : 0);
        result = 31 * result + thickStart;
        result = 31 * result + thickEnd;
        result = 31 * result + (itemRGB != null ? itemRGB.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BEDIntervalRecord");
        sb.append("{blocks=").append(blocks);
        sb.append(", chrom='").append(chrom).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", chromStart=").append(chromStart);
        sb.append(", chromEnd=").append(chromEnd);
        sb.append(", score=").append(score);
        sb.append(", strand=").append(strand);
        sb.append(", thickStart=").append(thickStart);
        sb.append(", thickEnd=").append(thickEnd);
        sb.append(", itemRGB=").append(itemRGB);
        sb.append('}');
        return sb.toString();
    }
}
