/**          +-
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Nov 20, 2009
 * Time: 3:43:04 PM
 * To change this template use File | Settings | File Templates.
 */
package org.broad.igv.bbfile;

import org.broad.tribble.util.LittleEndianInputStream;
import org.apache.log4j.Logger;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.SeekableFileStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/*
*   Broad Institute Interactive Genome Viewer Big Binary File (BBFile) Reader
*   -   File reader for UCSC BigWig and BigBed file types.
*
*   Notes:   Table entries refer to Jim Kent of UCSC's document description:
*           "BigWig and BigBed: Enabling Browsing of Large Distributed Data Sets",
*           November 2009.
*
*           The overall binary file layout is defined in Table B of the document.
*
*   BBFile Reader sequences through this binary file layout:
*
*   1) Reads in BBFile Header Table C and determine if file is a valid Big Bed or Big Wig
*      binary file type.
*
*   2) Reads in Zoom Header Tables D if zoom data is present, as defined by zoomLevels
*       in Table C, one for each zoom level.
*
*   3) Reads in  the AutoSQL block Table B if present, as referenced by autoSqlOffset in Table C.
*
*   4) Reads in Total Summary Block Table DD if present, as referenced by
*       TotalSummaryOffset in Table C.
*
*   5) Reads in B+ Tree Header Chromosome Index Header Table E, as referenced
*       by chromosomeTreeOffset in Table C.
*
*   6)Reads in B+ Tree Nodes indexing mChromosome ID's for mChromosome regions;
*       Table F for node type (leaf/child), Table G for leaf items,
*       Table H for child node items.
*
*   7) Reads in R+ Tree Chromosome ID Index Header Table K.
*
*   8) Reads in R+ Tree Nodes indexing of data arranged by mChromosome ID's;
*       Table L for node type (leaf or child), Table M for leaf items,
*       Table N for child node items.
*
*   9) Verifies Data Count of data records, as referenced by fullDataOffset in Table C
*
*   10) References data count records of data size defined in Table M of R+ Tree index
*       for all leaf items in the tree.
*
*   11) Reads in zoom level format Table O for each zoom level comprised of
*       zoom record count followed by that many Table P zoom statistics records,
*       followed by an R+ tree of zoom data locations indexed as in Tables L, M, and N.
*
*   12) Returns information on chromosome name keys and chromosome data regions.
*
*   13) Provides iterators using chromosome names and data regions to extract
*       zoom data, Wig data, and Bed data.
* 
* */

public class BBFileReader {

    public static final long BBFILE_HEADER_OFFSET = 0;

    private static Logger log = Logger.getLogger(BBFileReader.class);

    // Defines the Big Binary File (BBFile) access
    private String mBBFilePath;         // BBFile source file/pathname
    private SeekableStream mBBFis;      // BBFile input stream handle
    private long mFileOffset;           // file offset for next item to be read

    private BBFileHeader mBBFileHeader; // Big Binary file header
    private int mDataCount;             // Number of data records in the file - Table BB
    private boolean mIsLowToHigh;       // BBFile binary data format: low to high or high to low
    private int mUncompressBufSize;     // buffer byte size for data decompression; 0 for uncompressed

    // AutoSQL String defines custom BigBed formats
    private long mAutoSqlOffset;
    private String mAutoSqlFormat;

    // This section defines the zoom items if zoom data exists
    private int mZoomLevelCount;       // number of zoom levels defined
    private long mZoomLevelOffset;      // file offset to zoom level headers
    private BBZoomLevels mZoomLevels;   // zoom level headers and data locations

    // Total Summary Block - file statistical info
    private long mTotalSummaryBlockOffset;
    private BBTotalSummaryBlock mTotalSummaryBlock;

    // B+ tree
    private long mChromIDTreeOffset; // file offset to mChromosome index B+ tree
    private BPTree mChromosomeIDTree;     // Container for the mChromosome index B+ tree

    // R+ tree
    private long mChromDataTreeOffset;  // file offset to mChromosome data R+ tree
    private RPTree mChromosomeDataTree;     // Container for the mChromosome data R+ tree

    public BBFileReader(String path) {
        log.info("Opening BBFile source  " + path);
        mBBFilePath = path;

        // Verify the path
        try {
            mBBFis = new SeekableFileStream(new File(path));

            // read in file header
            mFileOffset = BBFILE_HEADER_OFFSET;
            mBBFileHeader = new BBFileHeader(path, mBBFis, mFileOffset);
            mBBFileHeader.print();

            if (!mBBFileHeader.isHeaderOK()) {
                log.error("BBFile header is unrecognized type, header magic = " +
                        mBBFileHeader.getMagic());
                throw new RuntimeException("Error reading BBFile header for: " + path);
            }

            // get data characteristics
            mIsLowToHigh = mBBFileHeader.isLowToHigh();
            mUncompressBufSize = mBBFileHeader.getUncompressBuffSize();

            // update file offset past BBFile header
            mFileOffset += BBFileHeader.BBFILE_HEADER_SIZE;

            // get zoom level count from file header
            mZoomLevelCount = mBBFileHeader.getZoomLevels();

            // extract zoom level headers and zoom data records
            // Note: zoom headers Table D immediately follow the BBFile Header
            if (mZoomLevelCount > 0) {

                mZoomLevelOffset = mFileOffset;

                mZoomLevels = new BBZoomLevels(mBBFis, mZoomLevelOffset, mZoomLevelCount,
                        mIsLowToHigh, mUncompressBufSize);

                // end of zoom level headers - compare with next BBFile item location
                mFileOffset += mZoomLevelCount * BBZoomLevelHeader.ZOOM_LEVEL_HEADER_SIZE;
            }

            // get the AutoSQL custom BigBed fields
            mAutoSqlOffset = mBBFileHeader.getAutoSqlOffset();
            if (mAutoSqlOffset != 0) {
                // read in .as entry
                // mFileOffset = mAutoSqlOffset + sizeof(.as format field);
            }

            // get the Total Summary Block (Table DD)
            mFileOffset = mBBFileHeader.getTotalSummaryOffset();
            if (mBBFileHeader.getVersion() >= 2 && mFileOffset > 0) {
                mTotalSummaryBlock = new BBTotalSummaryBlock(mBBFis, mFileOffset, mIsLowToHigh);
                mFileOffset += BBTotalSummaryBlock.TOTAL_SUMMARY_BLOCK_SIZE;
            }

            // get Chromosome Data B+ Tree (Table E, F, G, H) : should always exist
            mChromIDTreeOffset = mBBFileHeader.getChromosomeTreeOffset();
            if (mChromIDTreeOffset != 0) {
                mFileOffset = mChromIDTreeOffset;
                mChromosomeIDTree = new BPTree(mBBFis, mFileOffset, mIsLowToHigh);
            }

            // get number of data records indexed by the R+ chromosome data location tree
            mFileOffset = mBBFileHeader.getFullDataOffset();
            mDataCount = getDataCount(mBBFis, mFileOffset);

            // get R+ chromosome data location tree (Tables K, L, M, N)
            mChromDataTreeOffset = mBBFileHeader.getFullIndexOffset();
            if (mChromDataTreeOffset != 0) {
                mFileOffset = mChromDataTreeOffset;
                mChromosomeDataTree = new RPTree(mBBFis, mFileOffset, mIsLowToHigh,
                        mUncompressBufSize);
            }

        } catch (IOException ex) {
            log.error("Error reading BBFile: " + path, ex);
            throw new RuntimeException("Error reading BBFile: " + path, ex);
        }

    }

    /*
    *   Method returns the Big Binary File pathname.
    *
    *   Returns:
    *       Big Binary File pathname
    * */

    public String getBBFilePath() {
        return mBBFilePath;
    }

    /*
    *   Method returns the Big Binary File input stream handle.
    *
    *   Returns:
    *       Big Binary File input stream handle
    * */

    public SeekableStream getBBFis() {
        return mBBFis;
    }

    /*
    *   Method returns the Big Binary File header which identifies
    *   the file type and content.
    *
    *   Returns:
    *       Big Binary File header (Table C)
    * */

    public BBFileHeader getBBFileHeader() {
        return mBBFileHeader;
    }

    /*
    *   Method returns if the Big Binary File is BigBed.
    *
    *   Returns:
    *       Boolean identifies if Big Binary File is BigBed
    *       (recognized from magic number in file header Table C)
    * */

    public boolean isBigBedFile() {
        return mBBFileHeader.isBigBed();
    }

    /*
    *   Method returns if the Big Binary File is BigWig
    *
    *   Returns:
    *       Boolean identifies if Big Binary File is BigWig
    *       (recognized from magic number in file header Table C)
    * */

    public boolean isBigWigFile() {
        return mBBFileHeader.isBigWig();
    }

    /*
    *   Method returns the total number of data records in the file.
    *
    *   Returns:
    *       Count of the total number of compressed/uncompressed data records:
    *           which for BigBed is the number of bed features,
    *           and for BiGWifg is the number of wig sections.
    * */

    public int getDataCount() {
        return mDataCount;
    }

    /*
    *   Method returns the number of chromosomes/contigs in the file.
    *
    *   Note: This is itemCount from B+ tree header BBFile Table E.
    * 
    *   Returns:
    *       Count of the total number of chromosomes/contigs in the file.
    * */

    public long getChromosomeNameCount() {
        return mChromosomeIDTree.getItemCount();
    }

    /*
    *   Method returns the number of chromosome/contig regions in the file.
    *
    *   Note: This is itemCount from R+ tree header BBFile Table K.
    *
    *   Returns:
    *       Count of the total number of chromosome/contig regions in the file.
    * */

    public long getChromosomeRegionCount() {
        return mChromosomeDataTree.getItemCount();
    }

    /*
   *   Method returns the Big Binary File decompressed buffer size.
   *
   *   Returns:
   *       Largest required buffer size for decompressed data chunks (from Table C)
   * */

    public int getDecompressionBufSize() {
        return mUncompressBufSize;
    }

    /*
    *   Method returns if the Big Binary File is written with a low to high byte
    *   order for formatted data.
    *
    *   Returns:
    *       Boolean identifies if Big Binary File is low to high byte order
    *       (recognized from magic number in file header Table C); else is
    *       high to low byte order if false.
    * */

    public boolean isLowToHigh() {
        return mIsLowToHigh;
    }

    /*
    *   Method returns the total summary block for the Big Binary File.
    *
    *   Returns:
    *       Total summary block data statistics for the whole file (Table DD)
    * */

    public BBTotalSummaryBlock getTotalSummaryBlock() {
        return mTotalSummaryBlock;
    }

    /*
    *   Method returns the B+ Chromosome Index Tree.
    *
    *   Returns:
    *       B+ Chromosome Index Tree (includes Tables  E, F, G, H)
    * */

    public BPTree getChromosomeIDTree() {
        return mChromosomeIDTree;
    }

    /*
    *   Method returns the R+ Chromosome Data Locations Tree.
    *
    *   Returns:
    *       R+ Chromosome Data Locations Tree (includes Tables  K, L, M, N)
    * */

    public RPTree getChromosomeDataTree() {
        return mChromosomeDataTree;
    }

    /*
    *   Method returns number of zoom level data is included in the file.
    *
    *   Returns:
    *       Number of zoom levels (from Table C)
    * */

    public int getZoomLevelCount() {
        return mZoomLevelCount;
    }

    /*
    *   Method returns the zoom levels in the Big Binary File.
    *
    *   Returns:
    *       Zoom level object containing zoom level headers and R+ zoom data locations tree
    *       (includes Tables  D, O)
    * */

    public BBZoomLevels getZoomLevels() {
        return mZoomLevels;
    }

    /*
    *   Method finds the zoom data bounds in R+ tree for a chromosome ID range.
    *
    *   Parameters:
    *       zoomLevel - zoom level
    *       startChromID - start chromosome for the region
    *       endChromID - end chromosome for the region
    *
    *   Returns:
    *       Chromosome region bounds for chromosome ID range
    * */

    public RPChromosomeRegion getZoomLevelBounds(int zoomLevel, int startChromID,
                                                 int endChromID) {

        RPChromosomeRegion chromosomeBounds =
                mZoomLevels.getZoomLevelRPTree(zoomLevel).getChromosomeRegion(startChromID, endChromID);

        return chromosomeBounds;
    }

    /*
    *   Method finds chromosome bounds for entire chromosome ID range in the zoom level R+ tree.
    *
    *   Parameters:
    *       zoomLevel - zoom level
    *
    *   Returns:
    *       Chromosome bounds for the entire chromosome ID range in the R+ tree.
    * */

    public RPChromosomeRegion getZoomLevelBounds(int zoomLevel) {

        RPChromosomeRegion chromosomeBounds =
                mZoomLevels.getZoomLevelRPTree(zoomLevel).getChromosomeBounds();

        return chromosomeBounds;
    }

    /*
    *   Method returns the zoom record count for the zoom level.
    *
    *   Parameters:
    *       zoomLevel - zoom level
    *
    *   Returns:
    *       Chromosome bounds for the entire chromosome ID range in the R+ tree.
    * */

    public int getZoomLevelRecordCount(int zoomLevel) {

        return mZoomLevels.getZoomLevelFormats().get(zoomLevel - 1).getZoomRecordCount();
    }

    /*
    *   Method finds chromosome key name for the associated chromosome ID in the B+ tree.
    *
    *   Returns:
    *       chromosome key name for associated chromosome ID.
    * */

    public String getChromosomeName(int chromID) {

        String chromosomeName = mChromosomeIDTree.getChromosomeName(chromID);
        return chromosomeName;
    }

    /*
    *   Method finds chromosome names in the B+ chromosome index tree.
    *
    *   Returns:
    *       LIst of all chromosome key names in the B+ tree.
    * */

    public ArrayList<String> getChromosomeNames() {

        ArrayList<String> chromosomeList = mChromosomeIDTree.getChromosomeNames();
        return chromosomeList;
    }

    /*
   *   Returns a chromosome ID  which  can be used to search for a
   *   corresponding data section in the R+ tree for data.
   *
      Parameters:
   *       chromosomeKey - chromosome name of valid key size.
   *
   *
   *   Note: A chromosomeID of -1 means chromosome name not included in B+ tree.
   *
   * */

    public int getChromosomeID(String chromosomeKey) {

        int chromosomeID = mChromosomeIDTree.getChromosomeID(chromosomeKey);

        return chromosomeID;
    }

    /*
    *   Method finds the chromosome bounding region in R+ tree for a chromosome ID range.
    *
    *   Parameters:
    *       startChromID - start chromosome for the region
    *       endChromID - end chromosome for the region
    *
    *   Returns:
    *       Chromosome region bounds for chromosome ID range
    * */

    public RPChromosomeRegion getChromosomeBounds(int startChromID, int endChromID) {

        RPChromosomeRegion chromosomeBounds =
                mChromosomeDataTree.getChromosomeRegion(startChromID, endChromID);

        return chromosomeBounds;
    }

    /*
    *   Method finds the chromosome bounds for the entire chromosome ID range in the R+ tree.
    *
    *   Returns:
    *       chromosome bounds for the entire chromosome ID range in the R+ tree.
    * */

    public RPChromosomeRegion getChromosomeBounds() {

        RPChromosomeRegion chromosomeBounds = mChromosomeDataTree.getChromosomeBounds();

        return chromosomeBounds;
    }

    /*
    *   Method finds all chromosome data regions in the R+ tree.
    *
    *   Returns:
    *       List of chromosome ID's and regions.
    * */

    public ArrayList<RPChromosomeRegion> getChromosomeRegions() {

        ArrayList<RPChromosomeRegion> regionList = mChromosomeDataTree.getAllChromosomeRegions();

        return regionList;
    }

    /*
    *   Method finds all zoom level data regions in the R+ tree.
    *
    *   Parameters:
    *       int zoomLevel - zoom level
    *   Returns:
    *       List of chromosome ID's and regions for the zoom level.
    * */

    public ArrayList<RPChromosomeRegion> getZoomLevelRegions(int zoomLevel) {

        ArrayList<RPChromosomeRegion> regionList =
                mZoomLevels.getZoomLevelRPTree(zoomLevel).getAllChromosomeRegions();

        return regionList;
    }

    /**
     * Returns an iterator for BigBed features which occupy a chromosome selection region.
     * <p/>
     * Note: the BBFile type should be BigBed; else a null iterator is returned.
     * <p/>
     * Parameters:
     * startChromosome - name of start chromosome
     * startBase     - starting base position for features
     * endChromosome - name of end chromosome
     * endBase       - ending base position for feature
     * contained     - flag specifies bed features must be contained in the specified
     * base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigBed.(see isBigBedFile method)
     */
    public BigBedIterator getBigBedIterator(String startChromosome, int startBase,
                                            String endChromosome, int endBase, boolean contained) {

        if (!isBigBedFile())
            return null;

        // go from chromosome names to chromosome ID region
        RPChromosomeRegion selectionRegion = getChromosomeBounds(startChromosome, startBase,
                endChromosome, endBase);

        // check for valid selection region
        if (selectionRegion == null)
            throw new RuntimeException("Error finding BigBedIterator region: chromosome not found \n");

        // compose an iterator
        BigBedIterator bedIterator = new BigBedIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return bedIterator;
    }

    /**
     * Returns an iterator for BigBed features for all chromosome regions.
     * <p/>
     * Note: the BBFile type should be BigBed; else a null iterator is returned.
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for all chromosome regions.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigBed.(see isBigBedFile method)
     */
    public BigBedIterator getBigBedIterator() {

        if (!isBigBedFile())
            return null;

        // get all region bounds
        RPChromosomeRegion selectionRegion = mChromosomeDataTree.getChromosomeBounds();

        // compose an iterator
        boolean contained = true;   /// all regions are contained
        BigBedIterator bedIterator = new BigBedIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return bedIterator;
    }

    /**
     * Returns an iterator for BigBed features which occupy a chromosome selection region.
     * <p/>
     * Note: the BBFile type should be BigBed; else a null iterator is returned.
     * <p/>
     * Parameters:
     * selectionRegion - chromosome selection region consisting of:
     * startChromID - ID of start chromosome
     * startBase     - starting base position for features
     * endChromosome - ID of end chromosome
     * endBase       - ending base position for feature
     * contained     - flag specifies bed features must be contained in the specified
     * base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome selection region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigBed.(see isBigBedFile method)
     */
    public BigBedIterator getBigBedIterator(RPChromosomeRegion selectionRegion, boolean contained) {

        if (!isBigBedFile())
            return null;

        // compose an iterator
        BigBedIterator bedIterator = new BigBedIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return bedIterator;
    }

    /**
     * Returns an iterator for BigWig values which occupy the specified startChromosome region.
     * <p/>
     * Note: the BBFile type should be BigWig; else a null iterator is returned.
     * <p/>
     * Parameters:
     * startChromosome  - name of start chromosome
     * startBase    - starting base position for features
     * endChromosome  - name of end chromosome
     * endBase      - ending base position for feature
     * contained    - flag specifies bed features must be contained in the specified
     * base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigWig.(see isBigWigFile method)
     */
    public BigWigIterator getBigWigIterator(String startChromosome, int startBase,
                                            String endChromosome, int endBase, boolean contained) {

        if (!isBigWigFile())
            return null;

        // go from chromosome names to chromosome ID region
        RPChromosomeRegion selectionRegion = getChromosomeBounds(startChromosome, startBase,
                endChromosome, endBase);

        // check for valid selection region
        if (selectionRegion == null)
            throw new RuntimeException("Error finding BigWigIterator region: chromosome not found \n");

        // compose an iterator
        BigWigIterator wigIterator = new BigWigIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return wigIterator;
    }

    /**
     * Returns an iterator for BigWig values for all chromosome regions.
     * <p/>
     * Note: the BBFile type should be BigWig; else a null iterator is returned.
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for all chromosome regions.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigWig.(see isBigWigFile method)
     */
    public BigWigIterator getBigWigIterator() {

        if (!isBigWigFile())
            return null;

        // get all regions bounds
        RPChromosomeRegion selectionRegion = mChromosomeDataTree.getChromosomeBounds();

        // compose an iterator
        boolean contained = true;       // all regions are contained
        BigWigIterator wigIterator = new BigWigIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return wigIterator;
    }

    /**
     * Returns an iterator for BigWig values which occupy the specified chromosome selection region.
     * <p/>
     * Note: the BBFile type should be BigBed; else a null iterator is returned.
     * <p/>
     * Parameters:
     * selectionRegion - chromosome selction region consists of:
     * startChromID  - ID of start chromosome
     * startBase    - starting base position for features
     * endChromoID  - ID of end chromosome
     * endBase      - ending base position for feature
     * contained    - flag specifies bed features must be contained in the specified
     * base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome selection region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     * 2) A null object is returned if the file is not BigBed.(see isBigWigFile method)
     */
    public BigWigIterator getBigWigIterator(RPChromosomeRegion selectionRegion, boolean contained) {

        if (!isBigWigFile())
            return null;

        // compose an iterator
        BigWigIterator wigIterator = new BigWigIterator(mBBFis, mChromosomeIDTree, mChromosomeDataTree,
                selectionRegion, contained);

        return wigIterator;
    }

    /**
     * Returns an iterator for zoom level records for the chromosome selection region.
     * <p/>
     * Note: the BBFile can be BigBed or BigWig.
     * <p/>
     * Parameters:
     * zoomLevel - zoom level for data extraction; levels start at 1
     * startChromosome - start chromosome name
     * startBase     - staring base position for features
     * endChromosome - end chromosome name
     * endBase       - ending base position for feature
     * contained     - flag specifies bed features must be contained in the
     * specified base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     */
    public ZoomLevelIterator getZoomLevelIterator(int zoomLevel, String startChromosome, int startBase,
                                                  String endChromosome, int endBase, boolean contained) {
        // check for valid zoom level
        if (zoomLevel < 1 || zoomLevel > mZoomLevelCount)
            throw new RuntimeException("Error: ZoomLevelIterator zoom level is out of range\n");

        // get the appropriate zoom level R+ zoom data index tree
        RPTree zoomDataTree = mZoomLevels.getZoomLevelRPTree(zoomLevel);

        // go from chromosome names to chromosome ID region
        RPChromosomeRegion selectionRegion = getChromosomeBounds(startChromosome, startBase,
                endChromosome, endBase);

        // check for valid selection region
        if (selectionRegion == null)
            throw new RuntimeException("Error: ZoomLevelIterator selection region is null\n");

        /// compose an iterator
        ZoomLevelIterator zoomIterator = new ZoomLevelIterator(mBBFis, mChromosomeIDTree,
                zoomDataTree, zoomLevel, selectionRegion, contained);

        return zoomIterator;
    }

    /**
     * Returns an iterator for zoom level records for all chromosome regions.
     * <p/>
     * Note: the BBFile can be BigBed or BigWig.
     * <p/>
     * Parameters:
     * zoomLevel - zoom level for data extraction; levels start at 1
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     */
    public ZoomLevelIterator getZoomLevelIterator(int zoomLevel) {

        // check for valid zoom level
        if (zoomLevel < 1 || zoomLevel > mZoomLevelCount)
            throw new RuntimeException("Error: ZoomLevelIterator zoom level is out of range\n");

        // get the appropriate zoom level R+ zoom data index tree
        RPTree zoomDataTree = mZoomLevels.getZoomLevelRPTree(zoomLevel);

        // get all regions bounds
        RPChromosomeRegion selectionRegion = zoomDataTree.getChromosomeBounds();

        // compose an iterator
        boolean contained = true;   //all regions are contained
        ZoomLevelIterator zoomIterator = new ZoomLevelIterator(mBBFis, mChromosomeIDTree,
                zoomDataTree, zoomLevel, selectionRegion, contained);

        return zoomIterator;
    }

    /**
     * Returns an iterator for zoom level records for the chromosome selection region.
     * <p/>
     * Note: the BBFile can be BigBed or BigWig.
     * <p/>
     * Parameters:
     * zoomLevel - zoom level for data extraction; levels start at 1
     * selectionRegion - chromosome selection region consists of:
     * startChromID - ID of starting chromosome
     * startBase     - staring base position for features
     * endChromID - ID of endind chromosome
     * endBase       - ending base position for feature
     * contained     - flag specifies bed features must be contained in the
     * specified base region if true; else can intersect the region if false
     * <p/>
     * Returns:
     * Iterator to provide BedFeature(s) for the requested chromosome region.
     * Error conditions:
     * 1) An empty iterator is returned if region has no data available
     */
    public ZoomLevelIterator getZoomLevelIterator(int zoomLevel, RPChromosomeRegion selectionRegion,
                                                  boolean contained) {
        // check for valid zoom level
        if (zoomLevel < 1 || zoomLevel > mZoomLevelCount)
            throw new RuntimeException("Error: ZoomLevelIterator zoom level is out of range\n");

        // get the appropriate zoom level R+ zoom data index tree
        RPTree zoomDataTree = mZoomLevels.getZoomLevelRPTree(zoomLevel);

        /// compose an iterator
        ZoomLevelIterator zoomIterator = new ZoomLevelIterator(mBBFis, mChromosomeIDTree,
                zoomDataTree, zoomLevel, selectionRegion, contained);

        return zoomIterator;
    }

    /*
    *   Method generates a chromosome bounds region for the supplied chromosome region name.
    *
    *   Note: No attempt is made to verify the region exists in the file data, nor
    *   which data is being examined.
    *
    *   Parameters:
    *       startChromosome - name of start chromosome
    *       startBase - starting base position for region
    *       endChromosome - name of end chromosome
    *       endBase - ending base position for region
    *
    *   Returns:
    *       Chromosome bounds of a named chromosome region for data extraction;
    *       or null for regions not found in the B+ chromosome index tree.
    * */

    private RPChromosomeRegion getChromosomeBounds(String startChromosome, int startBase,
                                                   String endChromosome, int endBase) {

        // find the chromosome ID's using the name to get a valid name key, then associated ID
        String startChromKey = mChromosomeIDTree.getChromosomeKey(startChromosome, startBase);
        int startChromID = mChromosomeIDTree.getChromosomeID(startChromKey);
        if (startChromID < 0)       // mChromosome not in data?
            return null;

        String endChromKey = mChromosomeIDTree.getChromosomeKey(endChromosome, endBase);
        int endChromID = mChromosomeIDTree.getChromosomeID(endChromKey);
        if (endChromID < 0)       // mChromosome not in data?
            return null;

        // create the bounding mChromosome region
        RPChromosomeRegion chromBounds = new RPChromosomeRegion(startChromID, startBase,
                endChromID, endBase);

        return chromBounds;
    }

    /*
    *   Method reads data count which heads the data section of the BBFile.
    *
    *   Returns:
    *       Data count of the number of data records:
    *          number of Bed features for BigBed
    *          number of Wig sections for BigWig
    * */

    private int getDataCount(SeekableStream fis, long fileOffset) {
        int dataCount;
        LittleEndianInputStream lbdis = null;
        DataInputStream bdis = null;

        // Note: dataCount in BBFile is simply a 4 byte int
        // positioned at fullDataOffset in Table C
        byte[] buffer = new byte[4];
        int bytesRead;

        try {
            // read dataCount into a buffer
            mBBFis.seek(fileOffset);
            bytesRead = mBBFis.read(buffer);

            // decode data count with proper byte stream reader
            // first assume byte order is low to high
            if (mIsLowToHigh) {
                lbdis = new LittleEndianInputStream(new ByteArrayInputStream(buffer));
                dataCount = lbdis.readInt();
            } else {
                bdis = new DataInputStream(new ByteArrayInputStream(buffer));
                dataCount = bdis.readInt();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading data count for all data", ex);
        }

        // data count was read properly
        return dataCount;
    }

} // end of BBFileReader
