/*
 *    Copyright 2010-2011 University of Toronto
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
