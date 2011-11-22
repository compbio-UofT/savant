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

package savant.util.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.SequenceRecord;
import savant.api.util.GenomeUtils;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.util.DownloadEvent;
import savant.util.MiscUtils;
import savant.util.Range;


/**
 * Export a Savant sequence track (or portion thereof) to a local fasta file.  This is necessary
 * because GATK only deals with local files, and not with streams.
 *
 * @author tarkvara
 */
public class FastaExporter extends TrackExporter {
    private static final int LINE_SIZE = 50;
    
    /** Download sequence data in 1MB chunks. */
    private static final int CHUNK_SIZE = 1000000;

    /** Bases exported so far. */
    private int basesSoFar;

    /** Total length to be exported. */
    private int totalBases;

    /**
     * Should be instantiated from TrackExporter.getExporter();
     */
    FastaExporter(TrackAdapter t) {
        super(t);
    }

    /**
     * Export the given range of the sequence track.
     *
     * @param ref the reference containing the range be exported (null to export entire genome)
     * @param r the range to be exported (null to export entire reference)
     * @param destFile the fasta file to be created
     */
    @Override
    public void exportRange(String ref, RangeAdapter r, File destFile) throws IOException {
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(destFile));

            LocationController lc = LocationController.getInstance();
            if (ref == null) {
                for (String ref2: lc.getReferenceNames()) {
                    totalBases += lc.getReferenceLength(ref2);
                }
                for (String ref2: lc.getReferenceNames()) {
                    exportRangeToStream(ref2, new Range(1, lc.getReferenceLength(ref2)), output);
                }
            } else {
                totalBases = LocationController.getInstance().getReferenceLength(ref);
                if (r == null) {
                    r = new Range(1, totalBases);
                }
                exportRangeToStream(ref, r, output);
            }
            createIndex(ref, destFile);
            createSequenceDictionary(destFile);
            fireEvent(new DownloadEvent(destFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    /**
     * Export the specified reference (or subrange thereof) to the given output stream.
     * This may be invoked as part of a large export (i.e. whole genome).
     * 
     * @param ref the reference containing the range be exported
     * @param r the range to be exported (must be non-null)
     * @param output destination for fasta data
     * @throws IOException 
     */
    private void exportRangeToStream(String ref, RangeAdapter r, OutputStream output) throws IOException {
        // Write the contig name.
        output.write('>');
        output.write(ref.getBytes());
        output.write('\n');

        // If the range doesn't start at 1, prepend the range of interest with N characters.
        int linePos = 0;
        if (r.getFrom() > 1) {
            for (int i = 1; i < r.getFrom(); i++) {
                output.write('N');
                if (++linePos == LINE_SIZE) {
                    output.write('\n');
                    linePos = 0;
                }
            }
        }

        double len = r.getTo() - r.getFrom();
        for (int i = r.getFrom(); i < r.getTo(); i += CHUNK_SIZE) {
            byte[] seq = ((SequenceRecord)track.getDataSource().getRecords(ref, new Range(i, i + CHUNK_SIZE - 1), Resolution.HIGH).get(0)).getSequence();
            int j = 0;
            if (linePos > 0) {
                int numBytes = Math.min(seq.length, LINE_SIZE - linePos);
                output.write(seq, 0, numBytes);
                output.write('\n');
                j = numBytes;
                linePos = 0;
            }
            while (j + LINE_SIZE < seq.length) {
                output.write(seq, j, LINE_SIZE);
                output.write('\n');
                j += LINE_SIZE;
            }
            output.write(seq, j, seq.length - j);
            linePos = seq.length - j;
            basesSoFar += seq.length;
            fireEvent(new DownloadEvent((double)basesSoFar / totalBases));
        }
    }

    /**
     * Create the .fai (fasta index) file which describes the contents of the fasta file we just exported.
     * We actually lie and give lengths for all contigs (not just the chromosome in this file).
     */
    private void createIndex(String ref, File fastaFile) throws IOException {
        OutputStream output = null;
        try {
            // Index file is expected to be .fa.fai.
            output = new FileOutputStream(fastaFile.getAbsolutePath() + ".fai");
            
            // To keep GATK happy, we need to write entries for every contig, even though this file only has
            // data for a single chromosome.
            Set<String> allRefs = GenomeUtils.getGenome().getReferenceNames();
            for (String ref2: allRefs) {
                int numBases = LocationController.getInstance().getReferenceLength(ref2);

                // Only the ref being exported has a proper start position; others just have a placeholder.
                int start = Integer.MAX_VALUE;
                if (ref2.equals(ref)) {
                    start = ref.length() + 2;
                }
                String homoRef = MiscUtils.homogenizeSequence(ref2);
                if (!homoRef.equals(ref2)) {
                    output.write(String.format("%s\t%d\t%d\t%d\t%d\n", homoRef, numBases, start, LINE_SIZE, LINE_SIZE + 1).getBytes());
                }
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Create the .dict (SAM sequence dictionary) file which also describes the contents of the fasta file we just exported.
     * We do this so GATK won't waste half an hour calculating an MD5 hash for the entire sequence.
     */
    private void createSequenceDictionary(File fastaFile) throws IOException {
        OutputStream output = null;
        try {
            // Index file is expected to be .dict (not .fa.dict).
            String fastaPath = fastaFile.getAbsolutePath();
            String s = fastaPath.replaceFirst("\\.(fa)|(fasta)$", ".dict");
            output = new FileOutputStream(s);
            output.write("@HD\tVN:1.0\tSO:unsorted\n".getBytes());

            // To keep GATK happy, we need to write entries for every contig, even though this file only has
            // data for a single chromosome.
            Set<String> allRefs = GenomeUtils.getGenome().getReferenceNames();
            for (String ref: allRefs) {
                int numBases = LocationController.getInstance().getReferenceLength(ref);
                String homoRef = MiscUtils.homogenizeSequence(ref);
                output.write(String.format("@SQ\tSN:%s\tLN:%d\tUR:file:%s\tM5:00000000000000000000000000000000\n", homoRef, numBases, fastaPath).getBytes());
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
