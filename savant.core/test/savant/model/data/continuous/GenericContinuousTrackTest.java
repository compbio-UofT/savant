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
 * GenericContinuousTrackTest.java
 * Created on Jan 12, 2010
 */

package savant.model.data.continuous;

import junit.framework.TestCase;

/**
 * TODO:
 * @author vwilliams
 */
public class GenericContinuousTrackTest extends TestCase {

    private static final String GENERIC_CONTINUOUS_BIN_FILE_NAME = "testdata/continuous/test.continuous";

    public void setUp() {
        
    }

    public void testGetRecords() {
//        try {
//            GenericContinuousDataSource track = new GenericContinuousDataSource(new File(GENERIC_CONTINUOUS_BIN_FILE_NAME));
//            List<ContinuousRecord> records = track.getRecords(new Range(0,99), Resolution.VERY_HIGH);
//            for (ContinuousRecord record : records) {
//                assertNotNull(record.getValue());
//                System.out.println(record.getValue());
//            }
//            assertEquals(records.size(),100);
//            track.close();
//        } catch (FileNotFoundException e) {
//            fail("Unexpected FileNotFoundException.");
//            e.printStackTrace();
//        }
    }
}
