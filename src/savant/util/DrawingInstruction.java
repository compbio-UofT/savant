/*
 *    Copyright 2009-2011 University of Toronto
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

    MODE,               // Mode
    AXIS_RANGE,         // AxisRange
    RESOLUTION,         // Resolution
    RANGE,              // Range
    COLOR_SCHEME,       // ColorScheme
    GENOME,             // not used
    ARC_MIN,            // double
    DISCORDANT_MIN,     // int
    DISCORDANT_MAX,     // int
    REFERENCE_EXISTS,   // boolean
    SELECTION_ALLOWED,  // boolean
    ERROR,              // String (error message)
    PROGRESS,           // String (progress message)
    PAIRED_PROTOCOL,    // PairedProtocol enum, how paired sequencing is done
    ITEMRGB,            // itemRGB color for BED
    SCORE,              // score tint for BED
    ALTERNATE_NAME      // display alternate name instead of name
}

