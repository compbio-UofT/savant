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
 * WriteContinuousHeader.java
 * Created on Mar 1, 2010
 */

package savant.tools;

/**
 * <p>
 * This main class does nothing but write the header needed for a Generic Continuous file to
 * the output file you specify. It's a small utility no one should ever need, but it filled
 * a need of the developers at one point.</p>
 *
 * <p>
 * Please use command-line data formatting tools or in-Savant formatting tools.
 * </p>
 */

import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WriteContinuousHeader {

    private String outFile;
    DataOutputStream out;
    private RandomAccessFile raf;
    private List<FieldType> fields;
    private List<Object> modifiers;

    public WriteContinuousHeader(String outFile) {

        this.outFile = outFile;

    }

    public void format() {
        initOutput();
        closeOutput();
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
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: log properly
        }
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Missing argument: output file required");
        }

        WriteContinuousHeader instance = new WriteContinuousHeader(args[0]);
        instance.format();
    }

}
