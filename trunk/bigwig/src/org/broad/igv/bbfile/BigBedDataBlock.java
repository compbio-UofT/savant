package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.LittleEndianInputStream;
import org.broad.tribble.util.SeekableStream;

import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.HashMap;


/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 26, 2010
 * Time: 12:18:32 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for reading and storing a block of bed data items.
* */
public class BigBedDataBlock {

 private static Logger log = Logger.getLogger(BigBedDataBlock.class);

    // Bed data block access variables   - for reading in bed records from a file
    private SeekableStream mBBFis;  // file input stream handle
    private long mFileOffset;       // Bed data block file offset
    private long mDataBlockSize;     // byte size for data block specified in the R+ leaf
    private boolean mIsLowToHigh;   // if true, data is low to high byte order; else high to low

    // defines the bigBed/bigWig source chromosomes
    private HashMap<Integer, String> mChromosomeMap;  // map of chromosome ID's and corresponding names
    private RPTreeLeafNodeItem mLeafHitItem;   // R+ tree leaf item containing data block location

    // Provides uncompressed byte stream data reader
    private byte[] mBedBuffer;  // buffer containing leaf block data uncompressed
    private int mRemDataSize;   // number of unread data bytes
    private long mDataSizeRead;     // number of bytes read from the decompressed mWigBuffer

    // byte stream readers
    private LittleEndianInputStream mLbdis;    // low to high byte stream reader
    private DataInputStream mBdis;       // high to low byte stream reader

    // Bed data extraction members
    private ArrayList<BedFeature> mBedFeatureList; // array of BigBed data
    private int mItemsSelected;    // number of Bed features selected from this section

    /*
    *   Constructor for Bed data block reader.
    *
    *   Parameters:
    *       fis - file input stream handle
    *       leafItem - R+ tree leaf item containing chromosome region and file data location
    *       chromIDTree - B+ chromosome index tree returns chromosome ID's for names
    *       isLowToHigh - byte order is low to high if true; else high to low
    *       uncompressBufSize - byte size for decompression buffer; else 0 for uncompressed
    * */
    public BigBedDataBlock(SeekableStream fis, RPTreeLeafNodeItem leafHitItem,
            HashMap<Integer, String> chromosomeMap, boolean isLowToHigh, int uncompressBufSize){
        mBBFis = fis;
        mLeafHitItem = leafHitItem;
        mChromosomeMap = chromosomeMap;
        mIsLowToHigh = isLowToHigh;

        int bytesRead;
        mDataBlockSize = mLeafHitItem.geDataSize();
        byte[] buffer = new byte[(int) mDataBlockSize];

        mFileOffset = mLeafHitItem.getDataOffset();

        // read Bed data block into a buffer
        try {
            mBBFis.seek(mFileOffset);
            bytesRead = fis.read(buffer);

            // decompress if necessary - the buffer size is 0 for uncompressed data
            // Note:  BBFile Table C specifies a decompression buffer size
            if(uncompressBufSize > 0)
              mBedBuffer = BBCompressionUtils.decompress(buffer, uncompressBufSize);
            else
              mBedBuffer = buffer;    // use uncompressed read buffer directly

       }catch(IOException ex) {
            long itemIndex = mLeafHitItem.getItemIndex();
            String error = String.format("Error reading Bed data for leaf item %d \n", itemIndex);
            log.error(error, ex);
            throw new RuntimeException(error, ex);
       }

        // wrap the bed buffer as an input stream
        if(mIsLowToHigh)
            mLbdis = new LittleEndianInputStream(new ByteArrayInputStream(mBedBuffer));
        else
            mBdis = new DataInputStream(new ByteArrayInputStream(mBedBuffer));

        // initialize unread data size
        mRemDataSize = mBedBuffer.length;

        // use methods getBedData or getNextFeature to extract block data
    }

    /*
    *   Method returns all Bed features within the decompressed block buffer
    *
    *   Parameters:
    *       selectionRegion - chromosome region for selecting Bed features
    *       contained - indicates selected data must be contained in selection region
    *           if true, else may intersect selection region
    *
    *   Returns:
    *      Bed feature items in the data block
    *
    *   Note: Remaining bytes to data block are used to determine end of reading
    *   since a zoom record count for the data block is not known.
    * */
    public ArrayList<BedFeature> getBedData(RPChromosomeRegion selectionRegion,
                                                boolean contained) {
        int itemNumber = 0;
        int chromID, chromStart, chromEnd;
        String restOfFields;
        int itemHitValue;

        // chromID + chromStart + chromEnd + rest 0 byte
        // 0 byte for "restOfFields" is always present for bed data
        int minItemSize = 3 * 4 + 1;

        // allocate the bed feature array list
        mBedFeatureList = new ArrayList<BedFeature>();

        // check if all leaf items are selection hits
        RPChromosomeRegion itemRegion = new RPChromosomeRegion( mLeafHitItem.getChromosomeBounds());
        int leafHitValue = itemRegion.compareRegions(selectionRegion);
        
        try {
            for(int index = 0; mRemDataSize > 0; ++index) {
                itemNumber = index + 1;

                // read in BigBed item fields - BBFile Table I
                if(mIsLowToHigh){
                    chromID = mLbdis.readInt();
                    chromStart= mLbdis.readInt();
                    chromEnd = mLbdis.readInt();
                    restOfFields = mLbdis.readString();
                }
                else{
                    chromID = mBdis.readInt();
                    chromStart= mBdis.readInt();
                    chromEnd = mBdis.readInt();
                    restOfFields = mBdis.readUTF();
                }

                if(leafHitValue == 0) {     // contained leaf region items always added
                    String chromosome = mChromosomeMap.get(chromID);
                    BedFeature bbItem = new BedFeature(itemNumber, chromosome,
                         chromStart, chromEnd, restOfFields);
                    mBedFeatureList.add(bbItem);
                }
                else {                      // test for hit
                    itemRegion = new RPChromosomeRegion(chromID, chromStart, chromID, chromEnd);
                    itemHitValue = itemRegion.compareRegions(selectionRegion);

                    // abs(itemHitValue) == 1 for intersection; itemHitValue == 0 for contained
                    if(!contained && Math.abs(itemHitValue) < 2 ||
                            itemHitValue == 0) {
                        // add bed feature to item selection list
                        String chromosome = mChromosomeMap.get(chromID);
                        BedFeature bbItem = new BedFeature(itemNumber, chromosome,
                             chromStart, chromEnd, restOfFields);
                        mBedFeatureList.add(bbItem);
                    }
                }

                // compute data block remainder from size of item read
                // todo: check that restOfFields.length() does not also include 0 byte terminator
                mRemDataSize -= minItemSize + restOfFields.length();
            }

        }catch(IOException ex) {
            log.error("Read error for Bed data item " + itemNumber);

            // accept this as an end of block condition unless no items were read
            if(itemNumber == 1)
                throw new RuntimeException("Read error for Bed data item " + itemNumber);
        }

        return mBedFeatureList;
    }

    public void print() {

        log.info("BigBed data for " + mBedFeatureList.size() + " items");

        for(int index = 0; index <= mBedFeatureList.size(); ++index) {
            // BigBed data items print themselves
            mBedFeatureList.get(index).print();
         }
    }
}
