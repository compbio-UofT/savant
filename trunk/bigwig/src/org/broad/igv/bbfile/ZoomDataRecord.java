package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 18, 2010
 * Time: 2:24:44 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for holding zoom level statistics, BBFile Table P.
*
* */
public class ZoomDataRecord {

    private static Logger log = Logger.getLogger(ZoomDataRecord.class);

    public static final int RECORD_SIZE = 32;

    private int mZoomLevel;         // zoom level associated with data
    private int mRecordNumber;      // record number

    // chromosome region statistics (useful for calculating mean and standard deviation)
    private String mChromName;      // chromosome/contig name
    private int mChromId;           // Numerical ID for mChromosome/contig
    private int mChromStart;        // starting base position  (from 0)
    private int mChromEnd;          // ending base position
    private int mValidCount;        // number of bases with data
    private float mMinVal;          // minimum value for file data
    private float mMaxVal;          // maximum value for file data
    private float mSumData;         // sum of all squares of file data values
    private float mSumSquares;      // sum of squares of file data values

    /*
    *   Constructor for filling in zoom data record class.
    *
    *   Parameters:
    *       zoomLevel - level of zoom
    *       recordNumber - record sequence number of multiple zoom level records
    *       chromName - chromosome/contig name
    *       chromId - mChromosome ID
    *       chromstart - starting base for zoom data region
    *       chromEnd - ending base for zoom data region
     *      validCount - number of bases in the region for which there is data
     *      minVal - minimum value in region
     *      maxVal - maximum value in region
     *      sumData - sum of all region data
     *      sumSquares - sum of the squares of all region data
     *
    * */
    public ZoomDataRecord(int zoomLevel, int recordNumber, String chromName, int chromId, int chromStart, int chromEnd,
            int validCount, float minVal, float maxVal, float sumData, float sumSquares ){

        mZoomLevel = zoomLevel;
        mRecordNumber = recordNumber;
        mChromName = chromName;
        mChromId = chromId;
        mChromStart = chromStart;
        mChromEnd = chromEnd;
        mValidCount = validCount;
        mMinVal = minVal;
        mMaxVal = maxVal;
        mSumData = sumData;
        mSumSquares = sumSquares;
    }

    public int getZoomLevel() {
        return mZoomLevel;
    }

    public int getRecordNumber() {
        return mRecordNumber;
    }

     public String getmChromName() {
        return mChromName;
    }

     public int getChromId() {
        return mChromId;
    }

    public int getChromStart() {
        return mChromStart;
    }

    public int getChromEnd() {
        return mChromEnd;
    }

    public int getValidCount() {
        return mValidCount;
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

    public void print(){

        // Table P - zoom data record
       log.info("Zoom data record (Table DD) number " + mRecordNumber +
               " for zoom level " + mZoomLevel);
        log.info("ChromName = " + mChromName);
        log.info("ChromId = " + mChromId);
        log.info("ChromStart = " + mChromStart);
        log.info("ChromEnd = " + mChromEnd);
        log.info("ValidCount = " + mValidCount);
        log.info("MinVal = " + mMinVal);
        log.info("MaxVal = " + mMaxVal);
        log.info("Sum of data values = " + mSumData);
        log.info("Sum of squares values = " + mSumSquares);
    }
}

