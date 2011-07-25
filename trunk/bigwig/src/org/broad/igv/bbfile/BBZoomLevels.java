package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;

import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 17, 2009
 * Time: 12:36:35 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class which reads BBFile zoom information into ArrayLists.
*
*   1) Reads in Zoom Level Header Table D into Zoom Headers and
*       loads them into the ZoomLevelHeader array, one for each zoom level.
*
*   2) Reads in Table O overall zoom level format as referenced
*       by dataOffset in Table D.
*
*   Note: Zoom levels count from Table C is validated by BBZoomLevelHeader,
*   which will throw a RuntimeException if header is not found.
*
* */
public class BBZoomLevels {

    private static Logger log = Logger.getLogger(BBZoomLevels.class);

    // defines the zoom headers access
    private SeekableStream mBBFis;       // BBFile handle
    private long mZoomHeadersOffset;     // BBFile first zoom header offset
    private int mZoomLevelsCount;        // BB File header Table C specified zoom levels

    // zoom level headers - Table D , one for each zoom level
    // Note: array size determines how many zoom levels actually read
    private ArrayList<BBZoomLevelHeader> mZoomLevelHeaders;  // zoom level headers

    // zoom level data formats - BBFile Table O
    private ArrayList<BBZoomLevelFormat> mZoomLevelFormatList;

    // zoom level R+ trees - one per level
    private ArrayList<RPTree> mZoomLevelRPTree;

    /*
   *  constructor   - reads zoom level headers and data format from file I/O stream
   *      for all zoom levels and acts as a container for zoom data records.
   *
   *  Parameters:
   *      fis - file input stream handle
   *      fileOffset - file byte location for zoom level headers
   *      zoomLevels - count of zoom levels from BBFile Table C
   *      isLowToHigh - boolean flag indicates if values are arranged low to high bytes.
   *      uncompressBufSize - byte size of the buffer to use for decompression
   * */

    public BBZoomLevels(SeekableStream fis, long fileOffset, int zoomLevels,
                        boolean isLowToHigh, int uncompressBufSize){
        int zoomLevel;
        int zoomHeadersRead;
        long zoomDataOffset;
        long zoomIndexOffset;

        // save the seekable file handle and zoom zoomLevel headers file offset
        mBBFis = fis;
        mZoomHeadersOffset = fileOffset;
        mZoomLevelsCount = zoomLevels;
        
        // Note: a bad zoom header will result in a 0 count returned
        zoomHeadersRead =  readZoomHeaders(mZoomHeadersOffset, zoomLevels, isLowToHigh);

        if(zoomHeadersRead > 0){

            // create zoom level data format containers
            mZoomLevelFormatList = new ArrayList<BBZoomLevelFormat>();

            // for each zoom zoomLevel, get associated zoom data format
            for(int index = 0; index < zoomHeadersRead; ++index) {

                zoomLevel = index + 1;

                // Zoom dataOffset (from Table D) is file location for zoomCount (Table O)
                // Note: This dataOffset is zoomFormatLocation in BBZoomLevelFormat.
                zoomDataOffset = mZoomLevelHeaders.get(index).getDataOffset();

                // R+ zoom index offset (Table D) marks end of zoom data in the
                // zoom level format (Table O)
                long dataSize = mZoomLevelHeaders.get(index).getIndexOffset() - zoomDataOffset
                        - BBZoomLevelFormat.ZOOM_FORMAT_HEADER_SIZE;

                // get zoom zoomLevel data records  - zoomDataOffset references zoomCount in Table O
                // Note: zoom zoomLevel data records read their own data
                BBZoomLevelFormat zoomLevelData = new BBZoomLevelFormat(zoomLevel, mBBFis, zoomDataOffset,
                        dataSize, isLowToHigh, uncompressBufSize);

                mZoomLevelFormatList.add(zoomLevelData);
            }

            // create zoom level R+ tree containers
            mZoomLevelRPTree = new ArrayList<RPTree>();

            // for each zoom zoomLevel, get associated R+ tree
            for(int index = 0; index < zoomHeadersRead; ++index) {

                // Zoom indexOffset (from Table D) is file location
                // for Table O zoomIndex for R+ tree zoom data
                zoomIndexOffset = mZoomLevelHeaders.get(index).getIndexOffset();

                // get Zoom Data R+ Tree (Tables K, L, M, N): exists for zoom levels
                RPTree zoomRPTree = new RPTree(mBBFis, zoomIndexOffset, isLowToHigh, uncompressBufSize);

                if(zoomRPTree.getNodeCount() > 0)
                    mZoomLevelRPTree.add(zoomRPTree);
            }
        }
    }

    /*
    *   Method returns the file input stream handle
    *
    *   Returns:
    *       file input stream handle
    * */
    public SeekableStream getFileStream() {
        return mBBFis;
    }

    /*
    *   Method returns the BBFile's first zoom header file offset.
    *
    *   Note zoom headers immediately follow the BBFile header (Table C)
    *
    *   Returns:
    *       first zoom header file offset
    * */
    public long getZoomHeadersOffset() {
        return mZoomHeadersOffset;
    }

    /*
    *   Method returns the number of zoom level headers found.
    *
    *   Note Should match zoomLevels in the BBFile header (Table C)
    *
    *   Returns:
    *      number of zoom level headers found
    * */
    public int getZoomHeaderCount() {
        return mZoomLevelHeaders.size();
    }

    /*
    *   Method returns the zoom level headers.
    *
    *   Returns:
    *      zoom level headers
    * */
    public ArrayList<BBZoomLevelHeader> getZoomLevelHeaders() {
        return mZoomLevelHeaders;
    }

    /*
    *   Method returns the zoom level header for specified level.
    *
    *   Parameters:
    *       level - zoom level; level starts at 1
    *
    *   Returns:
    *      Zoom level header for specified level; or null for bad zoom level.
    * */
    public BBZoomLevelHeader getZoomLevelHeader(int level) {
        if(level < 1 || level > mZoomLevelsCount)
        return null;

        return mZoomLevelHeaders.get(level - 1);
    }

    /*
    *   Method returns the zoom level formats for zoom data.
    *
    *   Returns:
    *      zoom level formats for zoom data
    * */
    public ArrayList<BBZoomLevelFormat> getZoomLevelFormats(){
        return mZoomLevelFormatList;
    }

    /*
    *   Method returns the R+ index tree for the specified zoom level.
    *
    *   Parameters:
    *       level - zoom level; level starts at 1
    *
    *   Returns:
    *      R+ index tree for the specified zoom level; or null for bad zoom level
    * */
    public RPTree getZoomLevelRPTree(int level) {
        if(level < 1 || level > mZoomLevelsCount)
            return null;

        return mZoomLevelRPTree.get(level - 1);
    }

    // prints out the zoom level header information
    public void printZoomHeaders() {

        // note if successfully read - should always be correct
        if(mZoomLevelHeaders.size() == mZoomLevelsCount)
            log.info("Zoom level headers read for " + mZoomLevelsCount + " levels:");

        else
            log.error("Zoom level headers not successfully read for "
                    + mZoomLevelsCount + "levels.");

        for( int index = 0; index < mZoomLevelHeaders.size(); ++index) {

            // zoom level headers print themselves
            mZoomLevelHeaders.get(index).print();
        }

    }

    /*
    * Reads in all the Zoom Headers.
    *
    *   Parameters:
    *       fileOffset - File byte location for first zoom level header
    *       zoomLevels - count of zoom levels to read in
    *       isLowToHigh - indicate byte order is lwo to high, else is high to low
    *
    *   Returns:
    *       Count of zoom levels headers read, or 0 for failure to find the
    *       header information.
    * */
    private int readZoomHeaders(long fileOffset, int zoomLevels, boolean isLowToHigh) {
        int level = 0;
        BBZoomLevelHeader zoomLevelHeader;

        if(zoomLevels < 1)
            return 0;

        // create zoom headers and data containers
        mZoomLevelHeaders = new ArrayList<BBZoomLevelHeader>();

        // get zoom header information for each zoom levelsRead
        for(int index = 0; index < zoomLevels; ++index)  {
            level = index + 1;

            // read zoom level header - read error is returned as Runtime Exception
            zoomLevelHeader = new BBZoomLevelHeader(mBBFis, fileOffset, level, isLowToHigh);

            mZoomLevelHeaders.add(zoomLevelHeader);

            fileOffset += BBZoomLevelHeader.ZOOM_LEVEL_HEADER_SIZE;
        }

        return level;
    }

}
