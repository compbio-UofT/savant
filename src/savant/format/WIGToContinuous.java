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

/*
 * WIGToContinuous.java
 * Created on Mar 1, 2010
 */

package savant.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class to convert a WIG file to a Savant continuous generic file.
 */
public class WIGToContinuous {

    private static final Log log = LogFactory.getLog(WIGToContinuous.class);

    public static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    private String inFile;
    private String outFile;
    private DataOutputStream out;
    
    // stuff needed by IO; mandated by DataFormatUtils which we're depending on
    private List<FieldType> fields;
    private List<Object> modifiers;
    
    // variables to keep track of progress processing the input file(s)
    private long totalBytes;
    private long byteCount;
    private int progress; // 0 to 100%
    private List<FormatProgressListener> listeners = new ArrayList<FormatProgressListener>();

    public WIGToContinuous(String inFile, String outFile) {
        this.inFile = inFile;
        this.outFile = outFile;

        initOutput();
    }

    public void format() throws InterruptedException, ParseException {

        try {
            File inputFile = new File(inFile);
            
            // Initialize the total size of the input file, for purposes of tracking progress
            this.totalBytes = inputFile.length();

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            String lineIn;
            String[] tokens;

            // Skip the header if it exists.

            lineIn = reader.readLine();
            // update bytes read from input
            this.byteCount += lineIn.getBytes().length;

            tokens = lineIn.split("\\s");
            if (tokens.length > 0 && tokens[0].equals("track")){
            	lineIn = reader.readLine();
                // update bytes read from input
                this.byteCount += lineIn.getBytes().length;
            }


            //Read the rest of the file
            String mode = "none";
            int span = 1;
            int start = 1;
            int step = 1;
            int nextWrite = 1;

            boolean done = false;
            while (!done) {

                for (int j=0; j<RECORDS_PER_INTERRUPT_CHECK; j++) {
                    if (lineIn == null) {
                        done = true;
                        break;
                    }

                    tokens = lineIn.split("\\s");

                    if(tokens.length < 1){
                        //skip blank lines
                        continue;
                    }
                    if (tokens[0].equals("variableStep")){
                        if(tokens.length < 2){
                            closeOutput();
                            throw new ParseException("Error parsing file (variableStep line)", 0);
                            //log.fatal("Error parsing file (variableStep line)");
                            //System.exit(1);
                        }
                        mode = "variable";
                        if(tokens.length == 3){
                            span = Integer.parseInt(tokens[2].substring(5));
                        } else {
                            span = 1;
                        }
                    } else if (tokens[0].equals("fixedStep")){
                        if(tokens.length < 4){
                            closeOutput();
                            throw new ParseException("Error parsing file (fixedStep line)", 0);
                            //log.fatal("Error parsing file (fixedStep line)");
                            //System.exit(1);
                        }
                        mode = "fixed";

                        start = Integer.parseInt(tokens[2].substring(6));
                        step = Integer.parseInt(tokens[3].substring(5));

                        if (tokens.length == 5){
                            span = Integer.parseInt(tokens[4].substring(5));
                        } else {
                            span = 1;
                        }
                    } else if (mode.equals("variable")){
                        if (tokens.length < 2){
                            closeOutput();
                            throw new ParseException("Error parsing file (to few tokens on varialbe line)", 0);
                            //log.fatal("Error parsing file (to few tokens on varialbe line)");
                            //System.exit(1);
                        }
                        int dest = Integer.parseInt(tokens[0]);
                        this.fillWithZeros(nextWrite,dest,out);
                        float val = Float.parseFloat(tokens[1]);
                        for (int i = 0; i < span; i++){
                            out.writeFloat(val);
                        }
                        nextWrite = dest + span;

                    } else if (mode.equals("fixed")){
                        this.fillWithZeros(nextWrite,start,out);
                        float val = Float.parseFloat(tokens[0]);
                        for (int i = 0; i < span; i++){
                            out.writeFloat(val);
                        }
                        nextWrite = start+span;
                        start += step;


                    } else if (mode.equals("none")){
                        closeOutput();
                        throw new ParseException("Error parsing file (no format line)", 0);
                        //log.fatal("Error parsing file (no format line)");
                        
                        //System.exit(1);
                    }

                    lineIn = reader.readLine();
                    // update bytes read from input
                    if (lineIn != null) {
                        this.byteCount += lineIn.getBytes().length;
                    }

                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                updateProgress();
            }


        } catch (FileNotFoundException e) {
            log.error("File not found " + inFile);
        } catch (IOException io) {
            log.error("Error converting file " + inFile, io);
        } finally {
            closeOutput();
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

    private void fireProgressUpdate(int value) {
        for (FormatProgressListener listener : listeners) {
            listener.progressUpdate(value);
        }
    }

    private void initOutput() {

        try {
            // open output stream
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

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
            log.error("Error preparing output file", e);
        }
    }

    private void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            log.warn("Error closing output file", e);
        }
    }
    
    private void fillWithZeros(int curent, int dest,DataOutputStream out) throws IOException{
    	for (int i = curent; i < dest;i++){
    		out.writeFloat(0.0f);
    	}

    }

    private void updateProgress() {
        float proportionDone = (float)this.byteCount/(float)this.totalBytes;
        int percentDone = (int)Math.round(proportionDone * 100.0);
        setProgress(percentDone);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Missing argument: input file and output file required");
            System.exit(1);
        }
        System.out.println("Start process: " + new Date().toString());
        WIGToContinuous instance = new WIGToContinuous(args[0], args[1]);
        try {
            instance.format();
        } catch (InterruptedException e) {
            System.out.println("Formatting interrupted.");
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
        finally {
            System.out.println("End process: " + new Date().toString());
        }
    }
}
