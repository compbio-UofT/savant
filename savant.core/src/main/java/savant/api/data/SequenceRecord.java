/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.api.data;

import java.io.UnsupportedEncodingException;

/**
 * Immutable class to represent a sequence. Includes the sequence name.
 *
 * @author vwilliams
 */
public class SequenceRecord implements Record {

    private final String reference;
    private final byte[] sequence;

    protected SequenceRecord(String reference, byte[] sequence) {
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
