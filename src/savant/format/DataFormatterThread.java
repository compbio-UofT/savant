package savant.format;

import savant.controller.event.thread.ThreadActivityChangedEvent;
import savant.view.dialog.FormatFrame;

public class DataFormatterThread implements Runnable {

    private DataFormatter df;
    private FormatFrame ff;
    private boolean didComplete = false;

    public DataFormatterThread(DataFormatter df) {
        this.df = df;
    }

    public void setFormatFrame(FormatFrame ff) {
        this.ff = ff;
    }

    /*
    public boolean didFormattingComplete() {
        return this.didComplete;
    }
     * 
     */

    @Override
    public void run() {

        ThreadActivityChangedEvent.Activity status;

        try {
            df.format();
            notifyFormatFrameOfTermination(true, null);
        } catch (Exception ex) {
            notifyFormatFrameOfTermination(false, ex);
        }
    }

    private void notifyFormatFrameOfTermination(boolean wasFormatSuccessful, Exception e) {
        if (ff != null) {
            ff.notifyOfTermination(wasFormatSuccessful, e);
        }
    }
}
