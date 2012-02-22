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
package savant.util;

import java.util.List;

import savant.api.data.Record;

/**
 * Interface shared by synthetic records which are created to represent an aggregate of actual records.
 * For instance, used to merge together VariantRecords from multiple sources.
 *
 * @author tarkvara
 */
public interface AggregateRecord<T extends Record> extends Record {
    List<T> getConstituents();
}
