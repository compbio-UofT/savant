/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.exception;


public class SavantTrackCreationCancelledException extends Exception {

    public SavantTrackCreationCancelledException() {
        super();
    }

    public SavantTrackCreationCancelledException(String msg) {
        super(msg);
    }

    public SavantTrackCreationCancelledException(String msg, Throwable t) {
        super(msg, t);
    }

    public SavantTrackCreationCancelledException(Throwable t) {
        super(t);
    }
}