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
 * Enum which categorises the various types of structural variants.
 *
 * @author tarkvara
 */
public enum VariantType {
    NONE,
    SNP_A,
    SNP_C,
    SNP_G,
    SNP_T,
    DELETION,
    INSERTION,
    OTHER;

    public String getDescription() {
        switch (this) {
            case SNP_A:
            case SNP_C:
            case SNP_G:
            case SNP_T:
                return "SNP";
            case DELETION:
                return "Deletion";
            case INSERTION:
                return "Insertion";
            case OTHER:
                return "Other";
            default:
                return "";
        }
    }
    
    public static VariantType fromChar(char c) {
        switch (c) {
            case 'A':
            case 'a':
                return VariantType.SNP_A;
            case 'C':
            case 'c':
                return VariantType.SNP_C;
            case 'G':
            case 'g':
                return VariantType.SNP_G;
            case 'T':
            case 't':
                return VariantType.SNP_T;
            default:
                return VariantType.OTHER;
        }
    }
}
