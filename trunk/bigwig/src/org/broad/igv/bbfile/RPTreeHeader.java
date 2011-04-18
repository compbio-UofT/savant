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
 * Date: Jan 14, 2010
 * Time: 3:59:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class RPTreeHeader {

    private static Logger log = Logger.getLogger(RPTreeHeader.class);

    public final int RPTREE_HEADER_SIZE = 48;

    public final int RPTREE_MAGIC_LTH = 0x2468ACE0;
    public final int RPTREE_MAGIC_HTL = 0xE0AC6824;

    // defines the R+ Tree access
    private SeekableStream mBBFis;      // BBFile handle
    private long mRPTreeOffset;         // BBFile file offset for mChromosome region tree
    private boolean mHeaderOK;          // R+ Tree header read OK

    // R+ Tree header - Table K
    private int mMagic;             // magic number identifies it as B+ header
    private int mBlockSize;         // number of children per block
    private long mItemCount;        // number of chromosomes/contigs in B+ tree
    private int mStartChromID;      // ID of the first mChromosome in item
    private int mStartBase;         // Position of first base in item
    private int mEndChromID;        // ID of the first mChromosome in item
    private int mEndBase;           // Position of first base in item
    private long mEndFileOffset;    // file position marking mEndBase of data
    private int mItemsPerSlot;      // number of items per leaf
    private long mReserved;         // Currently 0

    // constructor   - reads from file input stream
    /*
    *   Constructor
    *
    *   Parameters:
    *       fis - file input stream handle
    *       fileOffset - file offset to the RP tree header
    *       isLowToHigh - if true, indicates low to high byte order, else high to low
    * */
    public RPTreeHeader(SeekableStream fis, long fileOffset, boolean isLowToHigh) {

        long itemsCount;

       // save the file input handle  and B+ Tree file offset
       mBBFis = fis;
       mRPTreeOffset =  fileOffset;

       // Note: a bad R+ Tree header will result in false returned
       mHeaderOK =  readHeader(mBBFis, mRPTreeOffset, isLowToHigh);

    }

    public boolean isHeaderOK() {
        return mHeaderOK;
    }

    public int getHeaderSize() {
        return RPTREE_HEADER_SIZE;
    }

    public long getTreeOffset() {
        return mRPTreeOffset;
    }

    public int getMagic() {
        return mMagic;
    }

    public int getBlockSize() {
        return mBlockSize;
    }

    public long getItemCount() {
        return mItemCount;
    }

    public int getStartChromID() {
        return mStartChromID;
    }

    public int getStartBase() {
        return mStartBase;
    }

    public int getEndChromID() {
        return mEndChromID;
    }

    public int getEndBase() {
        return mEndBase;
    }

    public long getMEndFileOffset() {
        return mEndFileOffset;
    }

    public int getItemsPerSlot() {
        return mItemsPerSlot;
    }

    public long getReserved() {
        return mReserved;
    }

// prints out the B+ Tree Header
public void print() {

   // note if read successfully
   if(mHeaderOK){
       log.info("R+ tree header has " + RPTREE_HEADER_SIZE + " bytes.");
       log.info("R+ tree header magic = " + mMagic);
   }
   else {
       log.info("R+ Tree header is unrecognized type, header magic = " + mMagic);
       return;
   }

   // Table E - Chromosome B+ Tree  Header
   log.info("R+ Tree file offset = " +  mRPTreeOffset);
   log.info("magic = " + mMagic);
   log.info("Block size = " + mBlockSize);
   log.info("ItemCount = " + mItemCount);
   log.info("StartChromID = " + mStartChromID);
   log.info("StartBase = " + mStartBase);
   log.info("EndChromID = " + mEndChromID);
   log.info("EndBase = " + mEndBase);
   log.info("EndFileOffset = " + mEndFileOffset);
   log.info("ItemsPerSlot = " + mItemsPerSlot);
   log.info("Reserved = " + mReserved);
   }

  /*
  * Reads in the R+ Tree Header.
  *
  * Returns status of for tree header read; true if read, false if not.
  * */
   private boolean readHeader(SeekableStream fis, long fileOffset, boolean isLowToHigh){

   LittleEndianInputStream lbdis;
   DataInputStream bdis;
      
    byte[] buffer = new byte[RPTREE_HEADER_SIZE];
    int bytesRead;

   try {
       // Read R+ tree header into a buffer
       mBBFis.seek(fileOffset);
       bytesRead = mBBFis.read(buffer);

       // decode header
       if(isLowToHigh){
           lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
           mMagic = lbdis.readInt();

           // check for a valid B+ Tree Header
           if(mMagic != RPTREE_MAGIC_LTH)
               return false;

           // Get mChromosome B+ header information
           mBlockSize = lbdis.readInt();
           mItemCount = lbdis.readLong();
           mStartChromID = lbdis.readInt();
           mStartBase = lbdis.readInt();
           mEndChromID = lbdis.readInt();
           mEndBase = lbdis.readInt();
           mEndFileOffset = lbdis.readLong();
           mItemsPerSlot = lbdis.readInt();
           mReserved = lbdis.readInt();
       }
       else {
           bdis = new DataInputStream(new ByteArrayInputStream(buffer));

           // check for a valid B+ Tree Header
           mMagic = bdis.readInt();

           if(mMagic != RPTREE_MAGIC_HTL)
               return false;

           // Get mChromosome B+ header information
           mBlockSize = bdis.readInt();
           mItemCount = bdis.readLong();
           mStartChromID = bdis.readInt();
           mStartBase = bdis.readInt();
           mEndChromID = bdis.readInt();
           mEndBase = bdis.readInt();
           mEndFileOffset = bdis.readLong();
           mItemsPerSlot = bdis.readInt();
           mReserved = bdis.readInt();
       }

   }catch(IOException ex) {
           log.error("Error reading R+ tree header " + ex);
           throw new RuntimeException("Error reading R+ tree header ", ex);
       }

   // success
    return true;
   }


}
