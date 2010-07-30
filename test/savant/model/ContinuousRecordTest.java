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
 * ContinuousRecordTest.java
 * Created on Jan 11, 2010
 */

package savant.model;

import junit.framework.TestCase;

/**
 * A test case to exercies the ContinuousRecord class.
 * @author vwilliams
 */
public class ContinuousRecordTest extends TestCase {

    private ContinuousRecord a, b, c, d;

    public void setUp() {


        a = new ContinuousRecord("chr1", 1, new Continuous(10.5f));
        b = new ContinuousRecord("chr1", 1, new Continuous(10.5f));
        c = new ContinuousRecord("chr1", 1, new Continuous(10.5f));
        d = new ContinuousRecord("chr1", 2, new Continuous(20.3f));

    }

    public void testConstruct() {
        try {
            // This continuous item is invalid and should fail
            ContinuousRecord e = new ContinuousRecord("chr1", 1, null);
            fail("Expected IllegalArgumentException.");
        } catch (Exception success) {}
    }
    public void testEquals() {

        try {
            // reflexivity: A = A
            assertTrue(a.equals(a));

            // symmetry: A = B & B = A
            assertTrue(a.equals(b));
            assertTrue(b.equals(a));

            // transitivity: A = B & B = C & A = C
            assertTrue(b.equals(c));
            assertTrue(a.equals(c));

            // inequality: A <> D
            assertFalse(a.equals(d));

            // A <> null
            assertFalse(a.equals(null));

            // A <> object of another type
            assertFalse(a.equals(new Object()));

        } catch (Exception e) {
            fail("Unexpected exception " + e.getMessage());
        }

    }

    public void testHashCode() {

        try {
            // objects which are equal have same hash
            assertEquals(a.hashCode(), b.hashCode());

            // unequal objects have unequal hashes
            assertFalse(a.hashCode() == d.hashCode());
        } catch (Exception e) {
            fail("Unexpected exception " + e.getMessage());
        }
    }

    public void testToString() {

        // make sure no null pointers are thrown
        try {
            a.toString();
        }
        catch(Exception e) {
            fail("Unexpected exception " + e.getMessage());
        }
    }
}
