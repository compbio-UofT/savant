/*
 *    Copyright 2011 University of Toronto
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
package savant.api.data;

/**
 * Summarises the various data formats which are used internally by Savant.
 * Intended to be more abstract than the FileType enum.
 *
 * @author tarkvara
 */
public enum DataFormat {

    SEQUENCE,
    POINT,
    CONTINUOUS,
    GENERIC_INTERVAL,
    RICH_INTERVAL,
    ALIGNMENT,
    VARIANT;

    @Override
    public String toString() {
        switch (this) {
            case SEQUENCE:
                return "Sequence";
            case POINT:
                return "Point";
            case CONTINUOUS:
                return "Continuous";
            case GENERIC_INTERVAL:
                return "Generic Interval";
            case RICH_INTERVAL:
                return "Rich Interval";
            case ALIGNMENT:
                return "Alignment";
            case VARIANT:
                return "Variant";
        }
        return null;
    }
}