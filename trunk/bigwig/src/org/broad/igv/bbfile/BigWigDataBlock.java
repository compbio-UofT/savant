package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.LittleEndianInputStream;

import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Apr 16, 2010
 * Time: 10:32:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class BigWigDataBlock {

    private static Logger log = Logger.getLogger(BigWigDataBlock.class);

    // BigWig data types sizes
    final int FIXED_STEP_ITEM_SIZE = 4;
    final int VAR_STEP_ITEM_SIZE = 8;
    final int BED_GRAPH_ITEM_SIZE = 12;

    // Bed data block access variables   - for reading in bed records from a file
    private SeekableStream mBBFis;  // file input stream handle
    private long mFileOffset;       // Wig data block file offset
    private long mLeafDataSize;     // byte size for data block specified in the R+ leaf
    private boolean mIsLowToHigh;   // if true, data is low to high byte order; else high to low

    // defines the bigWig data source
    private HashMap<Integer, String> mChromosomeMap;  // map of chromosome ID's and corresponding names
    private RPTreeLeafNodeItem mLeafHitItem;   // R+ leaf item containing data block location

    // uncompressed byte stream buffer and readers
    private byte[] mWigBuffer;      // buffer containing leaf block data uncompressed
    private int mRemDataSize;       // number of uncompressed data bytes not extracted

    // Wig data extraction members
    private ArrayList<WigItem> mWigItemList;  // array of Wig section items

    /*
    *   Constructor for Wig data block reader.
    *
    *   Parameters:
    *       fis - file input stream handle
    *       leafHitItem - R+ tree leaf hit item containing data block file location and hit status
    *       chromIDTree - B+ chromosome index tree returns chromosome ID's for names
    *       isLowToHigh - byte order is low to high if true; else high to low
    *       uncompressBufSize - byte size for decompression buffer; else 0 for uncompressed
    *
    * */
    public BigWigDataBlock(SeekableStream fis, RPTreeLeafNodeItem leafHitItem,
            HashMap<Integer, String> chromosomeMap, boolean isLowToHigh, int uncompressBufSize){
        mBBFis = fis;
        mLeafHitItem = leafHitItem;
        mChromosomeMap = chromosomeMap;
        mIsLowToHigh = isLowToHigh;

        int bytesRead;
        mFileOffset = mLeafHitItem.getDataOffset();
        mLeafDataSize = mLeafHitItem.geDataSize();
        byte[] buffer = new byte[(int) mLeafDataSize];

        // read Wig data block into a buffer
        try {
            mBBFis.seek(mFileOffset);
            bytesRead = fis.read(buffer);

            // decompress if necessary - the buffer size is 0 for uncompressed data
            // Note:  BBFile Table C specifies a decompression buffer size
            if(uncompressBufSize > 0)
                mWigBuffer = BBCompressionUtils.decompress(buffer, uncompressBufSize);
            else
                mWigBuffer = buffer;    // use uncompressed read buffer directly
        }catch(IOException ex) {
             long itemIndex = mLeafHitItem.getItemIndex();
             log.error("Error reading Wig section for leaf item " + itemIndex, ex);
             String error = String.format("Error reading Wig section for leaf item %d\n", itemIndex);
             throw new RuntimeException(error, ex);
        }

        // initialize unread data size
        mRemDataSize = mWigBuffer.length;

        // use getWigData to extract data block items
    }

    /*
    *   Method reads all Wig data sections within the decompressed block buffer
    *   and returns those items in the chromosome selection region.
    *
    *   Parameters:
    *       selectionRegion - chromosome region for selecting Wig values
   *       contained - indicates selected data must be contained in selection region
    *           if true, else may intersect selection region
    *
    *   Returns:
    *      Wig sections in selected from the data block; else null for none selected.
    *
    * */
    public ArrayList<WigItem> getWigData(RPChromosomeRegion selectionRegion,
                                                boolean contained){

        mWigItemList = new ArrayList<WigItem>();
        
        for(int index = 0; mRemDataSize > 0; ++index) {

            // extract items in the Wig data section
            // Note: A RuntimeException is thrown if wig section is not read properly
            BigWigSection wigSection = new BigWigSection(mWigBuffer, mChromosomeMap, mIsLowToHigh, mLeafHitItem);

            // get wig section items and section bytes read
            int sectionBytes = wigSection.getSectionData(selectionRegion, contained, mWigItemList);

            // adjust remaining data block size
            mRemDataSize -= sectionBytes;
        }

        return mWigItemList;
    }

     public void print() {

        long itemIndex = mLeafHitItem.getItemIndex();
        log.info("Wig section data referenced by leaf item " + itemIndex);

        for(int index = 0; index <= mWigItemList.size(); ++index) {
            // BigWig sections print themselves
            mWigItemList.get(index).print();
         }
    }


}
