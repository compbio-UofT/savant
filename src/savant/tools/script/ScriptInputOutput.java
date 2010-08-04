/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.script;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class ScriptInputOutput {

    public enum Direction { INPUT, OUTPUT };
    public enum Type { FILE, MULTIFILE, STDIN, STDOUT, NONE };
    public enum Source { FIXED, SPECIFIED };

    private Direction direction;
    private Type type;
    private Source source;
    private List<String> sources = new ArrayList<String>();

    public ScriptInputOutput(Direction d) {
        setDirection(d);
    }

    public ScriptInputOutput(Direction d, Type t) {
        setDirection(d);
        this.type = t;
        this.source = Source.SPECIFIED;
    }

    public ScriptInputOutput(Direction d, Type t, String s) {
        List<String> srcs = new ArrayList<String>();
        srcs.add(s);
        this.type = t;
        this.source = Source.FIXED;
        this.sources = srcs;
    }

    public ScriptInputOutput(Direction d, Type t, List<String> sources) {
        this.type = t;
        this.source = Source.FIXED;
        this.sources = sources;
    }

    public Direction getDirection() { return this.direction; }
    public Type getType() { return type; }
    public Source getSource() { return source; }
    public List<String> getSources() { return sources; }

    public void setDirection(Direction d) { this.direction = d; }
    public void setType(Type t) { this.type = t; }
    public void setSource(Source s) { this.source = s; }
    public void setSources(List<String> sources) { this.sources = sources; }
}
