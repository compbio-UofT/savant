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

package savant.data.types;

/**
 * Immutable class to represent a block, e.g. an exome
 * FIXME: this class is redundant; use Interval with a different static factory instead.
 * @author mfiume
 */
public class Block {

    private final int position;
    private final int size;

    Block(int pos, int size) {
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
        int result = position;
        result = 31 * result + size;
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