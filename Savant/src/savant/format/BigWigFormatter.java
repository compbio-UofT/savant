/*
 *    Copyright 2011 University of Toronto
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;

import savant.file.FileType;
import savant.file.FieldType;
import savant.util.MiscUtils;


/**
 * Uses Broad Institute's bigwig library to format a bigWig file into a Savant continuous file.
 *
 * @author tarkvara
 */
public class BigWigFormatter extends SavantFileFormatter {
    private static final int RECORDS_PER_INTERRUPT_CHECK = 100;
    private BBFileReader bbReader;

    public BigWigFormatter(File inFile, File outFile) {
        super(inFile, outFile, FileType.CONTINUOUS_GENERIC);
    }

    @Override
    public void format() throws InterruptedException, IOException {

        try {
            bbReader = new BBFileReader(inFile.getAbsolutePath());
            BBFileHeader bbHeader = bbReader.getBBFileHeader();
            if (!bbReader.isBigWigFile()) {
                throw new IOException("Input is not a BigWig file.");
            }

            fields = new ArrayList<FieldType>();
            fields.add(FieldType.FLOAT);

            modifiers = new ArrayList<Object>();
            modifiers.add(null);

            setSubtaskStatus("Processing input file ...");
            incrementOverallProgress();

            int nextWrite = 0;

            List<String> chromNames = bbReader.getChromosomeNames();
            for (String chrom: chromNames) {
                bbReader.getChromosomeBounds();
            }

            BigWigIterator wigIterator = bbReader.getBigWigIterator();
            int j = 0;
            while (wigIterator.hasNext()) {
                WigItem wig = wigIterator.next();
                LOG.info(wig.toString());
                DataOutputStream output = getFileForReference(wig.getChromosome());

                int start = wig.getStartBase();
                int end = wig.getEndBase();
                while (nextWrite < start) {
                    output.writeFloat(0.0F);
                    nextWrite++;
                }
                while (nextWrite < end) {
                    output.writeFloat(wig.getWigValue());
                    nextWrite++;
                }
                if (j == RECORDS_PER_INTERRUPT_CHECK) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    j = 0;
                } else {
                    j++;
                }
                setSubtaskProgress(getProgressAsInteger(byteCount, totalBytes));
            }
        } finally {
            closeOutputStreams();
        }

        // map of reference name -> multiresolution *index* filename
        Map<String,String> refnameToIndexFileNameMap = ContinuousFormatterHelper.makeMultiResolutionContinuousFiles(referenceName2FilenameMap);

        List<String> refnames = MiscUtils.set2List(this.referenceName2FilenameMap.keySet());
        writeContinuousOutputFile(refnames, refnameToIndexFileNameMap, referenceName2FilenameMap);
    }

    private void fillWithZeros(int current, int dest, DataOutputStream output) throws IOException {
    	for (int i = current; i < dest;i++) {
            output.writeFloat(0.0f);
        }
    }
}
