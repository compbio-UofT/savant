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

import java.io.*;
import java.util.*;
import org.apache.commons.logging.Log;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

public class GenericFormatter {

    protected Log log;
    protected static final int OUTPUT_BUFFER_SIZE = 1024 * 128; // 128K
    
    protected DataOutputStream out;
    protected BufferedReader inputFile;
    protected String inFile;      // xx.bam file
    protected String outFile;
    protected String sortPath;

    // variables to keep track of progress processing the input file(s)
    protected long positionCount=0;
    protected int progress=0; // 0 to 100%
    protected List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    // stuff needed by IO; mandated by DataFormatUtils which we're depending on
    protected List<FieldType> fields;
    protected List<Object> modifiers;

    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    protected String tmpOutPath = "tmp";
    protected String indexExtension = ".index";

    protected int baseOffset = 0; // 0 if 1-based; 1 if 0-based

    protected void initOutput() {

        try {
            // open output stream
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile), OUTPUT_BUFFER_SIZE));

            // write file type header
            FileTypeHeader fileTypeHeader = new FileTypeHeader(FileType.CONTINUOUS_GENERIC, 1);
            out.writeInt(fileTypeHeader.fileType.getMagicNumber());
            out.writeInt(fileTypeHeader.version);

            // prepare and write fields header
            fields = new ArrayList<FieldType>();
            fields.add(FieldType.FLOAT);
            modifiers = new ArrayList<Object>();
            modifiers.add(null);
            out.writeInt(fields.size());
            for (FieldType ft : fields) {
                out.writeInt(ft.ordinal());
            }
        } catch (IOException e) {
            log.error("Error initializing output file " + outFile, e);
        }
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        fireProgressUpdate(progress);

    }

    public void addProgressListener(FormatProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        listeners.remove(listener);
    }

    protected void fireProgressUpdate(int value) {
        for (FormatProgressListener listener : listeners) {
            listener.progressUpdate(value);
        }
    }

    protected void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            log.warn("Error closing output file", e);
        }
    }

    protected void updateProgress() {
        float proportionDone = (float)this.byteCount/(float)this.totalBytes;
        int percentDone = (int)Math.round(proportionDone * 100.0);
        setProgress(percentDone);
    }

    /**
     * Open the input file
     * @return
     * @throws FileNotFoundException
     */
    protected BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFile));
    }

    protected DataOutputStream openNewTmpOutputFile() throws IOException {
        deleteTmpOutputFile();
        return openTmpOutputFile();
    }

    /**
     * Open the output file
     * @return
     * @throws FileNotFoundException
     */
    protected DataOutputStream openTmpOutputFile() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpOutPath)));
    }

    protected void deleteTmpOutputFile() {
        File f = new File(tmpOutPath);
        if (f.exists()) {
            f.delete();
        }
    }

    protected void deleteOutputFile() {
        File f = new File(outFile);
        if (f.exists()) {
            f.delete();
        }
    }

}
