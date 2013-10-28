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

/**
 * Immutable class to represent a block, e.g. an exome
 * @author mfiume
 */
public final class Block {

    private final int position;
    private final int size;

    private Block(int pos, int size) {
        this.position = pos;
        this.size = size;
    }

    public static Block valueOf(int pos, int size) {
        return new Block(pos, size);
    }

    public int getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    /**
     * End-position of the block.
     * @since 1.6.0
     */
    public int getEnd() {
        return position + size;
    }

    /**
     * UCSC stores exon starts and ends as a comma-separated list of numbers packed into a blob.
     *
     * @param s the field to be unpacked
     * @return an array of integers extracted from the string
     */
    public static int[] extractBlocks(String s) {
        String[] blocks = s.split(",");
        int numBlocks = blocks.length;
        if (blocks[numBlocks - 1].length() == 0) {
            // Last block was a vestigial one created by a trailing comma.
            numBlocks--;
        }
        int[] result = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            result[i] = Integer.parseInt(blocks[i]);
        }
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (position != block.position) return false;
        if (size != block.size) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)position;
        result = 31 * result + (int)size;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Block");
        sb.append("{position=").append(position);
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
