/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin;

/**
 *
 * @author mfiume
 */
public class ProgramInformation {

    String name;
    String category;
    String description;
    String version;
    String author;
    String link;

    public ProgramInformation(String name, String category, String description, String version, String author, String link) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.version = version;
        this.author = author;
        this.link = link;
    }

    public String getName() { return this.name; }
    public String getCategory() { return this.category; }
    public String getDescription() { return this.description; }
    public String getVersion() { return this.version; }
    public String getAuthor() { return this.author; }
    public String getLink() { return this.link; }

    @Override
    public String toString() {
        return
                getName() + " "
                + getCategory() + " "
                + getDescription() + " "
                + getVersion() + " "
                + getAuthor() + " "
                + getLink();
    }

}

