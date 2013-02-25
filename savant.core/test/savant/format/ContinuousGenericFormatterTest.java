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
 * Test for formatting of Generic Continuous files.
 *
 * @author tarkvara
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ContinuousGenericFormatterTest {
    private static final File INPUT_FILE = new File("testdata/human.hg18.genes.gencont");
    private static final File GOOD_FILE = new File("testdata/human.hg18.genes.gencont.savant");
    private static final File OUTPUT_FILE = new File("testdata/test.gencont.savant");

    public ContinuousGenericFormatterTest() {
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
     * Test of format method, of class ContinuousGenericFormatter.
     */
    @Test
    public void testFormat() throws Exception {
        System.out.println("ContinuousGenericFormatter.format");
        ContinuousGenericFormatter instance = new ContinuousGenericFormatter(INPUT_FILE, OUTPUT_FILE);
        instance.format();
        assertBinaryEquals(OUTPUT_FILE, GOOD_FILE);
    }
}