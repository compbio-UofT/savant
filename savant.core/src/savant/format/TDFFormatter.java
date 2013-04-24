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

package savant.format;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.Cytoband;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.tools.PreprocessingException;
import org.broad.igv.tools.Preprocessor;
import org.broad.igv.tools.StatusMonitor;
import org.broad.igv.track.WindowFunction;

import savant.util.MiscUtils;


/**
 * Invokes IGVTools classes to format an input file as TDF.  Note that although this
 * extends the SavantFileFormatter class, it makes no actual use of the SavantFileFormatter
 * methods.  This is only done so that it fits into our progress-bar mechanism.
 *
 * @author tarkvara
 */
public class TDFFormatter extends SavantFileFormatter {
    /** For our purposes, the only stat we want is mean. */
    protected static final List<WindowFunction> MEAN = Arrays.asList(WindowFunction.mean);

    /** 9 zoom levels is plenty */
    protected static final int DEFAULT_ZOOMS = 9;

    private static final int RECORDS_PER_INTERRUPT_CHECK = 100;
    protected static final double INFER_CHROMOSOMES_FRACTION = 0.2;
    protected static final double GENERATE_TDF_FRACTION = 0.8;

    protected int lineCount;

    public TDFFormatter(File inFile, File outFile) {
        super(inFile, outFile);
    }

    @Override
    public void format() throws InterruptedException, IOException {
        Genome genome = getTDFGenome();
        setProgress(INFER_CHROMOSOMES_FRACTION, "Generating TDF file...");
        Preprocessor pp = new Preprocessor(outFile, genome, MEAN, lineCount, new TDFProgressMonitor());
        try {
            pp.preprocess(inFile, DEFAULT_ZOOMS);
            pp.finish();
        } catch (PreprocessingException x) {
            throw new IOException(x.getMessage());
        }
    }

    /**
     * TDF formatter needs an IGV Genome object to get Chromosome sizes from.  IGV
     * provides a library of useful genomes, but we just infer the chromosome sizes
     * from the range of data found in the input file.
     *
     * @return a degenerate IGV genome object, containing only the chromosome lengths
     */
    protected Genome getTDFGenome() throws InterruptedException, IOException {
        // We have to synthesise a Genome based on the contents of the file being formatted.
        Genome tdfGenome = new Genome("unknown");

        LinkedHashMap<String, Integer> chromosomeLengths = inferChromosomes();

        // We need a map of chromosomes with the appropriate lengths.  Unfortunately,
        // TDF doesn't let us set the chromosome length directly, only implicitly by
        // adding a cytoband of the appropriate size.  Let's fake it out.
        LinkedHashMap<String, Chromosome> chromosomes = new LinkedHashMap<String, Chromosome>();
        for (String ref: chromosomeLengths.keySet()) {
            String homoRef = MiscUtils.homogenizeSequence(ref);
            Cytoband band = new Cytoband(homoRef);
            band.setStart(1);
            band.setEnd(chromosomeLengths.get(ref));
            Chromosome chrom = new Chromosome(homoRef);
            chrom.addCytoband(band);
            chromosomes.put(homoRef, chrom);
        }
        tdfGenome.setChromosomeMap(chromosomes, true);
        return tdfGenome;
    }

    /**
     * Parse a wig file to determine the chromosome sizes.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    protected LinkedHashMap<String, Integer> inferChromosomes() throws InterruptedException, IOException {

        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

        // Set the input file size (for tracking progress).
        totalBytes = inFile.length();

        inFileReader = openInputFile();

        setProgress(0.0, "Processing input file...");

        // Skip the header if it exists.
        String strLine = inFileReader.readLine();
        String[] tokens = strLine.split("\\s");

        // Read the rest of the file
        String mode = "none";
        int span = 1;
        int start = 1;
        int step = 1;
        int nextWrite = 1;
        String ref = null;
        lineCount = 1;

        try {
            // read until EOF
            while (strLine != null) {

                // split line up into tokens
                tokens = strLine.split("\\s");

                if (tokens.length < 1 || (tokens.length > 0 && (tokens[0].equals("track") || tokens[0].equals("#") || tokens[0].equals("browser")))){
                    // skip the track definition lines and comment lines and blank lines
                    strLine = inFileReader.readLine();
                    byteCount += strLine.getBytes().length + 1;
                    lineCount++;
                    continue;
                } else if (tokens[0].equals("variableStep")){
                    if(tokens.length < 2){
                        throw new IOException(String.format("Error parsing file at line %d (variableStep).", lineCount));
                    }

                    mode = "variable";
                    ref = tokens[1].substring(6);
                    span = tokens.length == 3 ? Integer.parseInt(tokens[2].substring(5)) : 1;

                } else if (tokens[0].equals("fixedStep")){
                    if (tokens.length < 4){
                        throw new IOException(String.format("Error parsing file at line %d (fixedStep).", lineCount));
                    }

                    mode = "fixed";
                    ref = tokens[1].substring(6);
                    start = Integer.parseInt(tokens[2].substring(6));
                    step = Integer.parseInt(tokens[3].substring(5));
                    span = tokens.length == 5 ? Integer.parseInt(tokens[4].substring(5)) : 1;

                } else if (tokens.length == 4) {
                    mode = "bedGraph";
                    ref = tokens[0];
                    int end = Integer.parseInt(tokens[2]);
                    nextWrite = end;

                } else if (mode.equals("variable")){
                    if (tokens.length < 2){
                        throw new IOException(String.format("Error parsing file at line %d (too few tokens on variable line).", lineCount));
                    }

                    int dest = Integer.parseInt(tokens[0]);
                    nextWrite = dest + span;

                } else if (mode.equals("fixed")){
                    nextWrite = start+span;
                    start += step;

                } else if (mode.equals("none")){
                    throw new IOException("Error parsing file (no format line)");
                }
                Integer len = result.get(ref);
                if (len == null || nextWrite > len) {
                    result.put(ref, new Integer(nextWrite));
                }

                strLine = inFileReader.readLine();
                if (strLine != null) {
                    byteCount += strLine.getBytes().length + 1;
                    lineCount++;

                    if (lineCount % RECORDS_PER_INTERRUPT_CHECK == 0) {
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        setProgress(INFER_CHROMOSOMES_FRACTION * byteCount / totalBytes, null);
                    }
                }
            }
        } finally {
            inFileReader.close();
        }

        return result;
    }

    class TDFProgressMonitor implements StatusMonitor {
        double percentComplete = 0.0;

        @Override
        public void setPercentComplete(double d) {
            percentComplete = d;
            setProgress(INFER_CHROMOSOMES_FRACTION + d * 0.01 * GENERATE_TDF_FRACTION, null);
        }

        @Override
        public void incrementPercentComplete(double d) {
            setPercentComplete(d + percentComplete);
        }

        @Override
        public boolean isInterrupted() {
            return Thread.interrupted();
        }
    }
}
