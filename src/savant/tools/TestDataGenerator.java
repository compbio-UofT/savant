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
 * TestDataGenerator.java
 * Created on Jan 8, 2010
 */

package savant.tools;

import java.io.File;
import java.io.IOException;

/**
 * A tool to generate data needed by unit tests.
 * @author vwilliams
 */
public class TestDataGenerator {

    // TODO: move data generation to corresponding test classes; remove this class
    // FIXME: this class is completely broken and won't build. Just ignore until previous TODO is done
    private static final String GENERIC_POINT_TXT_FILE_NAME = "testdata/point/test.point.txt";
    private static final String GENERIC_POINT_BIN_FILE_NAME = "testdata/point/test.point";

    private static final String GENERIC_INTERVAL_TXT_FILE_NAME = "testdata/interval/test.interval.txt";
    private static final String GENERIC_INTERVAL_BIN_FILE_NAME = "testdata/interval/test.interval";

    private static final String GENERIC_CONTINUOUS_TXT_FILE_NAME = "testdata/continuous/test.continuous.txt";
    private static final String GENERIC_CONTINUOUS_BIN_FILE_NAME = "testdata/continuous/test.continuous";

    private static final String FASTA_TXT_FILE_NAME = "testdata/sequence/test.sequence.txt";
    private static final String FASTA_BIN_FILE_NAME = "testdata/sequence/test.sequence";


    private static TestDataGenerator instance;

    // characters used by randomChar
    private static final String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final char[] chars = alphaNumeric.toCharArray();
    private static final int numChars = alphaNumeric.length();

    // characters used by randomBase
    private static final String baseLetters = "ACGT";
    private static final char[] bases = baseLetters.toCharArray();
    private static final int numBases = baseLetters.length();

    public TestDataGenerator() throws IOException {
        generateGenericPointTrackTestData();
        generateGenericContinuousTrackData();
        generateSequenceTrackData();
        generateIntervalTrackData();
    }

    public static void main(String args[]) {
        try {
            instance = new TestDataGenerator();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private void generateGenericPointTrackTestData() throws IOException {

//        // create a sorted set of 100 positions
//        SortedSet<Integer> positionSet = new TreeSet<Integer>();
//        while (positionSet.size() < 100) {
//            positionSet.add(randomPoint());
//        }
//
//        File temporaryTextFile = new File(GENERIC_POINT_TXT_FILE_NAME);
//        BufferedWriter textFileWriter = new BufferedWriter(new FileWriter(temporaryTextFile));
//        for (Integer pos : positionSet) {
//            textFileWriter.append(pos.toString() + "\t" + randomDescription());
//            textFileWriter.newLine();
//        }
//        textFileWriter.close();
//        DataFormatter formatter = new DataFormatter(FileFormat.POINT, GENERIC_POINT_TXT_FILE_NAME, GENERIC_POINT_BIN_FILE_NAME);
//        //formatter.format();
//        temporaryTextFile.delete();
    }

    private void generateGenericContinuousTrackData() throws IOException {
//        File temporaryTextFile = new File(GENERIC_CONTINUOUS_TXT_FILE_NAME);
//        BufferedWriter textFileWriter = new BufferedWriter(new FileWriter(temporaryTextFile));
//        for (Double d = 0.0; d < 100.0; d++) {
//            textFileWriter.append(d.toString());
//            textFileWriter.newLine();
//        }
//        textFileWriter.close();
//        DataFormatter formatter = new DataFormatter(FileFormat.CONTINUOUS_GENERIC, GENERIC_CONTINUOUS_TXT_FILE_NAME, GENERIC_CONTINUOUS_BIN_FILE_NAME);
//        //formatter.format();
//        temporaryTextFile.delete();
    }

    private void generateSequenceTrackData() throws IOException {
//        File temporaryTextFile = new File(FASTA_TXT_FILE_NAME);
//        BufferedWriter textFileWriter = new BufferedWriter(new FileWriter(temporaryTextFile));
//        for (int i=0; i<200; i++) {
//            textFileWriter.append(randomBase());
//        }
//        textFileWriter.close();
//        DataFormatter formatter = new DataFormatter(FileFormat.BFASTA, FASTA_TXT_FILE_NAME, FASTA_BIN_FILE_NAME);
//        //formatter.format();
//        temporaryTextFile.delete();
    }

    private void generateIntervalTrackData() throws IOException {
        File textFile = new File("testdata/interval/test.interval.txt");
        //DataFormatter formatter = new DataFormatter(TrackDataType.INTERVAL, "testdata/interval/test.interval.txt", "testdata/interval/test.interval");
        //formatter.format();
    }

    private int randomPoint() {
        Long rand = Math.round(Math.random()*217);
        return rand.intValue();
    }

    private String randomDescription() {
        //return randomString(DataFormatter.DESCRIPTIONLEN);
        return null;
    }

    private char randomBase() {
        int index = (int) Math.round(Math.random()*(numBases-1));
        return bases[index];
    }
    /**
     * Generate a random alphanumeric string of at most maxChars.
     *
     * @param numChars - number of characters in the returned string
     * @return random string
     */
    private String randomString(int numChars) {

        StringBuilder result = new StringBuilder();
        for (int i=0; i<numChars; i++) {
            result.append(randomChar());
        }
        return result.toString();
    }
    /**
     * Return a random character from the set [A-Za-z0-9].
     *
     * @return an alphanumeric character
     */
    private Character randomChar() {
        return chars[random(numChars-1)];
    }

    private int random(int max) {
        Long result = Math.round(Math.random()*max);
        // just in case rounding causes boundary violation
        if (result < 0) return 0;
        if (result > max) return max;
        return result.intValue();
    }

}
