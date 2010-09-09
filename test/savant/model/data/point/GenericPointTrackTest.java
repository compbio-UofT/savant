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
 * GenericPointTrackTest.java
 * Created on Jan 11, 2010
 */

package savant.model.data.point;

//import savant.data.types.GenericPointRecord;
import junit.framework.TestCase;

public class GenericPointTrackTest extends TestCase {

    private static final String POINT_SEQUENCE_BIN_FILE_NAME = "testdata/point/test.point";

    public void setUp() {

    }

    public void testGetRecords() {

//        GenericPointDataSource pointTrack = null;
//        try {
//            pointTrack = new GenericPointDataSource(new File(POINT_SEQUENCE_BIN_FILE_NAME));
//
//            List<GenericPointRecord> items = pointTrack.getRecords(new Range(0,217), Resolution.VERY_HIGH);
//            for (GenericPointRecord item : items) {
//                assertNotNull(item.getPoint());
//                assertNotNull(item.getDescription());
//                System.out.println(item.getPoint() + "\t" + item.getDescription());
//            }
//            assertTrue(items.size()==100);
//            pointTrack.close();
//        } catch (FileNotFoundException e) {
//            fail("Unexpected FileNotFoundException.");
//            e.printStackTrace();
//        }
    }
}
