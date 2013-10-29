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
package savant.medsavant;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.dbspec.Column;
import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ut.biolab.medsavant.MedSavantClient;
import org.ut.biolab.medsavant.controller.FilterController;
import org.ut.biolab.medsavant.controller.LoginController;
import org.ut.biolab.medsavant.controller.ProjectController;
import org.ut.biolab.medsavant.controller.ReferenceController;
import org.ut.biolab.medsavant.db.api.MedSavantDatabase.DefaultVariantTableSchema;
import org.ut.biolab.medsavant.db.exception.FatalDatabaseException;
import org.ut.biolab.medsavant.db.exception.NonFatalDatabaseException;
import org.ut.biolab.medsavant.db.model.RangeCondition;
import org.ut.biolab.medsavant.db.model.structure.TableSchema;
import org.ut.biolab.medsavant.listener.ReferenceListener;
import org.ut.biolab.medsavant.model.event.FiltersChangedListener;
import org.ut.biolab.medsavant.model.event.LoginEvent;
import org.ut.biolab.medsavant.model.event.LoginListener;
import org.ut.biolab.medsavant.view.MainFrame;
import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.adapter.VariantDataSourceAdapter;
import savant.api.data.DataFormat;
import savant.api.data.VariantRecord;
import savant.api.util.Resolution;
import savant.view.tracks.Track;
import savant.controller.TrackController;
import savant.exception.RenderingException;
import savant.util.DrawingInstruction;
import savant.util.MiscUtils;

/**
 *
 * @author Andrew
 */
public class MedSavantDataSource implements DataSourceAdapter<VariantRecord>, VariantDataSourceAdapter, FiltersChangedListener, ReferenceListener, LoginListener {

    private boolean active = false;
    private Set<String> chromosomes = new HashSet<String>();
    private String[] participants = new String[0];
    private static final int LIMIT = 100000;
    
    public static final int RECORD_INDEX_DNA_ID = 0;
    public static final int RECORD_INDEX_DBSNP_ID = 1;
    public static final int RECORD_INDEX_CHROM = 2;
    public static final int RECORD_INDEX_POSITION = 3;
    public static final int RECORD_INDEX_REF = 4;
    public static final int RECORD_INDEX_ALT = 5;
    public static final int RECORD_INDEX_GT = 6;
    public static final int RECORD_INDEX_VARIANT_TYPE = 7;
    
    public MedSavantDataSource(){
        MedSavantClient.main(null);
        LoginController.addLoginListener(this);
        FilterController.addFilterListener(this);
        ReferenceController.getInstance().addReferenceListener(this);
    }
    
    private void updateSource() throws SQLException, RemoteException{

        //update chroms
        List<String> chroms = MedSavantClient.VariantQueryUtilAdapter.getDistinctValuesForColumn(
                LoginController.sessionId, 
                ProjectController.getInstance().getCurrentVariantTableName(), 
                DefaultVariantTableSchema.COLUMNNAME_OF_CHROM);
        chromosomes.clear();
        for(String c : chroms){
            chromosomes.add(c);
        }
        
        //update participants
        List<String> dnaIds = MedSavantClient.VariantQueryUtilAdapter.getDistinctValuesForColumn(
                LoginController.sessionId, 
                ProjectController.getInstance().getCurrentVariantTableName(), 
                DefaultVariantTableSchema.COLUMNNAME_OF_DNA_ID);
        participants = new String[dnaIds.size()];
        for(int i = 0; i < dnaIds.size(); i++){
            participants[i] = dnaIds.get(i);
        }
        
    }
    
    private void refresh() {
        Track t = getTrack();
        if(t != null){
            t.getFrame().forceRedraw();
        }
    }
    
    @Override
    public Set<String> getReferenceNames() {
        return chromosomes;
    }

    @Override
    public List<VariantRecord> getRecords(String ref, RangeAdapter range, Resolution resolution, RecordFilterAdapter<VariantRecord> filter) throws IOException, InterruptedException {
        if(active){
            
            try {
                
                String savantChrom = MiscUtils.homogenizeSequence(ref);
                String chrom = savantChrom;
                for(String c : chromosomes){
                    if(MiscUtils.homogenizeSequence(c).equals(savantChrom)){
                        chrom = c;
                    }
                }
                
                Condition[][] filterConditions = FilterController.getQueryFilterConditions();
                TableSchema table = ProjectController.getInstance().getCurrentVariantTableSchema();
                Condition rangeCondition = ComboCondition.and(
                        new Condition[]{
                            BinaryCondition.equalTo(table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_CHROM), chrom),
                            new RangeCondition(table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_POSITION), range.getFrom(), range.getTo())});
                Condition[][] conditions;
                if(filterConditions.length == 0){
                    conditions = new Condition[][]{new Condition[]{rangeCondition}};
                } else {
                    conditions = new Condition[filterConditions.length][];
                    for(int i = 0; i < filterConditions.length; i++){
                        conditions[i] = new Condition[2];
                        conditions[i][0] = rangeCondition;
                        conditions[i][1] = ComboCondition.and(filterConditions[i]);
                    }
                }
                
                Column[] columns = new Column[8];
                columns[RECORD_INDEX_DNA_ID] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_DNA_ID);
                columns[RECORD_INDEX_DBSNP_ID] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_DBSNP_ID); 
                columns[RECORD_INDEX_CHROM] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_CHROM); 
                columns[RECORD_INDEX_POSITION] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_POSITION);
                columns[RECORD_INDEX_REF] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_REF);
                columns[RECORD_INDEX_ALT] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_ALT);
                columns[RECORD_INDEX_GT] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_GT);
                columns[RECORD_INDEX_VARIANT_TYPE] = table.getDBColumn(DefaultVariantTableSchema.COLUMNNAME_OF_VARIANT_TYPE); 

                List<Object[]> filteredVariants = MedSavantClient.VariantQueryUtilAdapter.getVariants(
                            LoginController.sessionId,
                            ProjectController.getInstance().getCurrentProjectId(),
                            ReferenceController.getInstance().getCurrentReferenceId(),
                            conditions,
                            0,
                            LIMIT,
                            null,
                            columns);

                Map<String, MergedMedSavantVariantRecord> recordMap = new HashMap<String, MergedMedSavantVariantRecord>();
                
                if(filteredVariants.size() == LIMIT){
                    Track t = getTrack();
                    if(t != null){
                        t.getRenderer().addInstruction(DrawingInstruction.ERROR, new RenderingException("Too many variants to display", RenderingException.INFO_PRIORITY));
                    }
                    return new ArrayList<VariantRecord>();
                }
                
                for (Object[] arr : filteredVariants) {
                    Integer position = (Integer)arr[RECORD_INDEX_POSITION];
                    String refString = (String)arr[RECORD_INDEX_REF];
                    String key = position.toString() + refString;
                    MergedMedSavantVariantRecord m = recordMap.get(key);
                    if(m == null){
                        m = new MergedMedSavantVariantRecord(arr, participants.length);
                        recordMap.put(key, m);
                    } 
                    m.addRecord(arr, getIndexOfParticipant((String)arr[RECORD_INDEX_DNA_ID]));

                }
                
                List<VariantRecord> records = new ArrayList<VariantRecord>();
                for(String key : recordMap.keySet()){
                    records.add(recordMap.get(key));
                }

                return records;

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IOException(ex.getMessage());
            }
        } else {
            return new ArrayList<VariantRecord>();
        }
    }

    @Override
    public URI getURI() {
        return URI.create("meds://placeholder");
    }

    @Override
    public String getName() {
        return "MedSavant";
    }

    @Override
    public void close() {
        MainFrame.getInstance().requestClose();
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.VARIANT;
    }

    @Override
    public String[] getColumnNames() {
        return new String[0]; //TODO
    }

    @Override
    public void loadDictionary() throws IOException {}

    @Override
    public List<BookmarkAdapter> lookup(String key) {
        return new ArrayList<BookmarkAdapter>(); //TODO?
    }

    @Override
    public void filtersChanged() throws SQLException, FatalDatabaseException, NonFatalDatabaseException {
        refresh();
    }

    @Override
    public void referenceAdded(String string) {}

    @Override
    public void referenceRemoved(String string) {}

    @Override
    public void referenceChanged(String string) {
        try {
            updateSource();
        } catch (SQLException ex) {
            Logger.getLogger(MedSavantDataSource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            Logger.getLogger(MedSavantDataSource.class.getName()).log(Level.SEVERE, null, ex);
        }
        refresh();
    }

    @Override
    public void loginEvent(LoginEvent le) {
        active = (le.getType() == LoginEvent.EventType.LOGGED_IN);
    }
    
    private int getIndexOfParticipant(String dnaId){
        for(int i = 0; i < participants.length; i++){
            if(participants[i].equals(dnaId)){
                return i;
            }
        }
        return -1;
    }

    @Override
    public String[] getParticipants() {
        return participants;
    }

    private Track getTrack(){
        return TrackController.getInstance().getTrack(getName());
    }
    
}
