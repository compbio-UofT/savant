/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.data.types;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author zig
 */
public class BlockTest {

    public BlockTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of valueOf method, of class Block.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        long pos = 0L;
        long size = 0L;
        Block expResult = null;
        Block result = Block.valueOf(pos, size);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPosition method, of class Block.
     */
    @Test
    public void testGetPosition() {
        System.out.println("getPosition");
        Block instance = null;
        long expResult = 0L;
        long result = instance.getPosition();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSize method, of class Block.
     */
    @Test
    public void testGetSize() {
        System.out.println("getSize");
        Block instance = null;
        long expResult = 0L;
        long result = instance.getSize();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equals method, of class Block.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object o = null;
        Block instance = null;
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class Block.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        Block instance = null;
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class Block.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        Block instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}