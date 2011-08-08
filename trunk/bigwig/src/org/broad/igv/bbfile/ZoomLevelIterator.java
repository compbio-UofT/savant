package org.broad.igv.bbfile;

import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Apr 16, 2010
 * Time: 4:19:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZoomLevelIterator {

    private static Logger log = Logger.getLogger(ZoomDataBlock.class);

    // zoom level for zoom data
    private int mZoomLevel;

    //specification of chromosome selection region
    private RPChromosomeRegion mSelectionRegion;  // selection region for iterator
    private boolean mIsContained; // if true, features must be fully contained by extraction region
    private RPChromosomeRegion mHitRegion;  // hit selection region for iterator

    // File access variables for reading zoom level data block
    private SeekableStream mBBFis;  // file input stream handle
    private BPTree mChromIDTree;    // B+ chromosome index tree
    private RPTree mZoomDataTree;  // R+ zoom data locations tree

    // chromosome region extraction items
     private ArrayList<RPTreeLeafNodeItem> mLeafHitList; // array of leaf hits for selection region items
    private HashMap<Integer, String> mChromosomeMap;  // map of chromosome ID's and corresponding names
    private int mLeafItemIndex;   // index of current leaf item being processed from leaf hit list
    RPTreeLeafNodeItem mLeafHitItem;   // leaf item being processed by next

    // current zoom level block being processed
    ZoomDataBlock mZoomDataBlock;  // holds data block of zoom level records decompressed
    private boolean mDataBlockRead;  // flag indicates successful read of data block for current leaf item
    ArrayList<ZoomDataRecord> mZoomRecordList; // array of selected zoom data records
    private int mZoomRecordIndex;    // index of next zoom data record from the list

    /**
     * Constructs a zoom level iterator over the specified chromosome region
     *
     * Parameters:
     *      fis - file input stream handle
     *      chromIDTree - B+ index tree returns chromId for chromosome name key
     *      zoomLevelTree - zoom level R+ chromosome index tree
     *      zoomLevel - zoom level represented by the R+ tree
      *     selectionRegion - chromosome region for selection of Bed feature extraction
     *      consists of:
     *          startChromID - ID of start chromosome
     *          startBase - starting base position for features
     *          endChromID - ID of end chromosome
     *          endBase - starting base position for features
     *      contained - specifies bed features must be contained by region, if true;
     *          else return any intersecting region features
     */
      public ZoomLevelIterator(SeekableStream fis, BPTree chromIDTree, RPTree zoomDataTree,
                int zoomLevel, RPChromosomeRegion selectionRegion, boolean contained) {

        // check for valid selection region
        if(selectionRegion == null)
            throw new RuntimeException("Error: ZoomLevelIterator selection region is null\n");

        mBBFis = fis;
        mChromIDTree = chromIDTree;
        mZoomDataTree = zoomDataTree;
        mZoomLevel = zoomLevel;
        mSelectionRegion = selectionRegion;
        mIsContained = contained;

        // set up hit list and read in the first data block
        int hitCount = getHitRegion(selectionRegion, contained);
        if(hitCount == 0)   // no hits - no point in fetching data
            throw new RuntimeException("No zoom data found in the selection region");

        // Ready for next() data extraction
    }

    /*
     *  Method returns status on a "next record" being available.
     *
     *  Return:
     *      true if a "next record" exists; else false.
     *
     *  Note: If "next" method is called for a false condition,
     *      an UnsupportedOperationException will be thrown.
     * */
     public boolean hasNext() {

        // first check if current data block can be read for next
        if(mZoomRecordIndex < mZoomRecordList.size())
            return true;

        // need to fetch next data block
        else if(mLeafItemIndex < mLeafHitList.size())
            return true;

         else
            return false;
    }

    /**
     *  Method returns the current bed feature and advances to the next bed record.
     *
     *  Returns:
     *      Bed feature for current BigBed data record.
     *
     *  Note: If "next" method is called when a "next item" does not exist,
     *      an UnsupportedOperationException will be thrown.
    */
    public ZoomDataRecord next() {

        // Is there a need to fetch next data block?
        if(mZoomRecordIndex < mZoomRecordList.size())
            return(mZoomRecordList.get(mZoomRecordIndex++));

        // attempt to get next leaf item data block
        else {
            int nHits = getHitRegion(mSelectionRegion, mIsContained);

            if(nHits > 0){
                // Note: getDataBlock initializes bed feature index to 0
                return(mZoomRecordList.get(mZoomRecordIndex++)); // return 1st Data Block item
            }
            else{
                String result = String.format("Failed to find data for zoom region (%d,%d,%d,%d)\n",
                    mHitRegion.getStartChromID(),  mHitRegion.getStartBase(),
                        mHitRegion.getEndChromID(), mHitRegion.getEndBase());
                log.error(result);

                return null;
                //throw new NoSuchElementException(result);
            }
        }

    }

    public void remove() {
        throw new UnsupportedOperationException("Remove iterator item is not supported yet.");
    }

    // ************ ZoomLevelIterator specific methods *******************
     /*
    *   Method returns the zoom level assigned to the iterator.
    *
    *   Returns:
    *       Number of leaf node hits allowed at a time
    * */
    public int getZoomLevel() {
           return mZoomLevel;
     }

    /*
    *   Method returns the iterator selection region.
    * */
    public RPChromosomeRegion getSelectionRegion() {
        return mSelectionRegion;
    }

/*
    *   Method provides the iterator with a new selection region.
    *
    *   Parameters:
    *      selectionRegion - chromosome region for selection of Bed feature extraction
    *      consists of:
    *          startChromID - ID of start chromosome
    *          startBase - starting base position for features
    *          endChromID - ID of end chromosome
    *          endBase - starting base position for features
    *      contained - specifies bed features must be contained by region, if true;
    *          else return any intersecting region features
    *
    *   Returns:
    *       number of chromosome regions found in the selection region
    * */
    public int setSelectionRegion(RPChromosomeRegion selectionRegion,
                                                 boolean contained) {
        mSelectionRegion = selectionRegion;
        mIsContained = contained;

        // set up hit list and first data block read
        mLeafHitList = null;    // Must nullify existing hit list first!
        int hitCount = getHitRegion(selectionRegion, contained);
        if(hitCount == 0)   // no hits - no point in fetching data
            throw new RuntimeException("No wig data found in the selection region");

        // Ready for next() data extraction

        return hitCount;
    }


    /*
    *   Method returns if bed items must be completely contained in
    *   the selection region.
    *
    *   Returns:
    *       Boolean indicates items must be contained in selection region if true,
    *       else may intersect the selection region if false
    * */
    public boolean isContained() {
        return mIsContained;
    }

    /*
    *   Method returns the Big Binary file input stream handle.
    *
    *   Returns:
    *       File input stream handle
    * */
    public SeekableStream getBBFis() {
        return mBBFis;
    }

    /*
    *   Method returns the B+ chromosome index tree used for identifying
    *   chromosome ID's used to specify R+ chromosome data locations.
    *
    *   Returns:
    *       B+ chromosome index tree
    * */
    public BPTree getChromosomeIDTree() {
        return mChromIDTree;
    }

    /*
    *   Method returns the R+ zoom data data tree used for identifying
    *   chromosome data locations for the selection region.
    *
    *   Returns:
    *       R+ chromosome data locations tree
    * */
    public RPTree getZoomDataTree() {
        return mZoomDataTree;
    }

    /*
    *   Method finds the chromosome data hit items for the current hit selection region,
    *   and loads first hit data.
    *
    *   Parameters:
    *       hitRegion - selection region for extracting hit items
    *       contained - indicates hit items must contained in selection region if true;
    *       and if false, may intersect selection region
    *   Note: The selection region will be limited to accommodate  mMaxLeafHits; which terminates
    *       selection at the leaf node at which maxLeafHits is reached. Total number of selected
    *       items may exceed maxLeafHits, but only by the number of leaves in the cutoff leaf node.
    *
    *   Returns:
    *       number of R+ chromosome data hits
    * */
    private int getHitRegion(RPChromosomeRegion hitRegion, boolean contained) {

        int hitCount = 0;

        // check if new hit list is needed
        // Note: getHitList will reset mLeafItemIndex to 0, the beginning of new hit list
        if(mLeafHitList == null ){   //|| mLeafItemIndex >= mLeafHitList.size()){
            hitCount = getHitList(hitRegion, contained);
            if(hitCount == 0)
                return 0;   // no hit data found
        }
        else {
            hitCount =  mLeafHitList.size() - mLeafItemIndex;
            if(hitCount == 0)
                return 0;   // hit list exhausted
        }

        // Perform a block read for starting base of selection region - use first leaf hit
        mDataBlockRead = getDataBlock(mLeafItemIndex++);

        // try next item - probably intersection issue
        // Note: recursive call until a block is valid or hit list exhuasted
        if(!mDataBlockRead)
            hitCount = getHitRegion(hitRegion, contained);

        return hitCount;
    }

    /*
    *   Method finds the R+ chromosome data tree items for the hit region.
    *
    *   Parameters:
    *       hitRegion - selection region for extracting hit items
    *       contained - indicates hit items must contained in selection region if true;
    *       and if false, may intersect selection region
    *
    *   Note: The selection region will be limited to accommodate  mMaxLeafHits; which terminates
    *       selection at the leaf node at which maxLeafHits is reached. Total number of selected
    *       items may exceed maxLeafHits, but only by the number of leaves in the cutoff leaf node.
    *
    *   Returns:
    *       number of R+ chromosome data hits
    * */
    private int getHitList(RPChromosomeRegion hitRegion, boolean contained) {

        // hit list for hit region; subject to mMaxLeafHits limitation
         mLeafHitList = mZoomDataTree.getChromosomeDataHits(hitRegion, contained);

        // check if any leaf items were selected
        int nHits = mLeafHitList.size();
        if(nHits == 0)
            return 0;   // no data hits found
        else
             mLeafItemIndex = 0;    // reset hit item index to start of list

        // find hit bounds
        int startChromID = mLeafHitList.get(0).getChromosomeBounds().getStartChromID();
        int startBase = mLeafHitList.get(0).getChromosomeBounds().getStartBase();
        int endChromID = mLeafHitList.get(nHits-1).getChromosomeBounds().getEndChromID();
        int endBase = mLeafHitList.get(nHits-1).getChromosomeBounds().getEndBase();

        // save hit region definition; not currently used but useful for debug
        mHitRegion = new  RPChromosomeRegion(startChromID, startBase, endChromID, endBase);

        return nHits;
    }

/*
    *   Method sets up a decompressed data block of zoom data records for iteration.
    *
    *   Parameters:
    *       leafItemIndex - leaf item index in the hit list referencing the data block
    *
    *   Returns:
    *       Successful Zoom data block set up: true or false.
    * */
    private boolean getDataBlock(int leafItemIndex){

        // check for valid data block
        if(mLeafHitList == null || leafItemIndex >= mLeafHitList.size())
                return false;

        // Perform a block read for indexed leaf item
        mLeafHitItem = mLeafHitList.get(leafItemIndex);

        // get the chromosome names associated with the hit region ID's
        int startChromID = mLeafHitItem.getChromosomeBounds().getStartChromID();
        int endChromID = mLeafHitItem.getChromosomeBounds().getEndChromID();
        mChromosomeMap = mChromIDTree.getChromosomeIDMap(startChromID, endChromID);

        boolean isLowToHigh = mZoomDataTree.isIsLowToHigh();
        int uncompressBufSize = mZoomDataTree.getUncompressBuffSize();

        // decompress leaf item data block for feature extraction
        mZoomDataBlock = new ZoomDataBlock(mZoomLevel, mBBFis, mLeafHitItem, mChromosomeMap,
                        isLowToHigh, uncompressBufSize);

        // get data block zoom data record list and set next index to first item
        mZoomRecordList =  mZoomDataBlock.getZoomData(mSelectionRegion, mIsContained);
        mZoomRecordIndex = 0;

        // data block items available for iterator
        if(mZoomRecordList.size() > 0)
            return true;
        else
            return false;
    }

}
