/*
 *    Copyright 2010-2012 University of Toronto
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
import org.apache.commons.logging.LogFactory;

import savant.file.FileType;
import savant.file.FieldType;


public abstract class SavantFileFormatter {
    protected static final Log LOG = LogFactory.getLog(SavantFileFormatter.class);

    /**
     * Input file.  For now, this is usually a file URI.
     */
    protected File inFile;

    /**
     * Output file.
     */
    protected File outFile;

    protected BufferedReader inFileReader;  // input file reader

    /* PROGRESS */

    // UI ...
    protected List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    // non-UI ...
    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    // TODO: remove, or make cleaner
    // stuff needed by IO; mandated by SavantFileFormatterUtils which we're depending on
    protected List<FieldType> fields;
    protected List<Object> modifiers;

    private Integer makePercentage(Integer progress) {
        if (progress == null || progress < 0 || progress > 100) { return null; }
        int p = Math.max(0, progress);
        p = Math.min(progress, 100);
        return p;
    }

    public void setSubtaskProgress(int progress) {
        this.setSubtaskProgressAndStatus(progress,null);
    }

    public void setSubtaskStatus(String status) {
        this.setSubtaskProgressAndStatus(null,status);
    }
    
    public void incrementOverallProgress() {
        fireIncrementOverallProgress();
    }

    public abstract void format() throws InterruptedException, IOException;

    public void setSubtaskProgressAndStatus(Integer progress, String status) {
        fireStatusProgressUpdate(makePercentage(progress),status);
    }

    public void addProgressListener(FormatProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(FormatProgressListener listener) {
        listeners.remove(listener);
    }

    protected void fireStatusProgressUpdate(Integer progress, String status) {
        for (FormatProgressListener listener : listeners) {
            listener.taskProgressUpdate(progress,status);
        }
    }

    protected void fireIncrementOverallProgress() {
        for (FormatProgressListener listener : listeners) {
            listener.incrementOverallProgress();
        }
    }

    public SavantFileFormatter(File inFile, File outFile) {
        
        this.inFile = inFile;
        this.outFile = outFile;
    }


    /**
     * Open the input file.
     *
     * @throws FileNotFoundException
     */
    protected BufferedReader openInputFile() throws FileNotFoundException {
        return new BufferedReader(new FileReader(inFile));
    }

    public int getProgressAsInteger(float current, float total) {
        return (int) Math.round((current*100/total));
    }

}
