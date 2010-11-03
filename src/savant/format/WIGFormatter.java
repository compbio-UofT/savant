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

package savant.format;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import savant.file.FileType;
import savant.file.FieldType;
import savant.util.MiscUtils;

public class WIGFormatter extends SavantFileFormatter {

    private static final int RECORDS_PER_INTERRUPT_CHECK = 100;

    // variables to keep track of progress processing the input file(s)
    private long totalBytes;
    private long byteCount;

    public WIGFormatter(String inFile, String outFile) {
        super(inFile, outFile, FileType.CONTINUOUS_GENERIC);
    }

    private void updateByteCount(String s) {
        if (s != null) byteCount += s.getBytes().length;
    }

    public void format() throws InterruptedException, ParseException, IOException {

        fields = new ArrayList<FieldType>();
        fields.add(FieldType.FLOAT);

        modifiers = new ArrayList<Object>();
        modifiers.add(null);

        // set the input file size (for tracking progress)
        this.totalBytes = new File(inFilePath).length();

        // open the input file
        inFileReader = this.openInputFile();

        DataOutputStream outfile = null;

        this.setSubtaskStatus("Processing input file ...");
        this.incrementOverallProgress();

        //Read File Line By Line
        try {
            String strLine;
            boolean done = false;
        
            String[] tokens;

            // Skip the header if it exists.

            strLine = inFileReader.readLine();

            // update bytes read from input
            updateByteCount(strLine);

            tokens = strLine.split("\\s");

            //Read the rest of the file
            String mode = "none";
            int span = 1;
            int start = 1;
            int step = 1;
            int nextWrite = 1;

            // read until EOF
            while (!done) {

                // stop to update progress now and then
                for (int j=0; j<RECORDS_PER_INTERRUPT_CHECK; j++) {

                    // break if hit EOF
                    if (strLine == null) {
                        done = true;
                        break;
                    }

                    // split line up into tokens
                    tokens = strLine.split("\\s");

                    if (tokens.length < 1 || (tokens.length > 0 && (tokens[0].equals("track") || tokens[0].equals("#")))){
                        // skip the track definition lines and comment lines and blank lines
                        strLine = inFileReader.readLine();
                        // update bytes read from input
                        updateByteCount(strLine);
                        continue;
                    }
                    else if (tokens[0].equals("variableStep")){
                     // variable step
                        if(tokens.length < 2){
                            this.closeOutputStreams();
                            this.deleteOutputStreams();
                            throw new ParseException("Error parsing file (variableStep line)", 0);
                        }

                        String sectok = tokens[1];
                        int splitIndex = sectok.indexOf("=") + 1;
                        String refname = tokens[1].substring(splitIndex);

                        outfile = this.getFileForReference(refname);

                        mode = "variable";
                        if(tokens.length == 3){
                            span = Integer.parseInt(tokens[2].substring(5));
                        } else {
                            span = 1;
                        }
                    } else if (tokens[0].equals("fixedStep")){
                        if(tokens.length < 4){
                            this.closeOutputStreams();
                            this.deleteOutputStreams();
                            throw new ParseException("Error parsing file (fixedStep line)", 0);
                        }
                        mode = "fixed";
                        
                        String sectok = tokens[1];
                        int splitIndex = sectok.indexOf("=") + 1;
                        String refname = tokens[1].substring(splitIndex);

                        outfile = this.getFileForReference(refname);

                        start = Integer.parseInt(tokens[2].substring(6));
                        step = Integer.parseInt(tokens[3].substring(5));

                        if (tokens.length == 5){
                            span = Integer.parseInt(tokens[4].substring(5));
                        } else {
                            span = 1;
                        }
                    } else if (tokens.length == 4) {

                        mode = "bedGraph";

                        String refName = tokens[0];
                        outfile = this.getFileForReference(refName);

                        int dest = Integer.parseInt(tokens[1]);
                        this.fillWithZeros(nextWrite,dest,outfile);
                        
                        float val = Float.parseFloat(tokens[3]);
                        int end = Integer.parseInt(tokens[2]);
                        for (int i=dest; i<end; i++) {
                            outfile.writeFloat(val);
                        }
                        nextWrite = end;

                    } else if (mode.equals("variable")){
                        if (tokens.length < 2){
                            this.closeOutputStreams();
                            this.deleteOutputStreams();
                            throw new ParseException("Error parsing file (too few tokens on variable line)", 0);
                        }

                        int dest = Integer.parseInt(tokens[0]);
                        this.fillWithZeros(nextWrite,dest,outfile);
                        float val = Float.parseFloat(tokens[1]);
                        for (int i = 0; i < span; i++){
                            outfile.writeFloat(val);
                        }
                        nextWrite = dest + span;

                    } else if (mode.equals("fixed")){
                        this.fillWithZeros(nextWrite,start,outfile);
                        float val = Float.parseFloat(tokens[0]);

                        for (int i = 0; i < span; i++){
                            outfile.writeFloat(val);
                        }
                        nextWrite = start+span;
                        start += step;


                    } else if (mode.equals("none")){
                        this.closeOutputStreams();
                        this.deleteOutputStreams();
                        throw new ParseException("Error parsing file (no format line)", 0);
                    }

                    strLine = inFileReader.readLine();
                    // update bytes read from input
                    updateByteCount(strLine);

                }
                // check to see if format has been cancelled
                if (Thread.interrupted()) throw new InterruptedException();
                // update progress property for UI
                this.setSubtaskProgress(this.getProgressAsInteger(byteCount, totalBytes));
            }

        } catch (FileNotFoundException e) {
            log.error("File not found " + inFilePath);
        } catch (IOException io) {
            log.error("Error converting file " + inFilePath, io);
        } finally {
            this.closeOutputStreams();
        }

        // map of reference name -> multiresolution *index* filename
        Map<String,String> refnameToIndexFileNameMap = ContinuousFormatterHelper.makeMultiResolutionContinuousFiles(referenceName2FilenameMap);

        List<String> refnames = MiscUtils.set2List(this.referenceName2FilenameMap.keySet());
        this.writeContinuousOutputFile(refnames, refnameToIndexFileNameMap, this.referenceName2FilenameMap);
    }

    private void fillWithZeros(int curent, int dest,DataOutputStream out) throws IOException{
    	for (int i = curent; i < dest;i++){
    		out.writeFloat(0.0f);
    	}
    }


}
