/*
 *    Copyright 2012 University of Toronto
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
