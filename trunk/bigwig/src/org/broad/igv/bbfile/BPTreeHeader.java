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
 * Date: Jan 8, 2010
 * Time: 3:50:08 PM
 * To change this template use File | Settings | File Templates.
 */
/*
 *  Container class for BBFile B+ Tree header.
 *  B+ Tree Header can be constructed by reading values in from a BBFile
  *  (  Table E), or by assigning the values in a constructor.
 *
 * */
public class  BPTreeHeader {

    private static Logger log = Logger.getLogger(BPTreeHeader.class);

    static public final int BPTREE_HEADER_SIZE = 32;

    static public final int BPTREE_MAGIC_LTH = 0x78CA8C91;
    static public final int BPTREE_MAGIC_HTL = 0x918CCA78;

    private SeekableStream mBBFis;  // BBFile handle
    private long mHeaderOffset;     // BBFile file offset for mChromosome tree
    private boolean mHeaderOK;      // B+ Tree header OK?
    
    // Chromosome B+ Tree Header - Table E
    private int mMagic;        // magic number identifies it as B+ header
    private int mBlockSize;    // number of children per block
    private int mKeySize;      // min # of charcter bytes for mChromosome name
    private int mValSize;      // size of (bigWig) values - currently 8
    private long mItemCount;   // number of chromosomes/contigs in B+ tree
    private long mReserved;    // Currently 0

   /*
   *    Constructor for reading in a B+ tree header a from a file input stream.
   *
   *    Parameters:
   *        fis - file input handle
   *        fileOffset - file offset to the B+ tree header
   *        isLowToHigh - indicates byte order is low to high, else is high to low
   * */
    public BPTreeHeader(SeekableStream fis, long fileOffset, boolean isLowToHigh) {

        long itemsCount;

       // save the seekable file handle  and B+ Tree file offset
       mBBFis = fis;
       mHeaderOffset = fileOffset;

       // Note: a bad B+ Tree header will result in false returned
       mHeaderOK =  readHeader(mBBFis, mHeaderOffset, isLowToHigh);
    }

    public SeekableStream getBBFis() {
        return mBBFis;
    }

    public static int getHeaderSize() {
        return BPTREE_HEADER_SIZE;
    }

    public long getHeaderOffset() {
        return mHeaderOffset;
    }

     public boolean isHeaderOK() {
        return mHeaderOK;
    }

    public int getMagic() {
        return mMagic;
    }

    public int getBlockSize() {
        return mBlockSize;
    }

    public int getKeySize() {
        return mKeySize;
    }

    public int getValSize() {
        return mValSize;
    }

    public long getItemCount() {
        return mItemCount;
    }

    public long getReserved() {
        return mReserved;
    }

    // prints out the B+ Tree Header
     public void print() {

        // Chromosome B+ Tree  Header - BBFile Table E
        if(mHeaderOK)
            log.info("B+ Tree Header was read from file location " + mHeaderOffset);
        log.info(" Magic ID =" + mMagic);
        log.info(" Block size = " + mBlockSize);
        log.info(" Key size = " + mKeySize);
        log.info(" Indexed value size = " + mValSize);
        log.info(" Item Count = " + mItemCount);
        log.info(" Reserved = " + mReserved);
    }
    
   /*
   * Reads in the B+ Tree Header.
   * Returns status of B+ tree header read; true if read, false if not.
   * */
    private boolean readHeader(SeekableStream fis, long fileOffset, boolean isLowToHigh) {

        LittleEndianInputStream lbdis;
        DataInputStream bdis;

         byte[] buffer = new byte[BPTREE_HEADER_SIZE];
         int bytesRead;
    
        try {
            // Read B+ tree header into a buffer
            mBBFis.seek(fileOffset);
            bytesRead = mBBFis.read(buffer);
        
            // decode header
            if(isLowToHigh){
                lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));

                // check for a valid B+ Tree Header
                mMagic = lbdis.readInt();

                if(mMagic != BPTREE_MAGIC_LTH)
                    return false;

                // Get mChromosome B+ header information
                mBlockSize = lbdis.readInt();
                mKeySize = lbdis.readInt();
                mValSize = lbdis.readInt();
                mItemCount = lbdis.readLong();
                mReserved = lbdis.readLong();
            }
            else {
                bdis = new DataInputStream(new ByteArrayInputStream(buffer));

                // check for a valid B+ Tree Header
                mMagic = bdis.readInt();

                if(mMagic != BPTREE_MAGIC_HTL)
                    return false;

                // Get mChromosome B+ header information
                mBlockSize = bdis.readInt();
                mKeySize = bdis.readInt();
                mValSize = bdis.readInt();
                mItemCount = bdis.readLong();
                mReserved = bdis.readLong();

            }

        }catch(IOException ex) {
           log.error("Error reading B+ tree header " + ex);
           throw new RuntimeException("Error reading B+ tree header \n", ex);
            }

        // success
         return true;
    }

}

