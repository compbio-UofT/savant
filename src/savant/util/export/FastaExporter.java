/*
 *    Copyright 2011-2012 University of Toronto
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
import java.util.HashMap;
import java.util.Map;

import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.util.DownloadEvent;
import savant.util.MiscUtils;
import savant.view.tracks.SequenceTrack;


/**
 * Export a Savant sequence track (or portion thereof) to a local fasta file.  This is necessary
 * because GATK only deals with local files, and not with streams.
 *
 * @author tarkvara
 */
public class FastaExporter extends TrackExporter {
    private static final int LINE_SIZE = 50;

    /** Only report progress every 100000 bases. */
    private static final int PROGRESS_INTERVAL = 100000;
    
    /** Stream for writing to destination Fasta file. */
    FastaOutputStream fastaOutput;

    /** Keeps track of the file offsets where each chromosome's data starts. */
    Map<String, Long> refOffsets = new HashMap<String, Long>();
    
    /**
     * Should be instantiated from TrackExporter.getExporter();
     */
    FastaExporter(TrackAdapter t, File f) throws IOException {
        super(t, f);
        fastaOutput = new FastaOutputStream(new FileOutputStream(destFile));
    }

    /**
     * Export the given range of the sequence track.
     *
     * @param ref the reference containing the range be exported (null to export entire genome)
     * @param r the range to be exported (null to export entire reference)
     * @param destFile the fasta file to be created
     */
    @Override
    void close() throws IOException {
        if (fastaOutput != null) {
            fastaOutput.close();
        }
    }
    
    /**
     * Export the specified reference (or subrange thereof) to the exporter's output stream.
     * This may be invoked as part of a large export (i.e. whole genome).
     * 
     * @param ref the reference containing the range be exported
     * @param r the range to be exported (must be non-null)
     * @param fastaOutput destination for fasta data
     * @throws IOException 
     */
    @Override
    void exportRange(String ref, RangeAdapter r) throws IOException, InterruptedException {
        // Write the contig name.
        fastaOutput.write('>');
        fastaOutput.write(ref.getBytes());
        fastaOutput.write('\n');
        
        refOffsets.put(MiscUtils.homogenizeSequence(ref), fastaOutput.numWritten);

        // If the range doesn't start at 1, prepend the range of interest with N characters.
        int linePos = 0;
        if (r.getFrom() > 1) {
            for (int i = 1; i < r.getFrom(); i++) {
                fastaOutput.write('N');
                if (++linePos == LINE_SIZE) {
                    fastaOutput.write('\n');
                    linePos = 0;
                }
            }
            basesSoFar += r.getFrom() - 1;
            fireEvent(new DownloadEvent((double)basesSoFar / totalBases));
        }

        fireEvent(new DownloadEvent(-1.0));
        byte[] seq = ((SequenceTrack)track).getSequence(ref, r);
        if (seq == null) {
            // No data for this contig (often a problem if chrM is missing from the Fasta file).
            seq = new byte[r.getLength()];
            for (int i = 0; i < seq.length; i++) {
                seq[i] = 'N';
            }
        }
        int j = 0;
        if (linePos > 0) {
            int numBytes = Math.min(seq.length, LINE_SIZE - linePos);
            fastaOutput.write(seq, 0, numBytes);
            fastaOutput.write('\n');
            j = numBytes;
            linePos = 0;
        }
        int lastProgress = basesSoFar;
        while (j + LINE_SIZE < seq.length) {
            fastaOutput.write(seq, j, LINE_SIZE);
            fastaOutput.write('\n');
            j += LINE_SIZE;
            basesSoFar += LINE_SIZE;
            if (basesSoFar - lastProgress > PROGRESS_INTERVAL) {
                lastProgress = basesSoFar;
                fireEvent(new DownloadEvent((double)basesSoFar / totalBases));
            }
        }
        fastaOutput.write(seq, j, seq.length - j);
        basesSoFar += seq.length - j;
        fireEvent(new DownloadEvent((double)basesSoFar / totalBases));
    }

    /**
     * First we may have to a block of Ns at the end of the fasta file to serve as fodder for
     * contigs which exist in the SAMSequenceDictionary but not in the fasta file.
     */
    public long appendFiller(SAMSequenceDictionary samDict) throws IOException {
        long fillerLength = 0;
        for (SAMSequenceRecord samRec: samDict.getSequences()) {
            if (!refOffsets.containsKey(samRec.getSequenceName())) {
                fillerLength = Math.max(fillerLength, samRec.getSequenceLength());
            }
        }
        if (fillerLength > 0) {
            OutputStream fillerOutput = new BufferedOutputStream(new FileOutputStream(destFile, true));
            int j = 0;
            while (j < fillerLength) {
                for (int k = 0; k < LINE_SIZE; k++) {
                    fillerOutput.write('N');
                }
                fillerOutput.write('\n');
                j += LINE_SIZE;
            }
            fillerOutput.close();
        }
        return fastaOutput.numWritten;
    }

    /**
     * Create the .fai (fasta index) file which describes the contents of a fasta file we've just exported.
     * We actually lie and give lengths for all contigs (not just the chromosomes in this file).  This is necessary
     * because some tools (I'm looking at you, SRMA) make a braindead check that the bam file's sequence
     * dictionary be <b>exactly</b> the same as the fasta file's dictionary.
     * 
     * @param samDict sequence dictionary from the SAM file
     * @param fillerNeeded if true, we're running the tool over the full genome, we need to make sure all contigs in the SAM sequence dictionary have non-zero offsets in the FASTA index
     */
    public void createFakeIndex(SAMSequenceDictionary samDict, boolean fillerNeeded) throws IOException {
        // If necessary append a block of filler to the end of the fasta file to accommodate contigs
        // which exist in the SAM dictionary but not in the fasta file.
        long fillerStart = 0;
        if (fillerNeeded) {
            fillerStart = appendFiller(samDict);
        }

        OutputStream faiOutput = null;
        try {
            // Index file is expected to be .fa.fai.
            faiOutput = new FileOutputStream(destFile.getAbsolutePath() + ".fai");
            
            // We need to write entries for every contig, even though they may not actually exist in the Fasta file.
            for (SAMSequenceRecord samRec: samDict.getSequences()) {
                String samRef = samRec.getSequenceName();
                long offset = fillerStart;       // If not found, by default points to the block of Ns.
                if (refOffsets.containsKey(samRef)) {
                    offset = refOffsets.get(samRef);
                }

                faiOutput.write(String.format("%s\t%d\t%d\t%d\t%d\n", samRef, samRec.getSequenceLength(), offset, LINE_SIZE, LINE_SIZE + 1).getBytes());
            }
        } finally {
            if (faiOutput != null) {
                faiOutput.close();
            }
        }
    }

    /**
     * Create the .dict (SAM sequence dictionary) file which also describes the contents of the fasta file we just exported.
     * We do this so GATK won't waste half an hour calculating an MD5 hash for the entire sequence.  Because SRMA is so
     * braindead, we totally ignore the actual fasta file and just use the dictionary from the bam file.
     */
    public void createFakeSequenceDictionary(SAMSequenceDictionary samDict) throws IOException {
        OutputStream dictOutput = null;
        try {
            // Index file is expected to be .dict (not .fa.dict).
            String fastaPath = destFile.getAbsolutePath();
            String s = fastaPath.replaceFirst("\\.(fa)|(fasta)$", ".dict");
            dictOutput = new FileOutputStream(s);
            dictOutput.write("@HD\tVN:1.0\tSO:unsorted\n".getBytes());

            // To keep GATK happy, we need to write entries for every contig, even though this file only has
            // data for a single chromosome.
            for (SAMSequenceRecord samRec: samDict.getSequences()) {
                String samRef = samRec.getSequenceName();
                dictOutput.write(String.format("@SQ\tSN:%s\tLN:%d\tUR:file:%s\tM5:00000000000000000000000000000000\n", samRef, samRec.getSequenceLength(), fastaPath).getBytes());
            }
        } finally {
            if (dictOutput != null) {
                dictOutput.close();
            }
        }
    }

    /**
     * Just a normal BufferedOutputStream, but we count the bytes as we write them
     * so that we can record the current offset.
     */
    private static class FastaOutputStream extends BufferedOutputStream {
        long numWritten = 0;

        FastaOutputStream(OutputStream os) {
            super(os);
        }

        @Override
        public synchronized void write(int b) throws IOException {
            numWritten++;
            super.write(b);
        }

        @Override
        public synchronized void write(byte b[], int off, int len) throws IOException {
            numWritten += len;
            super.write(b, off, len);
        }
    }
}
