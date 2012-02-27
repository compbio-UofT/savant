/*
 *    Copyright 2010-2011 University of Toronto
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

import com.healthmarketscience.sqlbuilder.Condition;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.ut.biolab.medsavant.MedSavantClient;
import org.ut.biolab.medsavant.controller.FilterController;
import org.ut.biolab.medsavant.controller.LoginController;
import org.ut.biolab.medsavant.controller.ProjectController;
import org.ut.biolab.medsavant.controller.ReferenceController;
import org.ut.biolab.medsavant.controller.ResultController;
import org.ut.biolab.medsavant.db.exception.FatalDatabaseException;
import org.ut.biolab.medsavant.db.exception.NonFatalDatabaseException;
import org.ut.biolab.medsavant.model.event.FiltersChangedListener;


/**
 * Plugin class which exposes data retrieved from an SQL database.
 *
 * @author tarkvara
 */
public class SQLDataSourcePlugin extends savant.plugin.SavantPanelPlugin implements FiltersChangedListener {

    @Override
    public void init(JPanel p) {
        System.out.println("Hugo here");
        MedSavantClient.main(null);


        FilterController.addFilterListener(this);
    }

    @Override
    public String getTitle() {
        return "MedSavant";
    }

    @Override
    public void filtersChanged() throws SQLException, FatalDatabaseException, NonFatalDatabaseException {
        try {
            //VariantRecord c = ResultController.getInstance().getFilteredVariantRecords(start, limit, order);
            Condition[][] conditions = FilterController.getQueryFilterConditions();

            List<Object[]> filteredVariants = MedSavantClient.VariantQueryUtilAdapter.getVariants(
                        LoginController.sessionId,
                        ProjectController.getInstance().getCurrentProjectId(),
                        ReferenceController.getInstance().getCurrentReferenceId(),
                        conditions,
                        0,
                        500);

            for (Object[] arr : filteredVariants) {
                for (Object a : arr) {
                    System.out.print(a + "\t");
                }
                System.out.println();
            }
        } catch (RemoteException ex) {
            Logger.getLogger(SQLDataSourcePlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
