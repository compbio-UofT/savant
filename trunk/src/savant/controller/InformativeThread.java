/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import savant.view.tools.ToolRunInformation;

/**
 *
 * @author Marc Fiume
 */
public class InformativeThread extends Thread {

    private ToolRunInformation info;

    public InformativeThread(Runnable r, ToolRunInformation info) {
        super(r);
        this.info = info;
    }

    public ToolRunInformation getRunInformation() {
        return this.info;
    }
}
