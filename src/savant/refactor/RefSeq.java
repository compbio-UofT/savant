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

/*
 * RefSeq.java
 * Created on Jun 15, 2010
 */

package savant.refactor;

/**
 * Value class to represent a reference sequence by name and/or length
 *
 * @author vwilliams
 */
public class RefSeq {

    private String name;
    private Long length;

    public RefSeq(String name, Long length) {
        this.name = name;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefSeq refSeq = (RefSeq) o;

        if (length != null ? !length.equals(refSeq.length) : refSeq.length != null) return false;
        if (!compare(name, refSeq.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (length != null ? length.hashCode() : 0);
        return result;
    }

    private boolean compare(String src, String compare) {
        String srcWOPrefix = src.toLowerCase().replaceFirst("chr","");
        String compareWOPrefix = compare.toLowerCase().replaceFirst("chr","");
        return srcWOPrefix.equals(compareWOPrefix);
    }
}
