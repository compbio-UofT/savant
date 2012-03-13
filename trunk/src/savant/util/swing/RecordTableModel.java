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
package savant.util.swing;

import javax.swing.table.AbstractTableModel;
import savant.api.data.Record;

/**
 * Simple TableModel class where every row represents a single data record.
 *
 * @author tarkvara
 */
public abstract class RecordTableModel<T extends Record> extends AbstractTableModel {
    public abstract T getRecord(int row);
}
