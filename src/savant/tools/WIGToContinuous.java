/*
 *    Copyright 2010 Vanessa Williams
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

package savant.tools;

import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WIGToContinuous {

    private String inFile;
    private String outFile;
    private DataOutputStream out;
    // stuff needed by IO; mandated by DataFormatUtils which we're depending on
    //private RandomAccessFile raf;

    private List<FieldType> fields;
    private List<Object> modifiers;
    
    public WIGToContinuous(String inFile, String outFile) {
        this.inFile = inFile;
        this.outFile = outFile;

        initOutput();
    }

    private void fillWithZeros(int curent, int dest,DataOutputStream out) throws IOException{
    	for (int i = curent; i < dest;i++){
//    		System.out.println(0.0);
    		out.writeDouble(0.0);
    	}

    }

    public void format() {
        //System.out.println("Start process: " + new Date().toString());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(inFile)));

            String lineIn;
            String[] tokens;

            // Skip the header if it exists.

            lineIn = reader.readLine();
            tokens = lineIn.split("\\s");
            if (tokens.length > 0 && tokens[0].equals("track")){
            	lineIn = reader.readLine();
            }


            //Read the rest of the file
            String mode = "none";
            int span = 1;
            int start = 1;
            int step = 1;
            int nextWrite = 1;

            while(lineIn != null){
            	tokens = lineIn.split("\\s");
//            	for (int i = 0; i < tokens.length; i ++){
//            		System.out.println(tokens[i]);
//            	}

            	if(tokens.length < 1){
            		//skip blank lines
            		continue;
            	}
            	if (tokens[0].equals("variableStep")){
            		if(tokens.length < 2){
            			System.out.println("Error parsing file (variableStep line)");
                		System.exit(1);
            		}
            		mode = "variable";
            		if(tokens.length == 3){
            			span = Integer.parseInt(tokens[2].substring(5));
            		} else {
            			span = 1;
            		}
            	} else if (tokens[0].equals("fixedStep")){
            		if(tokens.length < 4){
            			System.out.println("Error parsing file (fixedStep line)");
                		System.exit(1);
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
            			System.out.println("Error parsing file (to few tokens on varialbe line)");
                		System.exit(1);
            		}
            		int dest = Integer.parseInt(tokens[0]);
            		this.fillWithZeros(nextWrite,dest,out);
            		double val = Double.parseDouble(tokens[1]);
            		for (int i = 0; i < span; i++){
//            			System.out.println(val);
            			out.writeDouble(val);
            		}
            		nextWrite = dest + span;

            	} else if (mode.equals("fixed")){
            		this.fillWithZeros(nextWrite,start,out);
            		double val = Double.parseDouble(tokens[0]);
            		for (int i = 0; i < span; i++){
//            			System.out.println(val);
            			out.writeDouble(val);
            		}
            		nextWrite = start+span;
            		start += step;


            	} else if (mode.equals("none")){
            		System.out.println("Error parsing file (no format line)");
            		System.exit(1);
            	}

            	lineIn = reader.readLine();
            }
            closeOutput();


//            int currentPosition=0;
//            int position;
//
//            lineIn = reader.readLine();
////            // find the first position
//            if (lineIn != null) {
//                tokens = lineIn.split("\\s");
//                position = Integer.parseInt(tokens[0]);
//                currentPosition = position;
//
//                // DEBUG
//                System.out.println("First position is " + currentPosition);
//                // END DEBUG
//
//                // fill up the beginning with zeros
//                int i;
//                for (i=1; i<currentPosition; i++) {
//                    out.writeDouble(0.0);
//                }
//                System.out.println("Wrote " + i + " zeroes");
//
//                out.writeDouble(Double.parseDouble(tokens[1]));
//
//            }
////            // process the rest of the positions, filling in zeroes where there's no value
//            while ((lineIn=reader.readLine()) != null) {
//                tokens = lineIn.split("\\s");
//                position = Integer.parseInt(tokens[0]);
//                int skip = position - currentPosition;
//                if (skip > 1) {
////                     need to fill in some values here
//                    for (int i=1; i<skip; i++) {
//                        out.writeDouble(0.0);
//                    }
//                }
//                currentPosition = position;
//                /*
//                // DEBUG
//                double value = Double.parseDouble(tokens[1]);
//                if (value != 0) {
//                    System.out.println("Non-zero value: " + value);
//                }
//                // END DEBUG
//                */
//                out.writeDouble(Double.parseDouble(tokens[1]));
//          }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: log properly
        } catch (IOException io) {
            io.printStackTrace();
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
            fields.add(FieldType.DOUBLE);
            modifiers = new ArrayList<Object>();
            modifiers.add(null);
            out.writeInt(fields.size());
            for (FieldType ft : fields) {
                out.writeInt(ft.ordinal());
            }


        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }

    private void closeOutput() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Missing argument: input file and output file required");
            System.exit(1);
        }
        System.out.println("Start process: " + new Date().toString());
        WIGToContinuous instance = new WIGToContinuous(args[0], args[1]);
        instance.format();
        System.out.println("End process: " + new Date().toString());

    }
}
