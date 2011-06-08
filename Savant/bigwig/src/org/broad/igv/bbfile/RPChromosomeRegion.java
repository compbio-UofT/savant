package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 22, 2010
 * Time: 3:37:38 PM
    private int mStartChromID;  // starting chromosome in item
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for R+ Tree bounding rectangle regions
* */
public class RPChromosomeRegion {

    private static Logger log = Logger.getLogger(RPChromosomeRegion.class);

    private int mStartChromID;  // starting mChromosome in item
    private int mStartBase;     // starting base pair in item
    private int mEndChromID;    // ending mChromosome in item
    private int mEndBase;       // ending base pair in item

    /*
    *   Construct region from a specification.
    * */
    public RPChromosomeRegion(int startChromID, int startBase,
                                   int endChromID, int endBase){

        mStartChromID = startChromID;
        mStartBase = startBase;
        mEndChromID = endChromID;
        mEndBase = endBase;
    }
    
    /*
    *   Construct region from an existing region.
    * */
    public RPChromosomeRegion(RPChromosomeRegion region){

        mStartChromID = region.mStartChromID;
        mStartBase = region.mStartBase;
        mEndChromID = region.mEndChromID;
        mEndBase = region.mEndBase;
    }

    /*  
    *   Null region constructor for setting region members.
    **/
    public RPChromosomeRegion() {
        // members auto-inited
    }

    public int getStartChromID() {
        return mStartChromID;
    }

    public void setStartChromID(int mStartChromID) {
        this.mStartChromID = mStartChromID;
    }

    public int getStartBase() {
        return mStartBase;
    }

    public void setStartBase(int mStartBase) {
        this.mStartBase = mStartBase;
    }

    public int getEndChromID() {
        return mEndChromID;
    }

    public void setEndChromID(int mEndChromID) {
        this.mEndChromID = mEndChromID;
    }

    public int getEndBase() {
        return mEndBase;
    }

     public void setEndBase(int mEndBase) {
        this.mEndBase = mEndBase;
     }

    public void print(){

        log.info("Chromosome bounds:");
        log.info("StartChromID = " + mStartChromID);
        log.info("StartBase = " + mStartBase);
        log.info("EndChromID = " + mEndChromID);
        log.info("EndBase = " + mEndBase);
    }

    /*
    *   Comparator for mChromosome bounds is used to find relevant intervals and
    *   rank placement of node items. Returned value indicates relative
    *   positioning to supplied chromosome test region , and expands on normal
    *   comparator by indicating partial overlap in the extremes.
    *
    *   Returns:
    *       - 2 indicates that this region is completely disjoint below the test region
    *       -1 indicates this region intersects the test region from below
    *       0 indicates that this region is inclusive to the test region
    *       1 indicates this region intersects the test region from above
    *       2 indicates that this region is completely disjoint above the test region
    *
    *   Note: additional tests can be applied to determine intersection from above
    *   or below the test region and disjoint above or below the test region cases.
    * */
    public int compareRegions(RPChromosomeRegion testRegion) {

        // test if this region is contained by (i.e. subset of) testRegion region
        if(this.containedIn(testRegion))
            return 0;

        // test if  testRegion region is disjoint from above or below
        else if(this.disjointBelow(testRegion))
            return -2;
        else if(this.disjointAbove(testRegion))
            return 2;

        // Otherwise this region must intersect
        else if(this.intersectsBelow(testRegion))
            return -1;
        else if(this.intersectsAbove(testRegion))
            return 1;

        // unexpected condition is unknown
        return 3;
    }

    /*
    *   Method checks if test region matches this region
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region equals the test region: true or false
    * */
    public boolean equals(RPChromosomeRegion testRegion){

        if(mStartChromID == testRegion.mStartChromID && mStartBase == testRegion.mStartBase &&
                mEndChromID == testRegion.mEndChromID && mEndBase == testRegion.mEndBase)
            return true;
        else
            return false;
    }

    /*
    *   Method checks if test region contains this region;
    *   (i.e this region is subset oftest region).
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region is contained in the test region: true or false
    * */
    public boolean containedIn(RPChromosomeRegion testRegion){

        if(mStartChromID > testRegion.mStartChromID  ||
          (mStartChromID == testRegion.mStartChromID && mStartBase >= testRegion.mStartBase)){
            if(mEndChromID < testRegion.mEndChromID ||
              (mEndChromID == testRegion.mEndChromID && mEndBase <= testRegion.mEndBase) )
                return true;
            else
                return false;
        }
        else
            return false;
    }

    /*
    *   Method checks if this region intersects test region from below
    *
    *   Note: To be true, this region must have some part outside the test region
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region intersects the test region from below: true or false
    * */
    public boolean intersectsBelow(RPChromosomeRegion testRegion){

        // Only need to test if some part of this region is below and some within test region.
        if(mStartChromID < testRegion.mStartChromID ||
          (mStartChromID == testRegion.mStartChromID && mStartBase < testRegion.mStartBase)){
             if(mEndChromID > testRegion.mStartChromID ||
               (mEndChromID == testRegion.mStartChromID && mEndBase > testRegion.mStartBase))
                return true;
             else
                return false;
        }
        else
            return false;
    }

    /*
    *   Method checks if this region intersects test region from above.
    *
    *   Note: To be true, this region must have some part outside the test region
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region intersects the test region from above: true or false
    * */
    public boolean intersectsAbove(RPChromosomeRegion testRegion){

        // Only need to test if some part of this region is above and some within test region.
        if(mEndChromID > testRegion.mEndChromID ||
          (mEndChromID == testRegion.mEndChromID && mEndBase > testRegion.mEndBase)){
            if(mStartChromID < testRegion.mEndChromID ||
              (mStartChromID == testRegion.mEndChromID && mStartBase < testRegion.mEndBase))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    /*
    *   Method checks if this region is completely below test region.
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region is disjoint below the test region: true or false
    * */
    public boolean disjointBelow(RPChromosomeRegion testRegion){

        if(mEndChromID < testRegion.mStartChromID ||
                mEndChromID == testRegion.mStartChromID && mEndBase <= testRegion.mStartBase)
            return true;
        else
            return false;
    }

    /*
    *   Method checks if this region region is completely above test region.
    *
    *   Parameters:
    *       testRegion - chromosome selection region
    *
    *   Returns:
    *       This region is disjoint above the test region: true or false
    * */
     public boolean disjointAbove(RPChromosomeRegion testRegion){

        if(mStartChromID > testRegion.mEndChromID ||
                mStartChromID == testRegion.mEndChromID && mStartBase >= testRegion.mEndBase)
            return true;
        else
            return false;
    }

    /*
    *   Method computes the extremes between this region and the test region
    *
    *   Parameters:
    *       testRegion - chromosome region to compare against this region
    *
    *   Returns:
    *       new chromosome region of extremes
    * */
    public RPChromosomeRegion getExtremes(RPChromosomeRegion testRegion){
        RPChromosomeRegion newRegion = new RPChromosomeRegion(this);

        // update node bounds
        if(testRegion.mStartChromID < newRegion.mStartChromID ||
            (testRegion.mStartChromID == newRegion.mStartChromID &&
                    testRegion.mStartBase < newRegion.mStartBase)){
            newRegion.mStartChromID = testRegion.mStartChromID;
            newRegion.mStartBase = testRegion.mStartBase;
       }

       if(testRegion.mEndChromID > newRegion.mEndChromID ||
            (testRegion.mEndChromID == newRegion.mEndChromID &&
                    testRegion.mEndBase > newRegion.mEndBase)){
            newRegion.mEndChromID = testRegion.mEndChromID;
            newRegion.mEndBase = testRegion.mEndBase;
       }

        return newRegion;
    }

     /*
    *   Method returns the the upper extreme region from this region.
    *
    *   Parameters:
    *       startChromID - new ending chromosome ID
    *       startBase - new ending base
    *
    *   Note: extreme start location must be a part of this region
    *
    *   Returns:
    *       chromosome region remaining after subtracting off the trim region
    * */
    public RPChromosomeRegion getUpperExtreme(int startChromID, int startBase){

        // screen disjoint extreme
        if(startChromID < mStartChromID || startChromID > mEndChromID)
            return null;
        else if(startChromID == mStartChromID && startBase < mStartBase)
            return null;

        RPChromosomeRegion newRegion = new RPChromosomeRegion();

        // trim upper extremity
        newRegion.mStartChromID = startChromID;
        newRegion.mStartBase = startBase;
        newRegion.mEndChromID = this.mEndChromID;
        newRegion.mEndBase = this.mEndBase;

        return newRegion;
    }

    /*
    *   Method returns the the lower extreme region from this region.
    *
    *   Parameters:
    *       endChromID - new ending chromosome ID
    *       endBase - new ending base
    *
    *   Note: extreme end location must be a part of this region
    *
    *   Returns:
    *       chromosome region remaining after subtracting of the trim region
    * */
    public RPChromosomeRegion getLowerExtreme(int endChromID, int endBase){

        // screen disjoint extreme
        if(endChromID < mStartChromID || endChromID > mEndChromID)
            return null;
        else if(endChromID == mEndChromID && endBase > mEndBase)
            return null;

        RPChromosomeRegion newRegion = new RPChromosomeRegion();

        // trim upper extremity
        newRegion.mStartChromID = this.mStartChromID;
        newRegion.mStartBase = this.mStartBase;
        newRegion.mEndChromID = endChromID;
        newRegion.mEndBase = endBase;

        return newRegion;
    }

}
