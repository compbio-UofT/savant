package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.LittleEndianInputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 18, 2010
 * Time: 12:51:25 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Containeer class for file statistics - BBFile Table DD
*
* */
public class BBTotalSummaryBlock {

    private static Logger log = Logger.getLogger(BBTotalSummaryBlock.class);

    public static final int TOTAL_SUMMARY_BLOCK_SIZE = 40;

    // defines the R+ Tree access
    private SeekableStream mBBFis;      // BBFile handle
    private long mSummaryBlockOffset;   // file offset to TotalSummaryBlock

    // File data statistics for calculating mean and standard deviation
    private long mBasesCovered;     // number of bases with data
    private float mMinVal;          // minimum value for file data
    private float mMaxVal;          // maximum value for file data
    private float mSumData;         // sum of all squares of file data values
    private float mSumSquares;      // sum of all squares of file data values

    /*
   *   Constructor for reading in TotalSummaryBlock from BBFile
   *
   *    Parameters:
   *    fis - file input stream handle
   *    fileOffset - file offset to TotalSummaryBlock
   *    isLowToHigh - indicates byte order is low to high if true, else is high to low
   * */
    public BBTotalSummaryBlock(SeekableStream fis, long fileOffset, boolean isLowToHigh)
    {

        LittleEndianInputStream lbdis = null;
        DataInputStream bdis = null;
        
        byte[] buffer = new byte[TOTAL_SUMMARY_BLOCK_SIZE];
        int bytesRead;

        // save the seekable file handle  and B+ Tree file offset
        mBBFis = fis;
        mSummaryBlockOffset = fileOffset;

        try {
            // Read TotalSummaryBlock header into a buffer
            mBBFis.seek(fileOffset);
            bytesRead = mBBFis.read(buffer);

            // decode header
            if(isLowToHigh)
                lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
            else
                bdis = new DataInputStream(new ByteArrayInputStream(buffer));

            // Get TotalSummaryBlcok information
            if(isLowToHigh){
                mBasesCovered = lbdis.readLong();
                mMinVal = lbdis.readFloat();
                mMaxVal = lbdis.readFloat();
                mSumData = lbdis.readFloat();
                mSumSquares = lbdis.readFloat();   
            }
            else {
                mBasesCovered = bdis.readLong();
                mMinVal = bdis.readFloat();
                mMaxVal = bdis.readFloat();
                mSumData = bdis.readFloat();
                mSumSquares = bdis.readFloat();
            }

        }catch(IOException ex) {
            log.error("Error reading Total Summary Block ", ex);
            throw new RuntimeException("Error reading Total Summary Block", ex);
            }

        }

    /*
    *   Constructor for filling in TotalSummaryBlock
    * */
    public BBTotalSummaryBlock(long basesCovered, float minVal, float maxVal,
                               float sumData, float sumSquares){

        mBasesCovered = basesCovered;
        mMinVal = minVal;
        mMaxVal = maxVal;
        mSumData = sumData;
        mSumSquares = sumSquares;

    }

    public static int getSummaryBlockSize() {
        return TOTAL_SUMMARY_BLOCK_SIZE;
    }

    public SeekableStream getMBBFis() {
        return mBBFis;
    }

    public long getSummaryBlockOffset() {
        return mSummaryBlockOffset;
    }

     public long getBasesCovered() {
        return mBasesCovered;
    }

    public float getMinVal() {
        return mMinVal;
    }

    public float getMaxVal() {
        return mMaxVal;
    }

    public float getSumData() {
        return mSumData;
    }

    public float getSumSquares() {
        return mSumSquares;
    }

    public void printTotalSummaryBlock(){

        // Table D - Zoom Level Header information
        log.info("BBFile TotalSummaryBlock (Table DD):");
        log.info("Number of bases covered= " + mBasesCovered);
        log.info("MinVal = " + mMinVal);
        log.info("MaxVal = " + mMaxVal);
        log.info("Sum of data values = "+ mSumData);
        log.info("Sum of squares values = " + mSumSquares);
    }

}
