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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.sam.reader;

import net.sf.samtools.*;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.util.CloseableIterator;
import org.apache.log4j.Logger;
import org.broad.igv.sam.Alignment;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jrobinso
 */
public class BAMQueryReader implements AlignmentQueryReader {

    private static Logger log = Logger.getLogger(BAMQueryReader.class);
    SAMFileReader reader;
    BAMIndex index;
    SAMFileHeader header;

    public BAMQueryReader(File samFile) throws IOException {
        reader = new SAMFileReader(samFile);
        reader.setValidationStringency(ValidationStringency.SILENT);
        loadHeader();
    }

    public SAMFileHeader getHeader() {
        if (header == null) {
            loadHeader();
        }
        return header;
    }

    public Set<String> getSequenceNames() {
        SAMFileHeader header = getHeader();
        if (header == null) {
            return null;
        }
        Set<String> seqNames = new HashSet();
        List<SAMSequenceRecord> records = header.getSequenceDictionary().getSequences();
        if (records.size() > 0) {
            for (SAMSequenceRecord rec : header.getSequenceDictionary().getSequences()) {
                String chr = rec.getSequenceName();
                seqNames.add(chr);
            }
        }
        return seqNames;
    }

    private void loadHeader() {
        header = reader.getFileHeader();
    }

    public void close() throws IOException {
        reader.close();
    }

    public boolean hasIndex() {
        return reader.hasIndex();
    }

    public CloseableIterator<Alignment> query(String sequence, int start, int end, boolean contained) throws ArrayIndexOutOfBoundsException {
        SAMRecordIterator query = null;
        query = reader.query(sequence, start, end, contained);
        return new WrappedIterator(query);
    }

    public CloseableIterator<Alignment> iterator() {
        return new WrappedIterator(reader.iterator());
    }
}
