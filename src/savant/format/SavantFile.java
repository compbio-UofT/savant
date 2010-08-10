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

package savant.format;

import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;
import savant.util.RAFUtils;
import savant.util.MiscUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author mfiume
 */
public class SavantFile extends RandomAccessFile {

    FileTypeHeader fileTypeHeader;
    Map<String,Long[]> referenceMap;
    List<FieldType> fields;
    long headerOffset;
    String filename;

    public void printOffset() throws IOException {
        System.out.println(super.getFilePointer());
    }

    public SavantFile(String filename) throws IOException {
        super(filename,"rw");

        this.filename = filename;

        //System.out.println("Reading file type");
        this.fileTypeHeader = RAFUtils.readFileTypeHeader(this);

        //System.out.println("File type: " + this.fileTypeHeader.fileType);

        //System.out.println("Done... at " + super.getFilePointer() + " bytes");

        //System.out.println("Reading fields");
        this.fields = RAFUtils.readFieldsHeader(this);
        //System.out.println(fields);

        System.out.println("Number of fields: " + fields.size());

        System.out.println("Reading reference<-> data map");
        this.referenceMap = RAFUtils.readReferenceMap(this);
        for (String refname : this.referenceMap.keySet()) {
            Long[] vals = this.referenceMap.get(refname);
            System.out.println("Reference " + refname + " at " + vals[0] + " of length " + vals[1]);
        }

        //System.out.println("Making note of offset: " + super.getFilePointer());
        this.headerOffset = super.getFilePointer();
    }

    public SavantFile(String filename, FileType ft) throws IOException {
        this(filename);
        if (!fileTypeHeader.fileType.equals(ft)) {
            throw new IOException("Wrong file type");
        }        
    }

    public List<FieldType> getFields() {
        return this.fields;
    }

    public long seek(String reference, long pos) throws IOException {

        //FIXME!!!

        if (!this.containsDataForReference(reference) && 
                !this.containsDataForReference(MiscUtils.homogenizeSequence(reference))) { //FIXME: temporary fix for chrx != x issue
            //System.out.println("No data for reference: " + reference);
            return -1;
        } else if (pos >= this.getReferenceLength(reference) &&
                pos >= this.getReferenceLength(reference.substring(reference.length()-1))) { //ditto ^^
            //System.out.println("End of data for reference: " + reference);
            return -1;
        }
        else {

            long refoffset = getReferenceOffset(reference);

            //FIXME: temporary fix for chrx != x issue
            if(refoffset == -1){
                refoffset = getReferenceOffset(MiscUtils.homogenizeSequence(reference));
            }

            /*
            System.out.println("Seeking to " + (pos + refoffset + this.getHeaderOffset())
                    + " pos=" + pos
                    + " ref=" + reference
                    + " refoffset=" + refoffset
                    + " headeroffset="+this.getHeaderOffset()
                    + " file="+this.filename);
             */

            //this.headerOffset = 44;
            //REMOVE ALL OF THIS!!!!
            //refoffset = 0;
            /*System.out.println("pos: " + pos);
            System.out.println("refoffset: " + refoffset);
            System.out.println("headerOffset: " + this.getHeaderOffset());
            System.out.println(pos + refoffset + this.getHeaderOffset());
            System.out.println();*/
            


            super.seek(pos + refoffset + this.getHeaderOffset());
            return super.getFilePointer();
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos + this.getHeaderOffset());
        //System.out.println("Seeking to " + (pos + this.getHeaderOffset()) + " (pos=" + pos + ", header="+ this.getHeaderOffset() + ")");
        //System.out.println("warning: consider calling seek (string reference, long pos) instead");
    }

    public void seekSuper(long pos) throws IOException {
        super.seek(pos);
    }

    @Override
    public long length() throws IOException {
        return super.length() - this.getHeaderOffset();
    }

    public long lengthSuper() throws IOException {
        return super.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer() - this.getHeaderOffset();
    }

    public long getFilePointerSuper() throws IOException {
        return super.getFilePointer();
    }

    public long getHeaderOffset() {
        return this.headerOffset;
    }

    public FileType getFileType() {
        return this.fileTypeHeader.fileType;
    }

    public int getFileTypeVersion() {
        return this.fileTypeHeader.version;
    }

    public Set<String> getReferenceNames() {
        return this.referenceMap.keySet();
    }

    public Map<String, Long[]> getReferenceMap() {
        return this.referenceMap;
    }

    public long getReferenceOffset(String reference) {
        if (!this.containsDataForReference(reference)) { return -1; }
        return this.referenceMap.get(reference)[0];
    }

    public long getReferenceLength(String reference) {
        if (!this.containsDataForReference(reference)) { return -1; }
        return this.referenceMap.get(reference)[1];
    }

    public boolean containsDataForReference(String reference) {
        return this.referenceMap.containsKey(reference);
    }

}
