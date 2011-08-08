
/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Nov 20, 2009
 * Time: 3:49:14 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 *  Container class defines the header information for BigBed and BigWig files
*/
package org.broad.igv.bbfile;
//import org.broad.igv.util.SeekableStream;
//import org.broad.igv.tdf.LittleEndianInputStream;
//import org.broad.tribble.util.SeekableStream;
import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

//import net.sf.samtools.util.SeekableStream;

/*
*   Container class for holding the BBFile header information, Table C .
**/
public class BBFileHeader
{

     private static Logger log = Logger.getLogger(BBFileHeader.class);

    // defines bigBed/bigwig Header Format types
    static public final int BBFILE_HEADER_SIZE = 64;

    static public final int BIGWIG_MAGIC_LTH = 0x888FFC26; // BigWig Magic Low to High
    static public final int BIGWIG_MAGIC_HTL = 0x26FC8F66; // BigWig Magic High to Low

    static public final int BIGBED_MAGIC_LTH = 0x8789F2EB; // BigBed Magic Low to High
    static public final int BIGBED_MAGIC_HTL = 0xEBF28987; // BigBed Magic High to Low

    // defines the bigBed/bigWig source file access
    private String mPath;               // bigBed file/pathname
    private SeekableStream mBBFis;      // BBFile I/O stream handle
    private long mFileHeaderOffset;     // file offset for file header

    private boolean mIsHeaderOK;        // File header read correctly?
    private boolean mIsLowToHigh;       // flag indicates values represented low to high bytes
    private boolean mIsBigBed;          // flag indicates file is BigBed format
    private boolean mIsBigWig;          // flag indicates file is BigWig format;
    private boolean mIsStreamSource;    // constructed from input stream

    // BBFile Header items - Table C:
    // mMagic number (4 bytes) indicates file type and byte order :
    // 0x888FFC26 for bigWig, little endian if swapped
    // 0x8789F2EB for bigBed, little endian if swapped
    private int mMagic;                // 4 byte mMagic Number
    private short mVersion;            // 2 byte version ID; currently 3
    private short mZoomLevels;         // 2 byte count of zoom sumary levels
    private long mChromTreeOffset;     // 8 byte offset to mChromosome B+ Tree index
    private long mFullDataOffset;      // 8 byte offset to unzoomed data dataCount
    private long mFullIndexOffset;     // 8 byte offset to R+ Tree index of items
    private short mFieldCount;         // 2 byte number of fields in bed. (0 for bigWig)
    private short mDefinedFieldCount;  // 2 byte number of fields that are bed fields
    private long mAutoSqlOffset;       // 8 byte offset to 0 terminated string with .as spec
    private long mTotalSummaryOffset;  // 8 byte offset to file summary data block
    private int mUncompressBuffSize;  // 4 byte maximum size for decompressed buffer
    private long mReserved;            // 8 bytes reserved for future expansion. Currently 0

    // constructor reads BBFile header from an input stream
    public BBFileHeader(String path, SeekableStream fis, long fileOffset) {

        // showing  input source
        mIsStreamSource = true;

        // save the path and seekable file handle
        mPath = path;
        mBBFis = fis;
        mFileHeaderOffset = fileOffset;

        // read in BBFile header
        mIsHeaderOK = readBBFileHeader(mFileHeaderOffset);

    }

    /*
    *   Constructor loads BBFile header class from parameter specifications.
    *
    *   Parameters: (as defined above)
    * */
    public BBFileHeader(
            int magic,
            short version,
            short zoomLevels,
            long chromTreeOffset,
            long fullDataOffset,
            long fullIndexOffset,
            short fieldCount,
            short definedFieldCount,
            long autoSqlOffset,
            long totalSummaryOffset,
            int uncompressBuffSize,
            long reserved) {

        // showing  input source not specified here
        // set
        mIsStreamSource = false;
        mMagic = magic;

        // Note: may want to validate the rest of the fields as well
        if(isBigWig() || isBigBed())
            mIsHeaderOK = true;

        mVersion = version;
        mZoomLevels = zoomLevels;
        mChromTreeOffset = chromTreeOffset;
        mFullDataOffset = fullDataOffset;
        mFullIndexOffset = fullIndexOffset;
        mFieldCount = fieldCount;
        mDefinedFieldCount = definedFieldCount;
        mAutoSqlOffset = autoSqlOffset;
        mTotalSummaryOffset = totalSummaryOffset;
        mUncompressBuffSize = uncompressBuffSize;
        mUncompressBuffSize = uncompressBuffSize;
        mReserved = reserved;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String mSourcePath) {
        this.mPath = mSourcePath;
    }

    public SeekableStream getFileStream() {
        return mBBFis;
    }

    public void setFileStream(SeekableStream mBBFis) {
        this.mBBFis = mBBFis;
    }

     public long getMFileHeaderOffset() {
        return mFileHeaderOffset;
    }

    // ************** return file info ****************

    public boolean isHeaderOK() {
           return mIsHeaderOK;
       }

     public boolean isStreamSource() {
        return mIsStreamSource;
    }

     public boolean isLowToHigh() {
        return mIsLowToHigh;
    }

    public boolean isBigBed() {
        return mIsBigBed;
    }

    public boolean isBigWig() {
        return mIsBigWig;
    }

    public int getFileHeaderSize() {
        return BBFILE_HEADER_SIZE;
    }

    // ************* return header items ****************

    public int getMagic() {
        return mMagic;
    }

     public short getVersion() {
        return mVersion;
     }

     public short getZoomLevels() {
         return mZoomLevels;
     }

     public long getChromosomeTreeOffset() {
         return mChromTreeOffset;
     }

     public long getFullDataOffset() {
         return mFullDataOffset;
     }

     public long getFullIndexOffset() {
         return mFullIndexOffset;
     }

     public short getFieldCount() {
         return mFieldCount;
     }

     public short getDefinedFieldCount() {
         return mDefinedFieldCount;
     }

     public long getAutoSqlOffset() {
         return mAutoSqlOffset;
     }

     public long getTotalSummaryOffset() {
         return mTotalSummaryOffset;
     }

     public int getUncompressBuffSize() {
         return mUncompressBuffSize;
     }

     public long getReserved() {
         return mReserved;
     }

    public void print() {

        // note if header read successfully from a source
        if(mIsStreamSource) {

            if(mIsHeaderOK){
                if(isBigWig())
                    log.info("BigWig file " + mPath + ", file header at location " + mFileHeaderOffset);
                else if(isBigBed())
                    log.info("BigBed file " + mPath + ", file header at location " + mFileHeaderOffset);
            }
            else {
               log.info("BBFile " + mPath + "  with bad magic = " +   mMagic +
                       " from file header location " + mFileHeaderOffset);
               return; // bad read - remaining header items not interpreted
            }
        }
        // otherwise header was constructed without reading
        else {
            if(isBigWig())
                log.info("BBFile " + mPath + " is a BigWig file, header magic = " + mMagic);
            else if(isBigBed())
                log.info("BBFile " + mPath + " is a BigBed file, header magic = " + mMagic);
        }

        // header fields
        log.info("BBFile header magic = " + mMagic);
        log.info("Version = " + mVersion);
        log.info("Zoom Levels = "+  mZoomLevels);
        log.info("Chromosome Info B+ tree offset = " +mChromTreeOffset);
        log.info("Data Block offset = " + mFullDataOffset);
        log.info("Chromosome Data R+ tree offset = " + mFullIndexOffset);
        log.info("Bed fields count = " + mFieldCount);
        log.info("Bed defined fields count = " + mDefinedFieldCount);
        log.info("AutoSql Offset = " + mAutoSqlOffset);
        log.info("Total Summary offset = " + mTotalSummaryOffset);
        log.info("Maximum uncompressed buffer size = " + mUncompressBuffSize);
        log.info("m_reserved = " + mReserved);
    }

     /*
     *  Reads in BBFile header information.
     *
     *  Returns:
     *      Success status flag is true for successfully read header,
     *      or is false for a read error.
    **/
    private boolean readBBFileHeader(long fileOffset) {

        BBFileHeader bbHeader = null;
        LittleEndianInputStream lbdis = null;
        DataInputStream bdis = null;

         byte[] buffer = new byte[BBFILE_HEADER_SIZE];
         int bytesRead;

        try {
            // Read bigBed header into a buffer
            mBBFis.seek(fileOffset);
            bytesRead = mBBFis.read(buffer);

            // decode header - determine byte order from first 4 bytes
            // first assume byte order is low to high
            mIsLowToHigh = true;
            lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
            mMagic = lbdis.readInt();

            // check for a valid bigBed or bigWig file
            if(mMagic == BIGWIG_MAGIC_LTH)
                mIsBigWig = true;
            else if(mMagic == BIGBED_MAGIC_LTH)
                mIsBigBed = true;

            // try high to low byte order
            else {
                bdis = new DataInputStream(new ByteArrayInputStream(buffer));
                mMagic = bdis.readInt();

                // check for a valid bigBed or bigWig file
                if(mMagic == BIGWIG_MAGIC_HTL)
                    mIsBigWig = true;
                else if(mMagic == BIGBED_MAGIC_HTL)
                    mIsBigBed = true;

                else
                    return false;   // can't identify BBFile type

                // success - set order high to low
                mIsLowToHigh = false;
            }

            // Get header information
            if(mIsLowToHigh) {
                mVersion = lbdis.readShort();
                mZoomLevels = lbdis.readShort();
                mChromTreeOffset = lbdis.readLong();
                mFullDataOffset = lbdis.readLong();
                mFullIndexOffset = lbdis.readLong();
                mFieldCount = lbdis.readShort();
                mDefinedFieldCount = lbdis.readShort();
                mAutoSqlOffset = lbdis.readLong();
                mTotalSummaryOffset = lbdis.readLong();
                mUncompressBuffSize = lbdis.readInt();
                mReserved = lbdis.readLong();
            }
            else
            {
                mVersion = bdis.readShort();
                mZoomLevels = bdis.readShort();
                mChromTreeOffset = bdis.readLong();
                mFullDataOffset = bdis.readLong();
                mFullIndexOffset = bdis.readLong();
                mFieldCount = bdis.readShort();
                mDefinedFieldCount = bdis.readShort();
                mAutoSqlOffset = bdis.readLong();
                mTotalSummaryOffset = bdis.readLong();
                mUncompressBuffSize = bdis.readInt();
                mReserved = bdis.readLong();
            }

        }catch(IOException ex) {
            throw new RuntimeException("Error reading file header for " + mPath, ex);
        }

         // file header was read properly
         return true;
    }

}  // mEndBase of class BBFileHeader
