/*
 * SequenceRecord.java
 * Created on Aug 23, 2010
 *
 *
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

package savant.data.types;

import java.io.UnsupportedEncodingException;

/**
 * Immutable class to represent a sequence. Includes the sequence name.
 *
 * @author vwilliams
 */
public class SequenceRecord implements Record {

    private final String reference;
    private final byte[] sequence;

    SequenceRecord(String reference, byte[] sequence) {
        if (reference == null) throw new IllegalArgumentException("Invalid argument; reference may not be null.");
        if (sequence == null) throw new IllegalArgumentException("Invalud argument; sequence may not be null.");

        this.reference = reference;
        this.sequence = sequence;
    }

    public static SequenceRecord valueOf(String reference, byte[] sequence) {
        return new SequenceRecord(reference, sequence);
    }

    public byte[] getSequence() {
        return sequence;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public int compareTo(Object o) {
        if (this == o) {
            return 0;
        }

        SequenceRecord that = (SequenceRecord)o;
        int result = reference.compareTo(that.reference);
        if (result == 0) {
            int thisLen = sequence.length;
            int thatLen = that.sequence.length;

            for (int i = 0; ; i++) {
                int a = 0, b = 0;

                if (i < thisLen) {
                    a = ((int)sequence[i]) & 0xff;
                } else if (i >= thatLen) {
                    return 0;
                }

                if (i < thisLen) {
                    b = ((int) that.sequence[i]) & 0xff;
                }

                if (a > b) {
                    return 1;
                }

                if (b > a) {
                    return -1;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SequenceRecord that = (SequenceRecord) o;
        return compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        int result = reference.hashCode();
        result = 31 * result + sequence.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SequenceRecord");
        sb.append("{reference='").append(reference).append("\', sequence='");
        try {
            sb.append(new String(sequence, "ISO-8859-1"));
        } catch (UnsupportedEncodingException ignored) {
        }
        sb.append("\'}");
        return sb.toString();
    }
}
