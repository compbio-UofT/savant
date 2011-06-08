/*
 * FormatTool.java
 * Created on Nov 7, 2010
 *
 *
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

package savant.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import savant.file.FileType;
import savant.format.DataFormatter;
import savant.format.SavantFileFormatterUtils;
import savant.format.SavantFileFormattingException;

/**
 * Command-line format utility.
 *
 * @author tarkvara
 */

public class FormatTool {

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String args[]) {
        try {
            File inFile = null;
            File outFile = null;
            FileType ft = null;
            boolean oneBased = false;
            boolean forceOneBased = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-t")) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("File type not specified.");
                    }
                    ft = parseFileType(args[++i]);
                } else if (args[i].equals("-1")) {
                    forceOneBased = true;
                    oneBased = true;
                } else if (inFile == null) {
                    inFile = new File(args[i]);
                } else if (outFile == null) {
                    outFile = new File(args[i]);
                } else {
                    throw new IllegalArgumentException(String.format("Unrecognised command line argument: %s.", args[i]));
                }
            }

            if (inFile == null) {
                throw new IllegalArgumentException("Input file not specified.");
            }
            if (!inFile.exists()) {
                throw new FileNotFoundException(String.format("File not found: %s.", inFile.getAbsolutePath()));
            }
            if (ft == null) {
                ft = inferFileType(inFile);
                if (ft == null) {
                    throw new IllegalArgumentException(String.format("Unable to determine file type of %s.", inFile.getName()));
                }
            }
            if (!forceOneBased) {
                oneBased = inferOneBased(ft);
            }
            if (outFile == null) {
                outFile = new File(inFile.getAbsolutePath() + ".savant");
            }
            try {
                DataFormatter df = new DataFormatter(inFile, outFile, ft, oneBased);
                df.format();
            } catch (InterruptedException ix) {
                System.err.println("Formatting interrupted.");
            } catch (IOException iox) {
                System.err.println("Fatal I/O error.");
                System.err.println(iox.getMessage());
            } catch (ParseException px) {
                System.err.println("Fatal parse error.");
                System.err.println(px.getMessage());
            } catch (SavantFileFormattingException sffx) {
                System.err.println(sffx.getMessage());
            }
        } catch (Exception x) {
            // We get here for usage exceptions.  Actual processing exceptions are
            // all caught inside the block.
            System.err.println(x.getMessage());
            System.err.println();
            usage();
        }
    }

    private static FileType parseFileType(String arg) {
        String s = arg.toLowerCase();
        if (s.equals("fasta")) {
            return FileType.SEQUENCE_FASTA;
        }
        if (s.equals("bed")) {
            return FileType.INTERVAL_BED;
        }
        if (s.equals("gff")) {
            return FileType.INTERVAL_GFF;
        }
        if (s.equals("bam")) {
            return FileType.INTERVAL_BAM;
        }
        if (s.equals("wig") || s.equals("bedgraph")) {
            return FileType.CONTINUOUS_WIG;
        }
        if (s.equals("interval")) {
            return FileType.INTERVAL_GENERIC;
        }
        if (s.equals("point")) {
            return FileType.POINT_GENERIC;
        }
        if (s.equals("continuous")) {
            return FileType.CONTINUOUS_GENERIC;
        }
        throw new IllegalArgumentException(String.format("Unknown file type: %s.", arg));
    }

    /**
     * Guess the FileType if the user hasn't specified one.  Right now, this is based
     * on code in SavantFileFormatterUtils, which looks only at the file extension.
     *
     * @param   inFile  path to the input file
     * @return  the file's type, or null if it wasn't recognised
     */
    private static FileType inferFileType(File inFile) {
        return SavantFileFormatterUtils.guessFileTypeFromPath(inFile.getAbsolutePath());
    }

    private static boolean inferOneBased(FileType ft) {
        switch (ft) {
            case SEQUENCE_FASTA:
            case INTERVAL_GFF:
            case INTERVAL_BAM:
            case CONTINUOUS_WIG:
                return true;
            case INTERVAL_BED:
                return false;
            default:
                // For the generic formats, if they didn't specify "-1", they must mean zero-based.
                return false;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void usage() {
        System.err.println("Usage: FormatTool [-t type] [-1] inFile [outFile]");
        System.err.println("    -t       file type (one of FASTA, BED, GFF, BAM, WIG, BedGraph, Interval,");
        System.err.println("             Point, Continuous; if omitted, will try to infer from file");
        System.err.println("             extension)");
        System.err.println("    -1       treat the file as one-based (default for FASTA, GFF, BAM, WIG, and");
        System.err.println("             BedGraph)");
        System.err.println("    inFile   the unformatted input file (required)");
        System.err.println("    outFile  the output file (if omitted, will default to inFile.savant)");
    }
}
