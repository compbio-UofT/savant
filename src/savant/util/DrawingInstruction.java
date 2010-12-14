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
 * Enum to represent drawing instructions to be set on a renderer.
 *
 * @author mfiume, tarkvara
 */
public enum DrawingInstruction {

    TRACK_DATA_TYPE,
    MODE,
    AXIS_RANGE,
    RESOLUTION,
    RANGE,
    COLOR_SCHEME,
    GENOME,
    ARC_MIN,
    DISCORDANT_MIN,
    DISCORDANT_MAX,
    MESSAGE,
    REFERENCE_EXISTS,
    SELECTION_ALLOWED,
    UNSUPPORTED_RESOLUTION
}

