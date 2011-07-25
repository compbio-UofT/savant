package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Apr 5, 2010
 * Time: 4:00:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class WigItem {

    private static Logger log = Logger.getLogger(WigItem.class);

    private int mItemIndex;         // wig section item index number
    private String mChromosome;     // mChromosome name
    private int mStartBase;         // mStartBase base position for feature
    private int mEndBase;           // mEndBase base position for feature
    private float mWigValue;        // wig value

    public WigItem(int itemIndex, String chromosome, int startBase, int endBase, float wigValue){

        mItemIndex = itemIndex;
        mChromosome = chromosome;
        mStartBase = startBase;
        mEndBase = endBase;
        mWigValue = wigValue;
    }

    public int getItemNumber(){
        return mItemIndex;
    }

    public String getChromosome() {
        return mChromosome;
    }

    public int getStartBase() {
        return mStartBase;
    }

    public int getEndBase() {
        return mEndBase;
    }

    public float getWigValue() {
        return mWigValue;
    }

     public void print(){
       log.info("Wig item index " + mItemIndex);
       log.info("mChromosome name: " + mChromosome);
       log.info("mChromosome start base = " + mStartBase);
       log.info("mChromosome end base = " + mEndBase);
       log.info("Wig value: \n" + mWigValue);
   }
}
