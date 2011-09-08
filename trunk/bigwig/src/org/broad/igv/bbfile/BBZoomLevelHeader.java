package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.LittleEndianInputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 17, 2009
 * Time: 4:36:21 PM
 *
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for holding zoom level header information, BBFile Table D.
*
*   Constructed either from BBFile read or by load of header values.
*
* */
public class BBZoomLevelHeader {

    private static Logger log = Logger.getLogger(BBZoomLevelHeader.class);

    static public final int ZOOM_LEVEL_HEADER_SIZE = 24;

    // Defines the Big Binary File (BBFile) access
    private SeekableStream mBBFis;          // BBFile input stream handle
    private long mZoomLevelHeaderOffset;    // file location for zoom level header
    int mZoomLevel;     // the zoom level for this information

    // zoom level header information - BBFile Table D
    private int mReductionLevel;   // number of bases summerized
    private int mReserved;         // reserved, currently 0
    private long mDataOffset;      // file position of zoom data
    private long mIndexOffset;     // file position for index of zoomed data

    /*
    *   Constructor reads zoom level header
    *
    *   Parameters:
    *       fis - File input stream handle
    *       fileOffset - file byte position for zoom header
    *       zoomLevel - level of zoom
    *       isLowToHigh - indicates byte order is low to high, else is high to low
    * */
    public BBZoomLevelHeader(SeekableStream fis, long fileOffset, int zoomLevel,
                             boolean isLowToHigh){

        mBBFis = fis;
        mZoomLevelHeaderOffset = fileOffset;
        mZoomLevel = zoomLevel;

        readZoomLevelHeader(mZoomLevelHeaderOffset, mZoomLevel, isLowToHigh);
    }
    /*
    *   Constructor loads zoom level header according to parameter specification.
    *
    *   Parameters: (as defined above)
    * */
    public BBZoomLevelHeader(int zoomLevel, int reductionLevel, int reserved,
                             long dataOffset, long indexOffset){
        mZoomLevel = zoomLevel;
        mReductionLevel = reductionLevel;
        mReserved = reserved;
        mDataOffset = dataOffset;
        mIndexOffset = indexOffset;
    }

    /*
    *   Method returns the zoom level.
    *
    *   Returns:
    *       zoom level
    * */
    public int getZoomLevel() {
        return mZoomLevel;
    }

    /*
    *   Method returns the reduction level for the zoom level.
    *
    *   Returns:
    *       reduction level
    * * */
    public int getReductionLevel() {
        return mReductionLevel;
    }

    /*
    *   Method returns the reserved value.
    *
    *   Returns:
    *       reserved value
    * * */
    public int getReserved() {
        return mReserved;
    }

    /*
    *   Method returns the zoom level data file location.
    *
    *   Returns:
    *       zoom level data file location
    * */
    public long getDataOffset() {
        return mDataOffset;
    }

    /*
    *   Method returns the zoom level R+ index tree file location.
    *
    *   Returns:
    *       R+ index tree file location
    * */
    public long getIndexOffset() {
        return mIndexOffset;
    }

    /*
    *   Method prints the zoom level header info.
    * */
    public void print(){

        // Table D - Zoom Level Header information
        log.info("Zoom level " + mZoomLevel + " header Table D: ");
        log.info("Number of zoom level bases = " + mReductionLevel);
        log.info("Reserved = " + mReserved);
        log.info("Zoom data offset = " + mDataOffset);
        log.info("Zoom index offset = " + mIndexOffset);
    }

    /*
    *   Reads zoom level header information into class data members.
    *
    *   Parameters:
    *       fileOffset - Byte position in fle for zoom header
    *       zoomLevel - level of zoom
    *       isLowToHigh - indicate byte order is low to high, else is high to low
    * */
    private void readZoomLevelHeader(long fileOffset, int zoomLevel, boolean isLowToHigh) {

       LittleEndianInputStream lbdis = null;
       DataInputStream bdis = null;

        byte[] buffer = new byte[ZOOM_LEVEL_HEADER_SIZE];
        int bytesRead;

            try {

            // Read zoom header into a buffer
            mBBFis.seek(fileOffset);
            bytesRead = mBBFis.read(buffer);

            // decode header
            if(isLowToHigh)
                lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
            else
                bdis = new DataInputStream(new ByteArrayInputStream(buffer));

            // Get zoom level information
            if(isLowToHigh){
                mReductionLevel = lbdis.readInt();
                mReserved = lbdis.readInt();
                mDataOffset = lbdis.readLong();
                mIndexOffset = lbdis.readLong();
            }
            else {
                mReductionLevel = bdis.readInt();
                mReserved = bdis.readInt();
                mDataOffset = bdis.readLong();
                mIndexOffset = bdis.readLong();
            }

        }catch(IOException ex) {
            log.error("Error reading zoom level header: " + zoomLevel, ex);
            throw new RuntimeException("Error reading zoom header " + zoomLevel, ex);
        }
    }


}
