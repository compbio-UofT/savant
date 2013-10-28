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
    COLOUR_SCHEME,      // ColorScheme
    DISCORDANT_MIN,     // int
    DISCORDANT_MAX,     // int
    REFERENCE_EXISTS,   // boolean
    SELECTION_ALLOWED,  // boolean
    ERROR,              // String (error message)
    PROGRESS,           // String (progress message)
    PAIRED_PROTOCOL,    // PairedProtocol enum, how paired sequencing is done
    ITEMRGB,            // itemRGB color for BED
    SCORE,              // boolean; use score tint for BED
    ALTERNATE_NAME,     // boolean; display alternate name instead of name
    BASE_QUALITY,       // boolean; alpha will be based on base quality
    MAPPING_QUALITY,    // boolean; alpha will be based on mapping quality
    PARTICIPANTS        // String[] containing names/IDs of all participants
}

