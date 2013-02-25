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

package savant.format;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static junitx.framework.FileAssert.*;

/**
 * Test for formatting of Generic Point files.
 *
 * @author tarkvara
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PointGenericFormatterTest {
    private static final File INPUT_FILE = new File("testdata/human.hg18.genes.genpoint");
    private static final File GOOD_FILE = new File("testdata/human.hg18.genes.genpoint.savant");
    private static final File OUTPUT_FILE = new File("testdata/test.genpoint.savant");

    public PointGenericFormatterTest() {
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
     * Test of format method, of class PointGenericFormatter.
     */
    @Test
    public void testFormat() throws Exception {
        System.out.println("PointGenericFormatter.format");
        PointGenericFormatter instance = new PointGenericFormatter(INPUT_FILE, OUTPUT_FILE, false);
        instance.format();
        assertBinaryEquals(OUTPUT_FILE, GOOD_FILE);
    }

}