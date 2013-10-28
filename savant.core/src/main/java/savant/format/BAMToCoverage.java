/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.tools.PreprocessingException;
import org.broad.igv.tools.Preprocessor;

import savant.util.MiscUtils;


/**
 * Special case of the TDF formatter, which uses the IGVTools "count" command to generate
 * a coverage file.
 *
 * @author tarkvara
 */
public class BAMToCoverage extends TDFFormatter {
    private static final int DEFAULT_WINDOW_SIZE = 25;
    private static final int DEFAULT_EXT_FACTOR = 0;
    private static final int DEFAULT_STRAND_OPTION = -1;

    public BAMToCoverage(File inFile) {
        super(inFile, new File(inFile.getAbsolutePath() + ".cov.tdf"));
    }

    @Override
    public void format() throws InterruptedException, IOException {
        Genome genome = getTDFGenome();
        setProgress(INFER_CHROMOSOMES_FRACTION, "Generating TDF file...");
        Preprocessor pp = new Preprocessor(outFile, genome, MEAN, 1000000, new TDFProgressMonitor());
        try {
            pp.count(inFile.getAbsolutePath(), DEFAULT_WINDOW_SIZE, DEFAULT_EXT_FACTOR, DEFAULT_ZOOMS, null, null, null);
            pp.finish();
        } catch (PreprocessingException x) {
            throw new IOException(x);
        }
    }

    @Override
    protected LinkedHashMap<String, Integer> inferChromosomes() throws InterruptedException, IOException {

        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

        inFileReader = openInputFile();

        setProgress(0.0, "Processing input file...");

        SAMFileReader samFileReader = new SAMFileReader(inFile, determineIndexFile());

        try {
            SAMFileHeader fileHeader = samFileReader.getFileHeader();
            SAMSequenceDictionary dictionary = fileHeader.getSequenceDictionary();
            List<SAMSequenceRecord> sequences = dictionary.getSequences();
            for (SAMSequenceRecord sequence : sequences) {
                result.put(MiscUtils.homogenizeSequence(sequence.getSequenceName()), sequence.getSequenceLength());
            }
        } finally {
            if (samFileReader != null) {
                samFileReader.close();
            }
        }

        return result;
    }

    private File determineIndexFile() throws IOException {

        // determine default track name from filename
        //int lastSlashIndex = inFile.lastIndexOf(System.getProperty("file.separator"));
        //String name = inFile.substring(lastSlashIndex+1, inFile.length());

        // infer index file name from track filename
        String path = inFile.getAbsolutePath();
        String pathWithoutExtension;
        int lastIndex = path.lastIndexOf(".bam");
        if (lastIndex == -1) {
            throw new IOException("BAM files should end with the \".bam\" file extension.");
        } else {
            pathWithoutExtension = path.substring(0, lastIndex);
        }
        //String pathWithoutExtension = inFile.substring(0, inFile.lastIndexOf(".bam"));
        File f = new File(path + ".bai");
        if (f.exists()) {
            return f;
        } else {
            f = new File(pathWithoutExtension + ".bai");
            if (f.exists()) {
                return f;
            }
        }
        throw new FileNotFoundException("Unable to find index file for " + inFile + ".");
    }
}
