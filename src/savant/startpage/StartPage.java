/*
 *    Copyright 2009-2010 University of Toronto
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

package savant.startpage;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import javax.swing.JEditorPane;
import javax.swing.event.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.view.swing.util.DialogUtils;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.view.swing.Savant;

/**
 *
 * @author AndrewBrook
 */
public class StartPage extends JEditorPane {
    private static final Log LOG = LogFactory.getLog(StartPage.class);

    public StartPage() throws IOException{

        //NOTE:
        //creating this start page fails frequently. something needs to be fixed


        URL resourceURL = getClass().getResource("startPage.html");
        this.setEditable(false);
        this.setPage(resourceURL);
        //this.setPage("http://www.google.ca");

        //TEST///////////////////////////////////
        String page = "";
        String insertPage = "";
        String recentPage = "";
        String recentSession = "";

        //insert page
        URL insertURL = new URL("http://compbio.cs.toronto.edu/savant/news/test.html");
        URLConnection conn = insertURL.openConnection();
        DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
        BufferedReader d = new BufferedReader(new InputStreamReader(in));
        while(d.ready()){ insertPage += d.readLine(); }

        //recent session
        recentSession += "<a href=\"F4:F:\\Documents and Settings\\AndrewBrook\\My Documents\\testdata\\Sequences (FASTA)\\Source\\chr1.fa.savant\">July 17, 2010</a><br>";

        //recent page
        recentPage += "<a href=\"F3:F:\\Documents and Settings\\AndrewBrook\\My Documents\\testdata\\Sequences (FASTA)\\Source\\chr1.fa.savant\">chr1.fa.savant</a><br>";


        //page
        URLConnection conn1 = resourceURL.openConnection();
        in = new DataInputStream ( conn1.getInputStream (  )  ) ;
        d = new BufferedReader(new InputStreamReader(in));
        while(d.ready()){
            String current = d.readLine();
            if(current.equals("<!--INSERT NEWS-->")){
                page += insertPage;
            } else if (current.equals("<!--INSERT FILES-->")){
                page += recentPage;
            } else if (current.equals("<!--INSERT SESSIONS-->")){
                page += recentSession;
            } else {
                page += current;
            }           
        }

        this.setText(page);
        //END TEST///////////////////////////////
        

        this.addHyperlinkListener(new HyperlinkListener(){
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e){
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
                    try {
                        link(e);
                    } catch (Exception ex) {
                        LOG.error(String.format("Unable to update hyperlink %s.", e.getURL()), ex);
                    }
                }
            }
        });
        this.setSize(500, 500);
    }

    private void link(HyperlinkEvent e) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        String urlString = e.getDescription();
        String function = urlString.substring(0,2);
        
        if (function.equals("F1")){
            //close
            this.setVisible(false);
        } else if (function.equals("F2")) {
            //external link
            openURL(urlString.substring(3));
        } else if (function.equals("F3")){
            //load file
            Savant.getInstance().addTrackFromFile(urlString.substring(3));
        } else if (function.equals("F5")){
            //open track dialog
            //Savant.getInstance().showOpenTracksDialog();
        } else {
            //internal link
            try{setPage(e.getURL());}
            catch (IOException E){}
        }
    }


    static final String[] browsers = { "google-chrome", "firefox", "opera",
    "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };
    static final String errMsg = "Error attempting to launch web browser";

    /**
    * Opens the specified web page in the user's default browser
    * @param url A web address (URL) of a web page (ex: "http://www.google.com/")
    */
    public static void openURL(String url) {
        try {  //attempt to use Desktop library from JDK 1.6+
            Class<?> d = Class.forName("java.awt.Desktop");
            d.getDeclaredMethod("browse", new Class[] {java.net.URI.class}).invoke(
            d.getDeclaredMethod("getDesktop").invoke(null),
            new Object[] {java.net.URI.create(url)});
            //above code mimicks:  java.awt.Desktop.getDesktop().browse()
        }
        catch (Exception ignore) {  //library not available or failed
            String osName = System.getProperty("os.name");
            try {
                if (osName.startsWith("Mac OS")) {
                    Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
                    "openURL", new Class[] {String.class}).invoke(null,
                    new Object[] {url});
                }
                else if (osName.startsWith("Windows"))
                    Runtime.getRuntime().exec(
                    "rundll32 url.dll,FileProtocolHandler " + url);
                else { //assume Unix or Linux
                    String browser = null;
                    for (String b : browsers)
                        if (browser == null && Runtime.getRuntime().exec(new String[]
                        {"which", b}).getInputStream().read() != -1)
                            Runtime.getRuntime().exec(new String[] {browser = b, url});
                    if (browser == null)
                        throw new Exception(Arrays.toString(browsers));
                }
            } catch (Exception e) {
                DialogUtils.displayException("Unable to Open URL", errMsg, e);
            }
        }
    }
}
