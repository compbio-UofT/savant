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
 * CovMapToContinuous.java
 * Created on Mar 1, 2010
 */

package savant.tools;

import java.io.*;

/**
 * This class takes Mike's magic coverage map and produce a file of doubles
 * of only the values, not the positions and not the header. It has to fill
 * in for empty positions with the last value.
 */
public class CovMapToContinuous {

    private String inFile;
    private String outFile;

    public CovMapToContinuous(String inFile, String outFile )  {

        this.inFile = inFile;
        this.outFile = outFile;

    }
    public void format() {

        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(inFile)));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

            String lineIn;
            String[] tokens;
            int currentPosition=0;
            int position;
            double currentValue=0.0;

            // find the first position
            lineIn = reader.readLine();
            if (lineIn != null) {
                tokens = lineIn.split("\\s");
                position = Integer.parseInt(tokens[0]);
                currentValue = Double.parseDouble(tokens[1]);
                currentPosition = position;
            }

            // fill up the beginning of the file with 0's
            for (int i=1; i< currentPosition; i++) {
                /*
                // DEBUG
                System.out.println("Pos: " + i + " Val: 0.0");
                // END DEBUG
                */
                out.writeDouble(0.0);
            }

            // read and write the rest of the file
            while ((lineIn=reader.readLine()) != null) {
                tokens = lineIn.split("\\s");
                position = Integer.parseInt(tokens[0]);
                // fill in with previous values if necessary
                int skip = position - currentPosition;
                if (skip > 1) {
                    // need to fill in some values here
                    for (int i=1; i<skip; i++) {
                        /*
                        // DEBUG
                        int fillIndex = currentPosition + i;
                        System.out.println("Pos: " + fillIndex + " Val: " + currentValue);
                        // END DEBUG
                        */
                        out.writeDouble(currentValue);
                    }
                }
                // update position and value
                currentPosition = position;
                currentValue = Double.parseDouble(tokens[1]);
                // write this position's value
                /*
                // DEBUG
                System.out.println("Pos: " + currentPosition + " Val: " + currentValue);
                // END DEBUG
                */
                out.writeDouble(currentValue);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: log properly
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Missing argument: input file and output file required");
        }

        CovMapToContinuous instance = new CovMapToContinuous(args[0], args[1]);
        instance.format();
    }

}
