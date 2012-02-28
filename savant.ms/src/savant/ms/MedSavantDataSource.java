/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.ms;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import savant.api.event.LocationChangedEvent;
import savant.api.util.Resolution;
import savant.controller.LocationController;
import savant.controller.FrameController;
import savant.view.tracks.Track;
import savant.controller.TrackController;
import savant.util.MiscUtils;
import savant.view.swing.Frame;

/**
 *
 * @author Andrew
 */
public class MedSavantDataSource implements DataSourceAdapter<VariantRecord>, VariantDataSourceAdapter, FiltersChangedListener, ReferenceListener, LoginListener {

    private boolean active = false;
    private Set<String> chromosomes = new HashSet<String>();
    private String[] participants = new String[0];
    
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

        Track t = TrackController.getInstance().getTrack(getName());
        if(t != null){
            t.getFrame().forceRedraw();
        }
        
        /*LocationController.getInstance().fireEvent(new LocationChangedEvent(
                false, 
                LocationController.getInstance().getReferenceName(), 
                LocationController.getInstance().getRange())); */
        //TODO is there a way to do this that doesn't refresh all tracks?
    }
    
    @Override
    public Set<String> getReferenceNames() {
        return chromosomes;
    }

    @Override
    public List<VariantRecord> getRecords(String ref, RangeAdapter range, Resolution resolution, RecordFilterAdapter<VariantRecord> filter) throws IOException, InterruptedException {
        if(active){
            
            System.out.println("Starting getRecords()");
            long start = System.nanoTime();
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

                List<Object[]> filteredVariants = MedSavantClient.VariantQueryUtilAdapter.getVariants(
                            LoginController.sessionId,
                            ProjectController.getInstance().getCurrentProjectId(),
                            ReferenceController.getInstance().getCurrentReferenceId(),
                            conditions,
                            0,
                            10000); //TODO enforce a limit...

                List<VariantRecord> records = new ArrayList<VariantRecord>();
                for (Object[] arr : filteredVariants) {
                    String dnaId = (String)arr[DefaultVariantTableSchema.INDEX_OF_DNA_ID];
                    records.add(new MedSavantVariantRecord(arr, getIndexOfParticipant(dnaId))); 
                }

                //for(MedSavantVariantRecord r : records){
                //    System.out.println(r);
                //}
                
                System.out.println("done. " + (System.nanoTime() - start)/1000000000.0);
                
                return records;

            } catch (Exception ex) {
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

    
}
