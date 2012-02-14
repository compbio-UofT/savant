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

public abstract class SavantFileFormatter {

    /** Input file. */
    protected final File inFile;

    /** Output file. */
    protected final File outFile;

    protected BufferedReader inFileReader;

    private final FormatProgressListener progressListener;

    // non-UI ...
    // variables to keep track of progress processing the input file(s)
    protected long totalBytes;
    protected long byteCount;

    private Integer makePercentage(Integer progress) {
        if (progress == null || progress < 0 || progress > 100) { return null; }
        int p = Math.max(0, progress);
        p = Math.min(progress, 100);
        return p;
    }

    public void setSubtaskProgress(int progress) {
        setSubtaskProgressAndStatus(progress, null);
    }

    public void setSubtaskStatus(String status) {
        setSubtaskProgressAndStatus(null, status);
    }
    
    public void incrementOverallProgress() {
        progressListener.incrementOverallProgress();
    }

    public abstract void format() throws InterruptedException, IOException;

    public void setSubtaskProgressAndStatus(Integer progress, String status) {
        progressListener.taskProgressUpdate(makePercentage(progress), status);
    }

    public SavantFileFormatter(File inFile, File outFile, FormatProgressListener listener) {
        
        this.inFile = inFile;
        this.outFile = outFile;
        progressListener = listener;
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
        return (int)Math.round((current * 100 / total));
    }
}
