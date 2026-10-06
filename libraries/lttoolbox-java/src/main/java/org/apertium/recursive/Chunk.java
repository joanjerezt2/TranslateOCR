package org.apertium.recursive;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Chunk {

    public void writeTree(TreeMode treeMode, Object o) {

    }

    public enum ClipType{
        SourceClip,
        TargetClip,
        ReferenceClip
    }
    public enum TreeMode{
        TreeModeFlat,
        TreeModeNest,
        TreeModeLatex,
        TreeModeBox;

    }

    public String source;
    public String target;
    public String coref;
    public String wblank;
    boolean isBlank;
    boolean isJoiner;
    public List<Chunk> contents;
    public int rule;

    public void output(Writer out) {
        List<String> tags = new ArrayList<>();
        output(tags, out);
    }

    public void output(List<String> parentTags, Writer out){
        if(!contents.isEmpty()){
            List<String> tags = getTags(parentTags);
            for (Chunk content : contents) {
                content.output(tags, out);
            }
        }
    }

    /* OK */
    private List<String> getTags(List<String> parentTags) {
        int last;
        Deque<String> ret = new ArrayDeque<>();
        for(int i=0, limit=target.length(); i<limit;i++){
            if(target.charAt(i) == '<'){
                last = i;
                boolean isNum = true;
                for(int j = i+1; j < limit; j++){
                    if(target.charAt(j) == '>'){
                        if(isNum) {
                            int n = Integer.parseInt(target.substring(last + 1, j - last - 1));
                            if (n != 0 && n <= parentTags.size()) {
                                ret.addLast(parentTags.get(n - 1));
                                last = j + 1;
                                break;
                            }
                        }
                        String tag = target.substring(last, j-last+1);
                        ret.addLast(tag);
                        last = j+1;
                        break;
                    }
                    if(!Character.isDigit(target.charAt(j))){
                        isNum = false;
                    }
                }
            }
            else if(target.charAt(i) == '\\'){
                i++;
            }
        }
        return new ArrayList<>(ret);
    }
}
