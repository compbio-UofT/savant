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
 * RangeTest.java
 * Created on Jan 11, 2010
 */

package savant.util;

//import savant.data.types.Range;
import junit.framework.TestCase;

/**
 * TODO:
 * @author vwilliams
 */
public class RangeTest extends TestCase {

    private Range a, b, c, d;

    public void setUp() {
        a = new Range(4,57);
        b = new Range(4,57);
        c = new Range(4,57);
        d = new Range(1,82);
    }

    public void testConstruct() {
        try {
            // these are invalid ranges and should fail
            Range e = new Range(-1, 9);
            Range f = new Range(10, 1);
            fail("Expected IllegalArgumentException");
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

    public void testLength() {
        try {
            assertEquals(a.getLength(), 54);
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
