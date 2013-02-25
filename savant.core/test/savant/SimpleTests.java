/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author zig
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({savant.data.types.BlockTest.class})
public class SimpleTests {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}