package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.LittleEndianInputStream;

import java.io.IOException;
import java.io.DataInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 6, 2010
 * Time: 3:00:11 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for BigWig section header class for data items - BBFile Table J
*
*   Note: appropriate WIG data formats are accomodated
*   according to WIG type in Table J
* */
public class BigWigSectionHeader {

    public enum WigItemType {
        BedGraph,
        VarStep,
        FixedStep,
        Unknown     // bad value
    }

    private static Logger log = Logger.getLogger(BigWigSectionHeader.class);

    public static final int SECTION_HEADER_SIZE = 24;
    public static final int FIXEDSTEP_ITEM_SIZE = 4;
    public static final int VARSTEP_ITEM_SIZE = 8;
    public static final int BEDGRAPH_ITEM_SIZE = 12;

    private int mChromID;       // Chromosome/contig Numerical ID from BBFile Chromosome B+ tree
    private int mChromStart;    // starting base position
    private int mChromEnd;      // ending base position
    private int mItemStep;      // number of base spaces between fixed items
    private int mItemSpan;      // number of bases in fixed step items
    private WigItemType mItemType; // type of data items: 1 = bedGraph, 2 = varStep, 3 = fixedStep
    private byte mReserved;     // reserved; currently = 0
    private short mItemCount;   // number of data items in this chromosome section

    private boolean mIsValidType;    // indicates a if a valid Wig item type was read
    private String mItemDescription; // string representation of item type.

    /*
    *   Constructor creates a Wig Section Header (Table J) from uncompressed buffer.
    *
    *   Parameters:
    *       mLbdis - // buffer stream containing section header arranged low to high bytes
    * */
    public BigWigSectionHeader(LittleEndianInputStream lbdis) {

        byte type;

        // get Wig Section Header
        try {
            mChromID = lbdis.readInt();
            mChromStart = lbdis.readInt();
            mChromEnd = lbdis.readInt();
            mItemStep = lbdis.readInt();
            mItemSpan = lbdis.readInt();
            type = lbdis.readByte();
            mReserved = lbdis.readByte();
            mItemCount = lbdis.readShort();
        }catch(IOException ex) {
            log.error("Error reading wig section header ", ex);
            throw new RuntimeException("Error reading wig section header", ex);
        }

        // tag as valid
        mIsValidType = getItemType(type);
    }

    /*
    *   Constructor creates a Wig Section Header (Table J) from uncompressed buffer.
    *
    *   Parameters:
    *       mLbdis - // buffer stream containing section header arranged high to low bytes
    * */
    public BigWigSectionHeader(DataInputStream bdis) {

        byte type;

        // get Wig Section Header
        try {
            mChromID = bdis.readInt();
            mChromStart = bdis.readInt();
            mChromEnd = bdis.readInt();
            mItemStep = bdis.readInt();
            mItemSpan = bdis.readInt();
            type = bdis.readByte();
            mReserved = bdis.readByte();
            mItemCount = bdis.readShort();
        }catch(IOException ex) {
            log.error("Error reading wig section header ", ex);
            throw new RuntimeException("Error reading wig section header", ex);
        }

        // tag as valid
        mIsValidType = getItemType(type);
    }

    /*
    *   Method returns the chromosome ID
    *
    *   Returns:
    *       Chromosome ID for the section's region
    * */
    public int getChromID() {
        return mChromID;
    }

    /*
    *   Method returns the chromosome starting base
    *
    *   Returns:
    *       Chromosome start base for the section's region
    * */
    public int getChromosomeStart() {
        return mChromStart;
    }

    /*
    *   Method returns the chromosome ending base
    *
    *   Returns:
    *       Chromosome end base for the section's region
    * */
    public int getChromosomeEnd() {
        return mChromEnd;
    }

    /*
    *   Method returns the base pairs step between items.
    *
    *   Returns:
    *       Chromosome base step between fixed step sections
    * */
    public int getItemStep() {
        return mItemStep;
    }

    /*
    *   Method returns the base pairs span in items.
    *
    *   Returns:
    *       Chromosome base span for fixed and variable step sections
    * */
    public int getItemSpan() {
        return mItemSpan;
    }

    /*
    *   Method returns the item type for the section's Wig data.
    *
    *   Returns:
    *       Section item type for Wig data
    * */
    public WigItemType getItemType() {
        return mItemType;
    }

    /*
    *   Method returns if the section's data item type is valid.
    *
    *   Returns:
    *       Specifies if section's data iytem type is valid
    * */
    public boolean IsValidType() {
        return mIsValidType;
    }

    /*
    *   Method returns the number of section items.
    *
    *   Returns:
    *       Number of items defined for the section
    * */
    public short getItemCount() {
        return mItemCount;
    }

    /*
    *   Method returns the reserved value for the section.
    *
    *   Returns:
    *       Reserved byte for the section (should always be 0)
    * */
    public byte getReserved() {
        return mReserved;
    }

    public void print(){
        log.info(" BigWig section header "
                + " for "+ mItemDescription + " data");
        log.info("Chromosome ID = " + mChromID);
        log.info("ChromStart = " + mChromStart);
        log.info("ChromEnd = " + mChromEnd);
        log.info("ItemStep = " + mItemStep);
        log.info("ItemSpan = " + mItemSpan);
        log.info("ItemType = " + mItemType);
        log.info("mReserved = " + mReserved);
        log.info("mItemCount = " + mItemCount);
    }

    /*
    *   Method determines the Wig data type.
    *
    *   Parameters:
    *       byte type read from Wig section header
    *
    *   Returns:
    *       Indicates if type is a valid Wig item type
    * */
    private boolean getItemType(byte type){
        boolean isValid;

        if(type == 1){
            mItemType = WigItemType.BedGraph;
            mItemDescription = "Wig Bed Graph";
            isValid = true;
        }
        else if(type == 2){
            mItemType = WigItemType.VarStep;
            mItemDescription = "Wig Variable Step";
            isValid = true;
        }
        else if(type == 3){
            mItemType = WigItemType.FixedStep;
            mItemDescription = "Wig Fixed Step";
            isValid = true;
        }
        else {
            mItemType = WigItemType.Unknown;
            mItemDescription = "Wig Type Unknown";
            isValid = false;
        }

        return isValid;
    }

}
