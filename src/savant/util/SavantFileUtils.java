/*
 *    Copyright 2010-2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package savant.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.Block;
import savant.data.types.ItemRGB;
import savant.file.FieldType;
import savant.file.FileType;
import savant.file.FileTypeHeader;
import savant.file.ROFile;
import savant.format.SavantFileFormatterUtils;


public class SavantFileUtils {

    private static final Log LOG = LogFactory.getLog(SavantFileUtils.class);

    public static List<FieldType> readFieldsHeader(ROFile src) throws IOException {

        int numFields = src.readInt();

        List<FieldType> fields = new ArrayList<FieldType>();

        for (int i = 0; i < numFields; i++) {
            fields.add(FieldType.class.getEnumConstants()[src.readInt()]);
        }

        return fields;
    }

    public static int getRecordSize(ROFile file) throws IOException {
//        long currpos = file.getFilePointerSuper();
        long currpos = file.getFilePointer();
        file.seek(MiscUtils.set2List(file.getReferenceMap().keySet()).get(0), 0);
        List<Object> record = readBinaryRecord(file, file.getFields());
//        file.seekSuper(currpos);
        file.seek(currpos);
        return SavantFileFormatterUtils.getRecordSize(record, file.getFields());
    }

    public static List<Object> readBinaryRecord(ROFile file, List<FieldType> fields) throws IOException {

        List<Object> record = new ArrayList<Object>(fields.size());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading binary record");
            LOG.debug("Fields");
            for (FieldType ft : fields) {
                LOG.debug("\t" + ft);
            }
        }

        for (FieldType ft : fields) {
            if (ft == FieldType.IGNORE) { continue; }

            switch(ft) {
                case INTEGER:
                    record.add(file.readInt());
                    break;
                case ITEMRGB:
                    int r = file.readInt();
                    int g = file.readInt();
                    int b = file.readInt();
                    record.add(ItemRGB.valueOf(r,g,b));
                    break;
                case DOUBLE:
                    record.add(file.readDouble());
                    break;
                case FLOAT:
                    record.add(file.readFloat());
                    break;
                case BLOCKS:
                    int numBlocks = file.readInt();
                    List<Block> blocks = new ArrayList<Block>(numBlocks);
                    for (int i = 0; i < numBlocks; i++) {
                        int start = file.readInt();
                        int size = file.readInt();
                        blocks.add(Block.valueOf(start,size));
                    }
                    record.add(blocks);
                    break;
                case STRING:
                    int len = file.readInt();
                    //System.out.println("Reading binary string of length: " + len);
                    if (len > 10000) {
                        throw new IOException("Tried to read binary string of length " + len + " characters");
                    }
                    String s = "";
                    for (int i = 0; i < len; i++) {
                        s += (char) file.readByte();
                    }
                    record.add(s);
                    break;
                case CHAR:
                    record.add((char) file.readByte());
                    break;
                case RANGE:
                    int start = file.readInt();
                    int end = file.readInt();
                    record.add(new Range(start,end));
                    break;
                case LONG:
                    record.add(file.readLong());
                    break;
                case IGNORE:
                    break;
                default:
                    LOG.warn("Not implemented yet for Field Type: " + ft);
                    break;
            }
        }

        return record;
    }

    public static FileTypeHeader readFileTypeHeader(ROFile rof) throws IOException {
        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        List<Object> record = readBinaryRecord(rof,fields);
        Integer magicNumber = (Integer) record.get(0);
        FileTypeHeader fth = new FileTypeHeader( FileType.fromMagicNumber(magicNumber), (Integer) record.get(1));
        if (fth.fileType == null) {
            if (littleEndian(magicNumber)) {
                throw new IOException("File is LITTLE_ENDIAN.");
            }
        }
        return fth;
    }

    /**
     * Read the reference map from a Savant file.  We use a LinkedHashMap so that
     * the iteration order is always the same as the insertion order.
     */
    public static Map<String, long[]> readReferenceMap(ROFile rof) throws IOException {

        int numRefs = rof.readInt();

        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.STRING);
        fields.add(FieldType.LONG);
        fields.add(FieldType.LONG);

        List<ReferenceInfo> unsortedRefs = new ArrayList<ReferenceInfo>(numRefs);
        //System.out.println("Reading " + numreferences + " references");
        for (int i = 0; i < numRefs; i++) {

            List<Object> record = readBinaryRecord(rof,fields);
            long[] vals = new long[] { (Long)record.get(1), (Long)record.get(2) };
            unsortedRefs.add(new ReferenceInfo(((String)record.get(0)).trim(), vals));
        }

        // Make sure the references are sorted in a human-friendly order.
        Collections.sort(unsortedRefs, new Comparator<ReferenceInfo>() {
            private ReferenceComparator comparator = new ReferenceComparator();

            @Override
            public int compare(ReferenceInfo t1, ReferenceInfo t2) {
                return comparator.compare(t1.ref, t2.ref);
            }
        });

        Map<String, long[]> referenceMap = new LinkedHashMap<String, long[]>();
        for (ReferenceInfo info: unsortedRefs) {
            referenceMap.put(info.ref, info.vals);
        }
        return referenceMap;
    }

    private static class ReferenceInfo {
        String ref;
        long[] vals;

        ReferenceInfo(String ref, long[] vals) {
            this.ref = ref;
            this.vals = vals;
        }
    }

//    public static void writeFileTypeHeader(RWFile rwf, FileTypeHeader fth) throws IOException {
//        rwf.writeInt(fth.fileType.getMagicNumber());
//        rwf.writeInt(fth.version);
//
//    }
//
//    public static void writeFieldsHeader(RWFile rwf, List<FieldType> fields) throws IOException {
//
//        rwf.writeInt(fields.size());
//
//        for (FieldType ft : fields) {
//            rwf.writeInt(ft.ordinal());
//        }
//    }

//    public static void writeBinaryRecord(RWFile rwf, List<Object> line, List<FieldType> fields, List<Object> modifiers) throws IOException {
//
////        FieldType fieldType = null;
//        Object o = null;
//        Object instruction = null;
//
//        int entryNum = 0;
//        int numIgnores = 0;
//
//        for (FieldType ft : fields) {
//            if (ft == FieldType.IGNORE) {
//                numIgnores++;
//                continue;
//            }
//
//            o = line.get(entryNum);
//            instruction = modifiers.get(entryNum + numIgnores);
//
//            //System.out.println("Writing " + o + " as " + ft + " with instruction " + instruction);
//
//            switch (ft) {
//                case STRING:
//                    int stringLength;
//                    if (modifiers.get(entryNum) == null) {
//                        stringLength = ((String) o).length();
//                    } else {
//                        stringLength = (Integer) instruction;
//                    }
//                    writeFixedLengthString(rwf, (String) o, stringLength);
//                    break;
//                case CHAR:
//                    rwf.writeByte((Character) o);
//                    break;
//                case BLOCKS:
//                    List<Block> blocks = (List<Block>) o;
//                    rwf.writeInt(blocks.size());
//                    for (Block b : blocks) {
//                        rwf.writeInt(b.getPosition());
//                        rwf.writeInt(b.getSize());
//                    }
//                    break;
//                case ITEMRGB:
//                    ItemRGB rgb = (ItemRGB) o;
//                    rwf.writeInt(rgb.getRed());
//                    rwf.writeInt(rgb.getBlue());
//                    rwf.writeInt(rgb.getGreen());
//                    break;
//                case INTEGER:
//                    rwf.writeInt((Integer) o);
//                    break;
//                case DOUBLE:
//                    rwf.writeDouble((Double) o);
//                    break;
//                case FLOAT:
//                    rwf.writeFloat((Float) o);
//                    break;
//                case LONG:
//                    rwf.writeLong((Long) o);
//                    break;
//                case BOOLEAN:
//                    int bool = Integer.parseInt((String) o);
//                    rwf.writeInt(bool);
//                    break;
//                case RANGE:
//                    Range r = (Range) o;
//                    rwf.writeInt(r.getFrom());
//                    rwf.writeInt(r.getTo());
//                    break;
//                default:
//                    System.err.println("DataFormatUtils.writeBinaryRecord: Not implemented for " + ft);
//                    break;
//            }
//
//            entryNum++;
//        }
//    }

//    public static void writeFixedLengthString(RWFile rwf, String s, int len) throws IOException {
//
//        int pad = len - s.length();
//
//        rwf.writeInt(len);
//        if (!s.equals("")) { rwf.writeBytes(s.substring(0, Math.min(s.length(),len))); }
//        while (pad > 0) {
//            rwf.writeBytes(" ");
//            pad--;
//        }
//
//        //System.out.println("\tWriting " + len + " chars from [" + s + "] padded by " + (len - s.length()) + " = " + (after-before) + " bytes");
//    }

//    public static RWFile openFile(String path) throws IOException, SavantUnsupportedVersionException {
//        return openFile(path,true);
//    }

//    public static SavantRWFile openFile(String path, boolean seekToEnd) throws IOException, SavantUnsupportedVersionException {
//        SavantRWFile f = new SavantRWFile(path);
//        if (seekToEnd) {
//            seekToEnd(f);
//        }
//        return f;
//    }

    /**
     * Seek to the end of the given RandomAccessFile
     * @param f
     * @throws IOException
     */
//    public static void seekToEnd(RWFile f) throws IOException {
//        f.seek(f.length());
//    }

    private static boolean littleEndian(Integer magicNumber) {

        boolean result = false;

        Integer highOrderWord = magicNumber >> 16;
        Integer highOrderWordLittleEndian = ((highOrderWord & 0x00FF) << 8) & (highOrderWord >> 8);

        Integer lowOrderWord = magicNumber & 0xFFFF;
        Integer lowOrderWordLittleEndian = ((lowOrderWord & 0x00FF) << 8) & (lowOrderWord >> 8);

        Integer magicNumberLittleEndian = (highOrderWordLittleEndian << 16) & lowOrderWordLittleEndian;
        
        if (FileType.fromMagicNumber(magicNumberLittleEndian) != null) {
            result = true;
        }
        return result;
    }
}
