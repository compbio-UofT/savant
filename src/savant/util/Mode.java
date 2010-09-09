/*
 *    Copyright 2009-2010 University of Toronto
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

package savant.util;

/**
 * Immutable class to hold information about drawing modes
 *
 * @author mfiume
 */
public class Mode {

    private final String name;
    private final String description;

    Mode(String name, String desc)
    {
        if (name == null) throw new IllegalArgumentException("Invalid argument; name must not be null.");
        if (desc == null) throw new IllegalArgumentException("Invalid argument; description must not be null.");

        this.name = name;
        this.description = desc;

    }

    public static Mode valueOf(String name, String desc) {
        return new Mode(name, desc);
    }

    public static Mode fromObject(Object o, String desc) {
        return new Mode(o.toString(), desc);
    }

    public String getDescription() { return this.description; }
    public String getName() { return this.name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mode mode = (Mode) o;

        if (!description.equals(mode.description)) return false;
        if (!name.equals(mode.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Mode");
        sb.append("{name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
