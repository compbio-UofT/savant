/*
 *    Copyright 2010 University of Toronto
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

/*
 * RAFUtils.java
 * Created on Mar 23, 2010
 */

package savant.util;

import savant.format.SavantFile;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class RAFUtils {

    public static List<FieldType> readFieldsHeader(RandomAccessFile src) throws IOException {

        int numFields = src.readInt();

        List<FieldType> fields = new ArrayList<FieldType>();

        for (int i = 0; i < numFields; i++) {
            fields.add(FieldType.class.getEnumConstants()[src.readInt()]);
        }

        return fields;
    }

    public static int getRecordSize(SavantFile file) throws IOException {
        long currpos = file.getFilePointer();
        file.seek(0);
        List<Object> record = readBinaryRecord((RandomAccessFile) file, file.getFields());
        file.seek(currpos);
        return DataFormatUtils.getRecordSize(record, file.getFields());
    }

    public static List<Object> readBinaryRecord(RandomAccessFile file, List<FieldType> fields) throws IOException {

        List<Object> record = new ArrayList<Object>(fields.size());

        //System.out.println("Reading binary record");
        //System.out.println("Fields");
        //for (FieldType ft : fields) {
        //    System.out.println("\t" + ft);
        // }

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
                    record.add(new ItemRGB(r,g,b));
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
                        blocks.add(new Block(start,size));
                    }
                    record.add(blocks);
                    break;
                case STRING:
                    int len = file.readInt();
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
                    System.err.println("Not implemented yet for Field Type: " + ft);
                    break;
            }
        }

        return record;
    }

    public static FileTypeHeader readFileTypeHeader(RandomAccessFile raf) throws IOException {
        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        List<Object> record = readBinaryRecord(raf,fields);
        FileTypeHeader fth = new FileTypeHeader( FileType.fromMagicNumber((Integer) record.get(0)), (Integer) record.get(1));
        return fth;
    }

    public static void writeFileTypeHeader(RandomAccessFile raf, FileTypeHeader fth) throws IOException {
        raf.writeInt(fth.fileType.getMagicNumber());
        raf.writeInt(fth.version);

    }

    public static void writeFieldsHeader(RandomAccessFile raf, List<FieldType> fields) throws IOException {

        raf.writeInt(fields.size());

        for (FieldType ft : fields) {
            raf.writeInt(ft.ordinal());
        }
    }

    public static void writeBinaryRecord(RandomAccessFile raf, List<Object> line, List<FieldType> fields, List<Object> modifiers) throws IOException {

//        FieldType fieldType = null;
        Object o = null;
        Object instruction = null;

        int entryNum = 0;
        int numIgnores = 0;

        for (FieldType ft : fields) {
            if (ft == FieldType.IGNORE) {
                numIgnores++;
                continue;
            }

            o = line.get(entryNum);
            instruction = modifiers.get(entryNum + numIgnores);

            //System.out.println("Writing " + o + " as " + ft + " with instruction " + instruction);

            switch (ft) {
                case STRING:
                    int stringLength;
                    if (modifiers.get(entryNum) == null) {
                        stringLength = ((String) o).length();
                    } else {
                        stringLength = (Integer) instruction;
                    }
                    writeFixedLengthString(raf, (String) o, stringLength);
                    break;
                case CHAR:
                    raf.writeByte((Character) o);
                    break;
                case BLOCKS:
                    List<Block> blocks = (List<Block>) o;
                    raf.writeInt(blocks.size());
                    for (Block b : blocks) {
                        raf.writeInt(b.position);
                        raf.writeInt(b.size);
                    }
                    break;
                case ITEMRGB:
                    ItemRGB rgb = (ItemRGB) o;
                    raf.writeInt(rgb.getRed());
                    raf.writeInt(rgb.getBlue());
                    raf.writeInt(rgb.getGreen());
                    break;
                case INTEGER:
                    raf.writeInt((Integer) o);
                    break;
                case DOUBLE:
                    raf.writeDouble((Double) o);
                    break;
                case FLOAT:
                    raf.writeFloat((Float) o);
                    break;
                case LONG:
                    raf.writeLong((Long) o);
                    break;
                case BOOLEAN:
                    int bool = Integer.parseInt((String) o);
                    raf.writeInt(bool);
                    break;
                case RANGE:
                    Range r = (Range) o;
                    raf.writeInt(r.getFrom());
                    raf.writeInt(r.getTo());
                    break;
                default:
                    System.err.println("DataFormatUtils.writeBinaryRecord: Not implemented for " + ft);
                    break;
            }

            entryNum++;
        }
    }

    public static void writeFixedLengthString(RandomAccessFile raf, String s, int len) throws IOException {

        int pad = len - s.length();

        raf.writeInt(len);
        if (!s.equals("")) { raf.writeBytes(s.substring(0, Math.min(s.length(),len))); }
        while (pad > 0) {
            raf.writeBytes(" ");
            pad--;
        }

        //System.out.println("\tWriting " + len + " chars from [" + s + "] padded by " + (len - s.length()) + " = " + (after-before) + " bytes");
    }

    public static RandomAccessFile openFile(String path) throws IOException {
        return openFile(path,true);
    }

    public static RandomAccessFile openFile(String path, boolean seekToEnd) throws IOException {
        RandomAccessFile f = new RandomAccessFile(path, "rw");
        if (seekToEnd) {
            seekToEnd(f);
        }
        return f;
    }

    /**
     * Seek to the end of the given RandomAccessFile
     * @param f
     * @throws IOException
     */
    public static void seekToEnd(RandomAccessFile f) throws IOException {
        f.seek(f.length());
    }
}
