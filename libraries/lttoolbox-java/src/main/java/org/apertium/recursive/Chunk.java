package org.apertium.recursive;

import org.apertium.lttoolbox.Pair;
import org.apertium.transfer.ApertiumRE;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * @author Joan Jerez, ToBeIT
 * @serial GPL 3.0
 */

public class Chunk {

    public enum ClipType {
        SourceClip,
        TargetClip,
        ReferenceClip
    }

    public enum TreeMode {
        TreeModeFlat,
        TreeModeNest,
        TreeModeLatex,
        TreeModeBox,
        TreeModeDot
    }

    public String source;
    public String target;
    public String coref;
    public String wblank;
    boolean isBlank;
    boolean isJoiner;
    public List<Chunk> contents;
    public int rule;

    /* OK */
    public static String combineWblanks(String wblank_current, String wblank_to_add) {
        if (wblank_current.isEmpty() && wblank_to_add.isEmpty()) {
            return wblank_current;
        } else if (wblank_current.isEmpty()) {
            return wblank_to_add;
        } else if (wblank_to_add.isEmpty()) {
            return wblank_current;
        }

        StringBuilder new_out_wblank = new StringBuilder();
        StringBuilder new_out_wblankBuilder = new StringBuilder(new_out_wblank.toString());
        for (int i = 0; i < wblank_current.length(); i++) {
            if (wblank_current.charAt(i) == '\\') {
                new_out_wblankBuilder.append(wblank_current.charAt(i));
                i++;
                new_out_wblankBuilder.append(wblank_current.charAt(i));
            } else if (wblank_current.charAt(i) == ']') {
                if (wblank_current.charAt(i + 1) == ']') {
                    new_out_wblankBuilder.append(';');
                    break;
                }
            } else {
                new_out_wblankBuilder.append(wblank_current.charAt(i));
            }
        }

        new_out_wblank = new StringBuilder(new_out_wblankBuilder.toString());
        for (int i = 0; i < wblank_to_add.length(); i++) {
            if (wblank_to_add.charAt(i) == '\\') {
                new_out_wblank.append(wblank_to_add.charAt(i));
                i++;
                new_out_wblank.append(wblank_to_add.charAt(i));
            } else if (wblank_to_add.charAt(i) == '[') {
                if (wblank_to_add.charAt(i + 1) == '[') {
                    new_out_wblank.append(' ');
                    i++;
                }
            } else {
                new_out_wblank.append(wblank_to_add.charAt(i));
            }
        }

        return new_out_wblank.toString();
    }

    /* OK */
    public String chunkPart(ApertiumRE part, ClipType side) {
        String chunk = switch (side) {
            case SourceClip -> source;
            case TargetClip -> target;
            case ReferenceClip -> coref;
        };
        String result = part.match(chunk);
        if (result.isEmpty()) {
            return "";
        } else {
            return result;
        }
    }

    /* OK */
    public void setChunkPart(ApertiumRE part, String value) {
        String surf = target;
        if (!part.match(surf).isEmpty()) {
            part.replace(surf, value);
            target = surf;
        }
    }

    /* OK */
    private List<String> getTags(List<String> parentTags) {
        int last;
        List<String> ret = new ArrayList<>();
        for (int i = 0, limit = target.length(); i < limit; i++) {
            if (target.charAt(i) == '<') {
                last = i;
                boolean isNum = true;
                for (int j = i + 1; j < limit; j++) {
                    if (target.charAt(j) == '>') {
                        if (isNum) {
                            int n = Integer.parseInt(target.substring(last + 1, j - last - 1));
                            if (n != 0 && n <= parentTags.size()) {
                                ret.addLast(parentTags.get(n - 1));
                                last = j + 1;
                                break;
                            }
                        }
                        String tag = target.substring(last, j - last + 1);
                        ret.addLast(tag);
                        last = j + 1;
                        break;
                    }
                    if (!Character.isDigit(target.charAt(j))) {
                        isNum = false;
                    }
                }
            } else if (target.charAt(i) == '\\') {
                i++;
            }
        }
        return ret;
    }

    /* OK */
    public void updateTags(List<String> parentTags) {
        if (isBlank) return;
        int last = 0;
        StringBuilder result = new StringBuilder();
        // a rough estimate - works if most number tags are 1 digit and most new tags are 3 chars or fewer
        for (int i = 0, limit = target.length(); i < limit; i++) {
            if (target.charAt(i) == '<') {
                result.append(target, last, i - last);
                last = i;
                boolean isNum = true;
                for (int j = i + 1; j < limit; j++) {
                    if (target.charAt(j) == '>') {
                        if (isNum) {
                            int n = Integer.parseInt(target.substring(last + 1, j - last - 1));
                            if (n != 0 && n <= parentTags.size()) {
                                result.append(parentTags.get(n - 1));
                            }
                        } else {
                            result.append(target, last, j - last + 1);
                        }
                        last = j + 1;
                        break;
                    }

                    if (!Character.isDigit(target.charAt(j))) {
                        isNum = false;
                    }
                }
            } else if (target.charAt(i) == '\\') {
                i++;
            }
        }
        if (last != target.length() - 1) {
            result.append(target.substring(last));
        }
        target = result.toString();
    }

    /* OK */
    public void output(List<String> parentTags, Writer out) throws IOException {
        if (!contents.isEmpty()) {
            List<String> tags = getTags(parentTags);
            for (Chunk content : contents) {
                content.output(tags, out);
            }
        } else if (isBlank) {
            if (out == null) {
                System.out.print(target);
            } else {
                out.write(target);
            }
        } else {
            updateTags(parentTags);
            if (!target.isEmpty()) {
                if (out == null) {
                    System.out.print(wblank);
                    System.out.print("^");
                    System.out.print(target);
                    System.out.print("$");
                } else {
                    out.write(wblank);
                    out.write('^');
                    out.write(target);
                    out.write('$');
                }
            }
        }
    }

    public void output (Writer out) throws IOException {
        List<String> tags = new ArrayList<>();
        output(tags, out);
    }

    /* OK */
    public String matchSurface(){
        if(contents.isEmpty())
        {
            return source;
        }
        return target;
    }

    /* OK */
    public void appendChild(Chunk kid){
        contents.addLast(kid);
    }

    /* OK */
    public void conjoin(Chunk other){
        int lemq_loc;
        for(lemq_loc = 0; lemq_loc < target.length(); lemq_loc++)
        {
            if(target.charAt(lemq_loc) == '\\')
            {
                lemq_loc++;
            }
            else if(target.charAt(lemq_loc) == '#')
            {
                        break;
            }
        }
        target = new StringBuilder(target).insert(lemq_loc,"+" + other.target).toString();
        wblank = combineWblanks(other.wblank, wblank);
    }

    /* OK */
    public void writeTree(TreeMode treeMode, Writer out) throws IOException {
        switch (treeMode) {
            case TreeModeFlat:
                writeTreePlain(out, -1);
                break;
            case TreeModeNest:
                writeTreePlain(out, 0);
                break;
            case TreeModeLatex:
                if (isBlank) return;
                writeString("\\begin{forest}\n%where n children=0{tier=word}{}\n", out);
                writeString("% Uncomment the preceding line to make the LUs bottom-aligned.\n", out);
                writeTreeLatex(out);
                writeString("\n\\end{forest}\n", out);
                break;
            case TreeModeDot:
                if (isBlank) return;
                writeString("digraph {", out);
                writeTreeDot(out);
                writeString("}\n", out);
                break;
            case TreeModeBox: {
                if (isBlank) return;
                List<List<StringBuilder>> tree = writeTreeBox();
                if (tree.isEmpty()) return;
                int tr = 4, sl = 12, st = 11, tl = 12, tt = 11, rl = 0, rt = 0;
                for (List<StringBuilder> strings : tree) {
                    if (strings.getFirst().length() > tr) tr = strings.getFirst().length();
                    if (strings.get(1).length() > sl) sl = strings.get(1).length();
                    if (strings.get(2).length() > st) st = strings.get(2).length();
                    if (strings.get(3).length() > tl) tl = strings.get(3).length();
                    if (strings.get(4).length() > tt) tt = strings.get(4).length();
                    if (strings.get(5).length() > rl) rl = strings.get(5).length();
                    if (strings.get(6).length() > rt) rt = strings.get(6).length();
                }
                boolean doCoref = (rl > 0 || rt > 0);
                if (doCoref && rl < 17) rl = 17;
                if (doCoref && rt < 16) rt = 16;
                writeString("Tree" + " ".repeat(tr - 3), out);
                writeString("Source Lemma" + " ".repeat(sl - 11), out);
                writeString("Source Tags" + " ".repeat(st - 10), out);
                writeString("Target Lemma" + " ".repeat(tl - 11), out);
                writeString("Target Tags" + " ".repeat(tt - 10), out);
                if (doCoref) {
                    writeString("Coreference Lemma" + " ".repeat(rl - 16), out);
                    writeString("Coreference Tags", out);
                    if (rt > 16) writeString(" ".repeat(rt - 16), out);
                }
                writeString("\n", out);
                writeString("─".repeat(tr) + " ", out);
                writeString("─".repeat(sl) + " ", out);
                writeString("─".repeat(st) + " ", out);
                writeString("─".repeat(tl) + " ", out);
                writeString("─".repeat(tt) + " ", out);
                if (doCoref) writeString(" " + "─".repeat(rl), out);
                if (doCoref) writeString(" " + "─".repeat(rt), out);
                writeString("\n", out);
                for (List<StringBuilder> stringBuilders : tree) {
                    writeString(" ".repeat(tr - stringBuilders.getFirst().length()) + stringBuilders.getFirst() + " ", out);
                    writeString(stringBuilders.get(1) + " ".repeat(sl - stringBuilders.get(1).length() + 1), out);
                    writeString(stringBuilders.get(2) + " ".repeat(st - stringBuilders.get(2).length() + 1), out);
                    writeString(stringBuilders.get(3) + " ".repeat(tl - stringBuilders.get(3).length() + 1), out);
                    writeString(stringBuilders.get(4) + " ".repeat(tt - stringBuilders.get(4).length()), out);
                    if (doCoref) {
                        writeString(" " + stringBuilders.get(5) + " ".repeat(rl - stringBuilders.get(5).length()), out);
                        writeString(" " + stringBuilders.get(6), out);
                    }
                    writeString("\n", out);
                }
                writeString("\n", out);
            }
            break;
            default:
                System.err.print("That tree mode has not yet been implemented.");
        }
    }

    /* OK */
    private Pair<String, String> chopString(String source) {
        String lem = "";
        String tags = "";
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '<') {
                lem = source.substring(0, i);
                tags = source.substring(i + 1, source.length() - i - 2);
                break;
            }
        }
        if (lem.isEmpty() && tags.isEmpty() && !source.isEmpty()) {
            lem = source;
        }
        return new Pair<>(lem, tags.replace("><", "."));
    }

    private static void writeString(String s, Writer out) throws IOException {
        if (out == null) {
            System.err.print(s);
        } else {
            out.write(s); // El Writer ya debe estar configurado en UTF-8 al crearse
        }
    }

    /* OK */
    private void writeTreePlain(Writer out, int depth) throws IOException {
        if(depth >= 0 && isBlank) return;
        StringBuilder base = new StringBuilder();
        base.repeat("\t", Math.max(0, depth));
        if(!isBlank)
        {
            if(!wblank.isEmpty())
            {
                base.append(wblank);
            }
            base.append("^");
        }
        if(!source.isEmpty())
        {
            base.append(source).append("/");
        }
        base.append(target);
        if(!coref.isEmpty())
        {
            base.append("/").append(coref);
        }
        writeString(base.toString(), out);
        if(!contents.isEmpty())
        {
            writeString((depth == -1) ? "{" : "{\n", out);
            int newdepth = (depth == -1) ? -1 : depth + 1;
            for (Chunk content : contents) {
                content.writeTreePlain(out, newdepth);
            }
            for(int i  = 0; i < depth; i++)
            {
                writeString("\t", out);
            }
            writeString("}", out);
        }
        if(!isBlank) writeString("$", out);
        if(depth != -1) writeString("\n", out);
    }

    /* OK */
    private void writeTreeLatex(Writer out) throws IOException {
        if(isBlank) return;
        String nl = " \\\\ ";
        String base = "";
        Pair<String, String> p;
        if(!source.isEmpty())
        {
            p = chopString(source);
            base += "\\textbf{" + p.first + "}" + nl + "\\texttt{" + p.second + "}" + nl;
        }
        p = chopString(target);
        if(contents.isEmpty())
        {
            base += "\\textit{" + p.first + "}" + nl + "\\texttt{" + p.second + "}";
        }
        else
        {
            int i = 0;
            for(; i < p.second.length(); i++)
            {
                if(p.second.charAt(i) == '.') break;
            }
            if(i < p.second.length())
            {
                base += p.second.substring(0, i) + nl + "\\textit{" + p.first + "}";
                base += nl + "\\texttt{" + p.second.substring(i+1) + "}";
            }
            else
            {
                base += p.second + nl + "\\textit{" + p.first + "}";
            }
        }
        if(!coref.isEmpty())
        {
            p = chopString(coref);
            base += nl + "\\textit{" + p.first + "}" + nl + "\\texttt{" + p.second + "}";
        }
        base = "[{ \\begin{tabular}{c} " + base + " \\end{tabular} } ";
        base = base.replace("_", "\\_"); // C++ reference: base = StringUtils::substitute(base, L"_", L"\\_");
        writeString(base, out);
        for (Chunk content : contents) {content.writeTreeLatex(out);}
        writeString(" ]", out);
    }

    /* OK */
    private String writeTreeDot(Writer out) throws IOException {
        if(isBlank) return "";
        int nodeId = 0;
        nodeId++;
        String name = "n" + nodeId;
        String node = name + " \\[label=\"";
        if(!source.isEmpty())
        {
            node += source + "\\\\n";
        }
        node += target;
        if(!coref.isEmpty())
        {
            node += "\\\\n" + coref;
        }
        node += "\"\\];";
        writeString(node, out);
        for (Chunk content : contents) {
            String kid = content.writeTreeDot(out);
            if (!kid.isEmpty()) writeString(name + " -> " + kid + ";", out);
        }
        return name;
    }

    /* OK */
    private List<List<StringBuilder>> writeTreeBox() {
        if (contents.isEmpty()) {
            List<StringBuilder> ret = new ArrayList<>(7);
            Pair<String, String> p = chopString(source);
            ret.set(1, new StringBuilder(p.first));
            ret.set(2, new StringBuilder(p.second));
            p = chopString(target);
            ret.set(3, new StringBuilder(p.first));
            ret.set(4, new StringBuilder(p.second));
            p = chopString(coref);
            ret.set(5, new StringBuilder(p.first));
            ret.set(6, new StringBuilder(p.second));
            List<List<StringBuilder>> list = new ArrayList<>();
            list.add(ret);
            return list;
        } else {
            List<Pair<Integer, Integer>> bounds = new ArrayList<>();
            List<List<StringBuilder>> tree = new ArrayList<>();
            for (Chunk content : contents) {
                if (!content.isBlank) {
                    List<List<StringBuilder>> temp = content.writeTreeBox();
                    tree.addAll(temp);
                    if (temp.size() == 1) {
                        bounds.addLast(new Pair<>(tree.size() - 1, tree.size() - 1));
                        continue;
                    }
                    int first = -1, last = -1;
                    for (int j = tree.size() - temp.size(); j < tree.size(); j++) {
                        if (first == -1 && tree.get(j).getFirst().charAt(0) != ' ') first = j;
                        else if (first != -1 && last == -1 && tree.get(j).getFirst().charAt(0) == ' ') last = j - 1;
                    }
                    first = (first == -1) ? tree.size() - temp.size() : first;
                    last = (last == -1) ? tree.size() - 1 : last;
                    bounds.addLast(new Pair<>(first, last));
                }
            }
            if (tree.size() == 1) {
                tree.getFirst().set(0, new StringBuilder("─" + tree.getFirst().getFirst()));
                return tree;
            }
            int center = tree.size() / 2;
            int len = 0;
            for (List<StringBuilder> strings : tree) {
                if (strings.getFirst().length() > len) len = strings.getFirst().length();
            }
            TreeSet<Integer> lines = new TreeSet<>();
            for (Pair<Integer, Integer> bound : bounds) {
                if (bound.second < center) lines.add(bound.second);
                else if (bound.first > center) lines.add(bound.first);
                else lines.add(center);
            }
            int firstLine = lines.first();
            int lastLine = lines.last();
            for (int i = 0; i < tree.size(); i++) {
                int sz = tree.get(i).getFirst().length();
                if (!lines.contains(i)) {
                    String padding = " ".repeat(len - sz);
                    tree.get(i).set(0, new StringBuilder(padding + tree.get(i).getFirst()));
                } else {
                    if (sz > 0) {
                        char firstChar = tree.get(i).getFirst().charAt(0);
                        switch (firstChar) {
                            case '│':
                                tree.get(i).getFirst().setCharAt(0, '┤');
                                break;
                            case '├':
                                tree.get(i).getFirst().setCharAt(0, '┼');
                                break;
                            case '┌':
                                tree.get(i).getFirst().setCharAt(0, '┬');
                                break;
                            case '└':
                                tree.get(i).getFirst().setCharAt(0, '┴');
                                break;
                            default:
                                break;
                        }
                    }
                    tree.get(i).set(0, new StringBuilder("─".repeat(len - sz) + tree.get(i).getFirst()));
                }
                if (i < firstLine || i > lastLine) {
                    tree.get(i).set(0, new StringBuilder(" " + tree.get(i).getFirst()));
                } else if (i == firstLine && i == lastLine) {
                    tree.get(i).set(0, new StringBuilder("─" + tree.get(i).getFirst()));
                } else if (i == firstLine) tree.get(i).set(0, new StringBuilder("┌" + tree.get(i).getFirst()));
                else if (i < lastLine) {
                    if (!lines.contains(i)) {
                        tree.get(i).set(0, new StringBuilder("│" + tree.get(i).getFirst()));
                    } else {
                        tree.get(i).set(0, new StringBuilder("├" + tree.get(i).getFirst()));
                    }
                } else {
                    tree.get(i).set(0, new StringBuilder("└" + tree.get(i).getFirst()));
                }
            }
            return tree;
        }
    }
}
