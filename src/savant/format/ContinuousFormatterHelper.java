/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.format;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import savant.util.RAFUtils;
import savant.util.SavantFileFormatterUtils;

/**
 *
 * @author mfiume
 */
public class ContinuousFormatterHelper {

    // size of the output buffer
    protected static final int OUTPUT_BUFFER_SIZE = 1024 * 128; // 128K


    public static Map<String, String> makeMultiResolutionContinuousFiles(Map<String, String> referenceName2FilenameMap) throws FileNotFoundException, IOException {

        System.out.println("Making multi resolution continuous files...");

        Map<String, String> referenceNameToIndexMap = new HashMap<String, String>();

        for (String refname : referenceName2FilenameMap.keySet()) {

            System.out.println("Making multi resolution continuous file for " + refname);

            String infile = referenceName2FilenameMap.get(refname);

            String indexpath = infile + SavantFileFormatter.indexExtension;

            List<Level> levels = new ArrayList<Level>();

            // start making levels

            Mode m = new FixedMode(1, 1, 1);
            Level l1 = new Level(
                    0,
                    new File(infile).length(),
                    SavantFileFormatterUtils.FLOAT_FIELD_SIZE,
                    1,
                    m);

            levels.add(l1);

            // end make levels

            // write index file
            writeLevelsToFile(refname, levels, indexpath);

            referenceNameToIndexMap.put(refname, indexpath);
        }

        return referenceNameToIndexMap;
    }

    private static void writeLevelsToFile(String refname, List<Level> levels, String filename) throws IOException {

        DataOutputStream outfile = new DataOutputStream(
                new BufferedOutputStream(
                new FileOutputStream(filename), OUTPUT_BUFFER_SIZE));

        System.out.println("Writing header for " + refname + " with " + levels.size() + " levels");

        outfile.writeInt(levels.size());
        
        for (Level l : levels) {
            writeLevelToFile(refname, l, outfile);
        }

        outfile.close();
    }

    private static void writeLevelToFile(String refname, Level l, DataOutputStream o) throws IOException {

        o.writeLong(l.offset);
        o.writeLong(l.size);
        o.writeLong(l.recordSize);
        o.writeLong(l.resolution);

        o.writeInt(l.mode.type.ordinal());
        if (l.mode.type == Mode.ModeType.FIXED) {
            FixedMode m = (FixedMode) l.mode;
            o.writeInt(m.start);
            o.writeInt(m.step);
            o.writeInt(m.span);
        } else if (l.mode.type == Mode.ModeType.VARIABLESTEP) {
            throw new UnsupportedOperationException("Variable step writing not implemented");
        }
    }

    public static Map<String,List<Level>> readLevelHeadersFromBinaryFile(SavantFile savantFile) throws IOException {

        // read the refname -> index position map
        Map<String,Long[]> refMap = RAFUtils.readReferenceMap(savantFile);

        //System.out.println("\n=== DONE PARSING REF<->DATA MAP ===");
        //System.out.println();

        // change the offset
        savantFile.headerOffset = savantFile.getFilePointerSuper();

        /*
        for (String s : refMap.keySet()) {
            Long[] vals = refMap.get(s);
            //System.out.println("Reference " + s + " at " + vals[0] + " of length " + vals[1]);
        }
         */

        Map<String,List<Level>> headers = new HashMap<String,List<Level>>();

        int headerNum = 0;

       System.out.println("Number of headers to get: " + refMap.keySet().size());

        // keep track of the maximum end of tree position
        // (IMPORTANT NOTE: order of elements returned by keySet() is not gauranteed!!!)
        long maxend = Long.MIN_VALUE;
        for (String refname : refMap.keySet()) {
            Long[] v = refMap.get(refname);

            //System.out.println("Getting header for refname: " + refname);

            //System.out.println("========== Reading header for reference " + refname + " ==========");
            savantFile.seek(v[0]);

            //System.out.println("Starting header at (super): " + savantFile.getFilePointerSuper());

            List<Level> header = readLevelHeaderFromBinaryFile(savantFile);

            //System.out.println("Finished header at (super): " + savantFile.getFilePointerSuper());

            maxend = Math.max(maxend,savantFile.getFilePointerSuper());

            headers.put(refname, header);
            headerNum++;
        }

        /*
        System.out.println("Read " + treenum + " trees (i.e. indicies)");
        System.out.println("\n=== DONE PARSING REF<->INDEX MAP ===");
        System.out.println("Changing offset from " + dFile.getHeaderOffset() + " to " + (dFile.getFilePointer()+dFile.getHeaderOffset()));
        System.out.println();
         */

        // set the header offset appropriately
        savantFile.headerOffset = maxend;

        return headers;
    }

    public static List<Level> readLevelHeaderFromBinaryFile(SavantFile savantFile) throws IOException {

        List<Level> header = new ArrayList<Level>();

        int numLevels = savantFile.readInt();

        //System.out.println("Number of levels in header: " + numLevels);

        for (int i = 0; i < numLevels; i++) {
            Level l = readLevelFromBinaryFile(savantFile);
            header.add(l);
        }

        return header;
    }

    private static Level readLevelFromBinaryFile(SavantFile savantFile) throws IOException {

        // need to use readBinaryRecord!
        long offset = savantFile.readLong();
        long size = savantFile.readLong();
        long recordSize = savantFile.readLong();
        long resolution = savantFile.readLong();
        Mode.ModeType type = Mode.ModeType.class.getEnumConstants()[savantFile.readInt()];
        Mode m = null;
        if (type == Mode.ModeType.FIXED) {
            int start = savantFile.readInt();
            int step = savantFile.readInt();
            int span = savantFile.readInt();
            m = new FixedMode(start, step, span);
        } else {
            throw new UnsupportedOperationException("Reading other mode types not implemented");
        }

        Level l = new Level(offset, size, recordSize, resolution, m);
        return l;
    }

    public static class Mode {

        public enum ModeType { FIXED, VARIABLESTEP };
        
        public ModeType type;

        public Mode(ModeType t) {
            this.type = t;
        }
    }

    public static class Level {

        public long offset;
        public long size;
        public long recordSize;
        public long resolution;
        public Mode mode;

        public Level(long offset, long size, long recordSize, long resolution, Mode m) {
            this.offset = offset;
            this.size = size;
            this.recordSize = recordSize;
            this.resolution = resolution;
            this.mode = m;
        }
    }

    public static class FixedMode extends Mode {

        public int start;
        public int step;
        public int span;

        public FixedMode(int start, int step, int span) {
            super(Mode.ModeType.FIXED);
            this.start = start;
            this.step = step;
            this.span = span;
        }
    }

    public static class VariableStepMode extends Mode {

        public int start;
        public int step;
        public int span;

        public VariableStepMode(int start, int step, int span) {
            super(Mode.ModeType.VARIABLESTEP);
            this.start = start;
            this.step = step;
            this.span = span;
        }
    }
}
