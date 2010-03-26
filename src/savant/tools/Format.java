/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools;

import savant.format.DataFormatter;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.util.RAFUtils;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mfiume
 */
public class Format {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //read();
        format();
    }
    
    private static void format() {
        boolean success = true;

        //success &= formatContinuousWIG();
        success &= formatContinuousGeneric();

        //success &= formatIntervalGFF();
        //success &= formatIntervalBED();
        success &= formatIntervalGeneric();
        success &= formatSequence();
        success &= formatGenericPoint();

        if (success) { System.out.println("Formatting completed successfully"); }
        else { System.out.println("Formatting unsuccessful"); }
    }

    private static boolean formatGenericPoint() {
        String infile = "C:\\sandbox\\debut\\data\\samplepoint.txt";
        String outfile = "C:\\sandbox\\debut\\data\\samplepoint.debut";
        FileType fileType = FileType.POINT_GENERIC;
        HashMap<String, Object> instructions = new HashMap<String, Object>();
        instructions.put("DescriptionLength", 10);
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatSequence() {
        String infile = "C:\\sandbox\\debut\\data\\chr1.fa";
        String outfile = "C:\\sandbox\\debut\\data\\chr1.debut";
        FileType fileType = FileType.SEQUENCE_FASTA;
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatContinuousGeneric() {
        String infile = "C:\\sandbox\\debut\\data\\sample.continuous.txt";
        String outfile = "C:\\sandbox\\debut\\data\\sample.continuous.debut";
        FileType fileType = FileType.CONTINUOUS_GENERIC;
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatContinuousWIG() {
        String infile = "C:\\sandbox\\debut\\data\\sample.continuous.wig";
        String outfile = "C:\\sandbox\\debut\\data\\sample.continuous.wig.debut";
        FileType fileType = FileType.CONTINUOUS_WIG;
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatIntervalGeneric() {
        //String infile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.txt";
        //String outfile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.debut";
        String infile = "C:\\sandbox\\debut\\data\\genes.chr1.txt";
        String outfile = "C:\\sandbox\\debut\\data\\genes.chr1.debut";
        FileType fileType = FileType.INTERVAL_GENERIC;
        HashMap<String, Object> instructions = new HashMap<String, Object>();
        instructions.put("MinLeafRangeLength", 4000);
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatIntervalBED() {
        //String infile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.txt";
        //String outfile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.debut";
        String infile = "C:\\sandbox\\debut\\data\\genes.chr1.bed";
        String outfile = "C:\\sandbox\\debut\\data\\genes.chr1.bed.debut";
        FileType fileType = FileType.INTERVAL_BED;
        HashMap<String, Object> instructions = new HashMap<String, Object>();
        instructions.put("MinLeafRangeLength", 4000);
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static boolean formatIntervalGFF() {
        //String infile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.txt";
        //String outfile = "C:\\sandbox\\debut\\data\\sampleintervalgeneric.debut";
        String infile = "C:\\sandbox\\debut\\data\\sample.gff";
        String outfile = "C:\\sandbox\\debut\\data\\sample.gff.debut";
        FileType fileType = FileType.INTERVAL_GFF;
        HashMap<String, Object> instructions = new HashMap<String, Object>();
        instructions.put("MinLeafRangeLength", 4000);
        DataFormatter df = new DataFormatter(infile, outfile, fileType);
        return df.format();
    }

    private static void read() {
        try {
            RandomAccessFile raf = new RandomAccessFile("C:\\sandbox\\debut\\data\\genes.chr1.debut", "r");
            FileTypeHeader fth = RAFUtils.readFileTypeHeader(raf);
            System.out.println("File Type: " + fth.fileType + ", version: " + fth.version);
        } catch (Exception ex) {
            Logger.getLogger(Format.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


}
