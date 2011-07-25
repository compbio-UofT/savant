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

package savant.sql;

/**
 * Defines one of several ways of mapping between Savant fields and SQL database columns.
 * The MappingFormat determines which of the columns in the ColumnMapping are actually
 * relevant.
 *
 * @author tarkvara
 */
public enum MappingFormat {
    CONTINUOUS_VALUE_COLUMN,    // Data values stored in a database column.
    CONTINUOUS_WIG,             // Data values stored in an external Wig (or Wib) file.
    CONTINUOUS_BIGWIG,          // Data values stored in an external BigWig file.
    INTERVAL_GENERIC,
    INTERVAL_RICH
}
