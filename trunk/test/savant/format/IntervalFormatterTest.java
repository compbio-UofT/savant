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

import savant.file.FileType;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static junitx.framework.FileAssert.*;

/**
 * Test for format of various interval types.
 *
 * @author tarkvara
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class IntervalFormatterTest {
    private static final File BED_INPUT_FILE = new File("testdata/human.hg18.genes.bed");
    private static final File BED_GOOD_FILE = new File("testdata/human.hg18.genes.bed.savant");
    private static final File BED_OUTPUT_FILE = new File("testdata/test.bed.savant");
    private static final File GENINT_INPUT_FILE = new File("testdata/human.hg18.genes.genint");
    private static final File GENINT_GOOD_FILE = new File("testdata/human.hg18.genes.genint.savant");
    private static final File GENINT_OUTPUT_FILE = new File("testdata/test.genint.savant");
    private static final File GFF_INPUT_FILE = new File("testdata/NC_005956.gff");
    private static final File GFF_GOOD_FILE = new File("testdata/NC_005956.gff.savant");
    private static final File GFF_OUTPUT_FILE = new File("testdata/test.gff.savant");

    public IntervalFormatterTest() {
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
     * Test of formatAsIntervalBED method, of class IntervalFormatter.
     */
    @Test
    public void testFormatAsIntervalBED() throws Exception {
        System.out.println("IntervalFormatter.formatAsIntervalBED");
        IntervalFormatter instance = new IntervalFormatter(BED_INPUT_FILE, BED_OUTPUT_FILE, false, FileType.INTERVAL_BED, 0, 1, 2, "#");
        instance.formatAsIntervalBED();
        instance.format();
        assertBinaryEquals(BED_OUTPUT_FILE, BED_GOOD_FILE);
    }

    /**
     * Test of formatAsIntervalGeneric method, of class IntervalFormatter.
     */
    @Test
    public void testFormatAsIntervalGeneric() throws Exception {
        System.out.println("IntervalFormatter.formatAsIntervalGeneric");
        IntervalFormatter instance = new IntervalFormatter(GENINT_INPUT_FILE, GENINT_OUTPUT_FILE, true, FileType.INTERVAL_GENERIC, 0, 1, 2, "#");
        instance.formatAsIntervalGeneric();
        instance.format();
        assertBinaryEquals(GENINT_OUTPUT_FILE, GENINT_GOOD_FILE);
    }

    /**
     * Test of formatAsIntervalGFF method, of class IntervalFormatter.
     */
    @Test
    public void testFormatAsIntervalGFF() throws Exception {
        System.out.println("IntervalFormat.formatAsIntervalGFF");
        IntervalFormatter instance = new IntervalFormatter(GFF_INPUT_FILE, GFF_OUTPUT_FILE, true, FileType.INTERVAL_GFF, 0, 3, 4, "#");
        instance.formatAsIntervalGFF();
        instance.format();
        assertBinaryEquals(GFF_OUTPUT_FILE, GFF_GOOD_FILE);
    }
}
