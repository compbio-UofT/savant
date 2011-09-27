package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.LittleEndianInputStream;
import org.broad.tribble.util.SeekableStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Owner
 * Date: May 5, 2010
 * Time: 8:27:56 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for reading and storing a block of zoom level data records.
* */
public class ZoomDataBlock {

    private static Logger log = Logger.getLogger(ZoomDataBlock.class);

    // Bed data block access variables   - for reading in bed records from a file
    private SeekableStream mBBFis;  // file input stream handle
    private long mFileOffset;       // data block file offset
    private long mDataBlockSize;    // byte size for data block specified in the R+ leaf
    private boolean mIsLowToHigh;   // if true, data is low to high byte order; else high to low

    // defines the zoom level source chromosomes
    private int mZoomLevel;         // zoom level for the R+ chromosome data location tree
    private HashMap<Integer, String> mChromosomeMap;  // map of chromosome ID's and corresponding names
    private RPTreeLeafNodeItem mLeafHitItem;   //R+ leaf item with chromosome region and file data location

    // Provides uncompressed byte stream data reader
    private byte[] mZoomBuffer;  // buffer containing leaf block data uncompressed
    private int mRemDataSize;   // number of unread decompressed data bytes
    private long mDataSizeRead;     // number of bytes read from the decompressed mWigBuffer

    // byte stream readers
    private LittleEndianInputStream mLbdis = null;    // low to high byte stream reader
    private DataInputStream mBdis = null;       // high to low byte stream reader

    // Bed data extraction members
    private ArrayList<ZoomDataRecord> mZoomDataList; // array of zoom level data
    private int mRecordsSelected;    // number of zoom records selected from data section

    /*
    *   Constructor for Bed data block reader.
    *
    *   Parameters:
    *       zoomLevel - zoom level for data block
    *       fis - file input stream handle
    *       leafItem - R+ tree leaf item containing block data file location
    *       chromIDTree - B+ chromosome index tree returns chromosome ID's for names
    *       isLowToHigh - byte order is low to high if true; else high to low
    *       uncompressBufSize - byte size for decompression buffer; else 0 for uncompressed
    * */
    public ZoomDataBlock(int zoomLevel, SeekableStream fis, RPTreeLeafNodeItem leafHitItem,
            HashMap<Integer, String> chromosomeMap, boolean isLowToHigh, int uncompressBufSize){

        mZoomLevel = zoomLevel;
        mBBFis = fis;
        mLeafHitItem = leafHitItem;
        mChromosomeMap = chromosomeMap;
        mIsLowToHigh = isLowToHigh;

        int bytesRead;
        mFileOffset = mLeafHitItem.getDataOffset();
        mDataBlockSize = mLeafHitItem.geDataSize();
        byte[] buffer = new byte[(int) mDataBlockSize];

        // read Bed data block into a buffer
        try {
            mBBFis.seek(mFileOffset);
            bytesRead = fis.read(buffer);

            // decompress if necessary - the buffer size is 0 for uncomressed data
            // Note:  BBFile Table C specifies a decompression buffer size
            if(uncompressBufSize > 0)
              mZoomBuffer = BBCompressionUtils.decompress(buffer, uncompressBufSize);
            else
              mZoomBuffer = buffer;    // use uncompressed read buffer directly

       }catch(IOException ex) {
             long itemIndex = mLeafHitItem.getItemIndex();
            log.error("Error reading Zoom level " +  mZoomLevel + " data for leaf item "
                    + itemIndex, ex);
            String error = String.format("Error reading zoom level %d data for leaf item %d\n",
                    mZoomLevel, itemIndex);
             throw new RuntimeException(error, ex);
       }

       // wrap the bed buffer as an input stream
        if(mIsLowToHigh)
            mLbdis = new LittleEndianInputStream(new ByteArrayInputStream(mZoomBuffer));
        else
            mBdis = new DataInputStream(new ByteArrayInputStream(mZoomBuffer));

        // initialize unread data size
        mRemDataSize = mZoomBuffer.length;

        // use method getZoomData to extract block data
    }

    /*
    *   Method returns all zoom level data within the decompressed block buffer
    *
    *   Parameters:
    *       selectionRegion - chromosome region for selecting zoom level data records
    *       contained - indicates selected data must be contained in selection region
    *           if true, else may intersect selection region
    *
    *   Returns:
    *      zoom data records in the data block
    *
    *   Note: Remaining bytes to data block are used to determine end of reading
    *   since a zoom record count for the data block is not known.
    * */
    public ArrayList<ZoomDataRecord> getZoomData(RPChromosomeRegion selectionRegion,
                                                boolean contained) {

        int chromID, chromStart, chromEnd, validCount;
        float minVal, maxVal, sumData, sumSquares;
        int itemHitValue;
        int recordNumber = 0;

        // allocate the bed feature array list
        mZoomDataList = new ArrayList<ZoomDataRecord>();

        // check if all leaf items are selection hits
        RPChromosomeRegion itemRegion = new RPChromosomeRegion( mLeafHitItem.getChromosomeBounds());
        int leafHitValue = itemRegion.compareRegions(selectionRegion);

        try {
            //for(int index = 0; mRemDataSize >= ZoomDataRecord.RECORD_SIZE; ++index) {
            for(int index = 0; mRemDataSize > 0; ++index) {
                recordNumber = index + 1;

                if(mIsLowToHigh) {  // buffer stream arranged low to high bytes
                    chromID = mLbdis.readInt();
                    chromStart = mLbdis.readInt();
                    chromEnd = mLbdis.readInt();
                    validCount = mLbdis.readInt();
                    minVal = mLbdis.readFloat();
                    maxVal = mLbdis.readFloat();
                    sumData = mLbdis.readFloat();
                    sumSquares = mLbdis.readFloat();
                }
                else{ // buffer stream arranged high to low bytes
                    chromID = mBdis.readInt();
                    chromStart = mBdis.readInt();
                    chromEnd = mBdis.readInt();
                    validCount = mBdis.readInt();
                    minVal = mBdis.readFloat();
                    maxVal = mBdis.readFloat();
                    sumData = mBdis.readFloat();
                    sumSquares = mBdis.readFloat();
                }

                if(leafHitValue == 0) {     // contained leaf region always a hit
                    String chromName =  mChromosomeMap.get(chromID);
                    ZoomDataRecord zoomRecord = new ZoomDataRecord(mZoomLevel, recordNumber, chromName,
                        chromID, chromStart, chromEnd, validCount, minVal, maxVal, sumData, sumSquares);
                    mZoomDataList.add(zoomRecord);
                }

                else {      // test for hit
                    itemRegion = new RPChromosomeRegion(chromID, chromStart, chromID, chromEnd);
                    itemHitValue = itemRegion.compareRegions(selectionRegion);

                    // itemHitValue < 2 for intersection; itemHitValue == 0 for is contained
                    if(!contained && Math.abs(itemHitValue) < 2 || itemHitValue == 0) {
                        String chromName =  mChromosomeMap.get(chromID);
                        ZoomDataRecord zoomRecord = new ZoomDataRecord(mZoomLevel, recordNumber, chromName,
                            chromID, chromStart, chromEnd, validCount, minVal, maxVal, sumData, sumSquares);
                        mZoomDataList.add(zoomRecord);
                    }
                }

                // compute data block remainder fom size item read
                mRemDataSize -= ZoomDataRecord.RECORD_SIZE;
            }

        }catch(IOException ex) {
            long itemIndex = mLeafHitItem.getItemIndex();
            log.error("Read error for zoom level " + mZoomLevel + " leaf item " + itemIndex);

            // accept this as an end of block condition unless no items were read
            if(recordNumber == 1)
                throw new RuntimeException("Read error for zoom level " + mZoomLevel +
                        " leaf item " + itemIndex);
        }

        return mZoomDataList;
    }

    public void print() {
        long leafIndex = mLeafHitItem.getItemIndex();
        log.info("Zoom Level " + mZoomLevel + "data for leaf item " + leafIndex + ":");

        for(int index = 0; index <= mZoomDataList.size(); ++index) {
            // zoom data records print themselves
            mZoomDataList.get(index).print();
         }
    }

}
