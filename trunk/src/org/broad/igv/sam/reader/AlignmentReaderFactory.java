/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */
package org.broad.igv.sam.reader;

import net.sf.samtools.SAMFileReader;
import org.apache.log4j.Logger;
import org.broad.igv.util.ResourceLocator;

import java.io.File;
import java.io.IOException;

/**
 * @author jrobinso
 *         TODO consider renaming this class AlignmentQueryReaderFactory now that two alignment formats are supported.
 */
public class AlignmentReaderFactory {
    private static Logger log = Logger.getLogger(AlignmentReaderFactory.class);

    static {
        SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    }

    public static AlignmentQueryReader getReader(String path, boolean requireIndex) throws IOException {
        return getReader(new ResourceLocator(path), requireIndex);
    }

    public static AlignmentQueryReader getReader(ResourceLocator locator) throws IOException {
        return getReader(locator, true);
    }

    public static AlignmentQueryReader getReader(ResourceLocator locator, boolean requireIndex) throws IOException {

        String pathLowerCase = locator.getPath().toLowerCase();

        AlignmentQueryReader reader = null;

        String samFile = locator.getPath();
        if (pathLowerCase.endsWith(".bam") && locator.isLocal()) {
            reader = new BAMQueryReader(new File(samFile));
        } else {
            throw new RuntimeException("Cannot find reader for aligment file: " + locator.getPath());
        }

        return reader;
    }
}

