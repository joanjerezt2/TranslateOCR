package org.apertium.anaphora;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseArx {

    public Map<String, Integer> get_all_markables_score() {
        return all_markables_score;
    }

    public Map<String, Integer> get_parameter_markables_score(String parameterName) {
        return parameter_markables_score.get(parameterName);
    }

    static void print_tags(List<String> input){
        for (String it : input) {
            System.err.print(it + " ");
        }
    }

    public Map<String, List<List<MarkablePattern>>> get_markables() {
        return markables;
    }

    public Map<String, List<Item>> get_cats() {
        return cats;
    }

    public static class Item { //for cat-item and parameter-item
        List<String> has_tags;
        List<String> exclude_tags = new ArrayList<>();
        String lemma;
    }

    public static class MarkablePattern
    {
        String name;
        int head;
    }

    private final Map<String, Map<String, List<Item>>> parameters = new HashMap<>(); //parameter type mapped to its parameter types, i.e. anaphor/antecedent mapped to a map which contains n="detpos" and n="verbal", etc.
    private final Map<String, List<Item>> cats = new HashMap<>(); //cat name mapped to acceptable tag lists

    private final Map<String, List<List<MarkablePattern>>> markables = new HashMap<>(); //markable name mapped to acceptable pattern lists. Also, each pattern has a head == 1
    Map<String, Integer> all_markables_score = new HashMap<>(); //markable name mapped to score of markable, will be applied on all anaphors
    Map<String, Map<String, Integer>> parameter_markables_score = new HashMap<>(); //parameter name mapped to a mapping of markable and score (when parameter name is explicitly mentioned in arx)

    public int parseDoc(String docname) {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document curDoc;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            curDoc = builder.parse(docname);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            System.err.print("Document not parsed successfully. \n");
            return -1;
        }

        Element cur = curDoc.getDocumentElement();

        if (cur == null)
        {
            System.err.print("Empty Document!\n");
            return 1;
        }

        if ((cur.getAttribute("name")).equals("ref"))
        {
            System.err.print("Document of the wrong type! Root node should be ref.\n");
            return 2;
        }

        for (int i=0; i<cur.getChildNodes().getLength(); i++){
            if(cur.getChildNodes().item(i).getNodeName().equals("section-parameters")){
                parseParameters(cur.getChildNodes().item(i));
            }
            else if(cur.getChildNodes().item(i).getNodeName().equals("section-def-cats")){
                parseCats(cur.getChildNodes().item(i));
            }
            else if(cur.getChildNodes().item(i).getNodeName().equals("section-markables")){
                parseMarkables(cur.getChildNodes().item(i));
            }
        }
        return 0;
    }

    private void parseMarkables(Node item) {

        for(int i=0; i<item.getChildNodes().getLength(); i++){
            if(item.getChildNodes().item(i).getNodeName().equals("markable")){
                parsePatterns(item.getChildNodes().item(i),
                        item.getChildNodes().item(i).getAttributes().getNamedItem("n").getTextContent());
            }
        }

    }

    private void parsePatterns(Node item, String markable_name) {

        for(int i=0; i<item.getChildNodes().getLength(); i++){
            if(item.getChildNodes().item(i).getNodeName().equals("pattern")){
                List<MarkablePattern> temp_pattern = parsePatternItem(item.getChildNodes().item(i));
                markables.get(markable_name).addLast(temp_pattern);
            }
            else if(item.getChildNodes().item(i).getNodeName().equals("score")){
                int score_int = Integer.parseInt(item.getChildNodes().item(i).getAttributes().getNamedItem("n").getTextContent());
                String param_name = item.getChildNodes().item(i).getAttributes().getNamedItem("parameter").getTextContent();

                if(param_name != null){
                    parameter_markables_score.get(param_name).put(markable_name, score_int);
                }
                else{
                    all_markables_score.put(markable_name,score_int);
                }
            }
        }
    }

    private List<MarkablePattern> parsePatternItem(Node item) {
        List<MarkablePattern> temp_pattern = new ArrayList<>();
        for(int i=0; i<item.getChildNodes().getLength(); i++){
            Node cur_node = item.getChildNodes().item(i);
            if(cur_node.getNodeName().equals("pattern-item")){
                MarkablePattern temp = new MarkablePattern();
                Node attr = cur_node.getAttributes().getNamedItem("n");
                temp.name = attr.getTextContent();
                attr = cur_node.getAttributes().getNamedItem("head");
                if(attr != null){
                    temp.head = 1;
                }
                else{
                    temp.head = 0;
                }
                temp_pattern.addLast(temp);
            }
        }
        return temp_pattern;
    }

    private void parseCats(Node item) {

        for(int i=0; i<item.getChildNodes().getLength(); i++){
            Node cur_node = item.getChildNodes().item(i);
            if(cur_node.getNodeName().equals("def-cat")){
                parseCatItem(cur_node, cur_node.getAttributes().getNamedItem("n").getTextContent());
            }
        }
    }

    private void parseCatItem(Node cur, String cat_name) {

        for(int i=0; i<cur.getChildNodes().getLength(); i++){
            Node cur_node = cur.getChildNodes().item(i);
            if(cur_node.getNodeName().equals("cat-item")){
                Item temp_item = new Item();
                temp_item.has_tags = parseTags(cur_node.getAttributes().getNamedItem("has-tags"));
                temp_item.exclude_tags = parseTags(cur_node.getAttributes().getNamedItem("exclude-tags"));
                temp_item.lemma = cur_node.getAttributes().getNamedItem("lemma").getTextContent();
                cats.get(cat_name).addLast(temp_item);
            }
        }
    }

    private List<String> parseTags(Node tags) {
        List<String> temp_tags_list = new ArrayList<>();
        StringBuilder temptag = new StringBuilder();
        String tags_txt = tags.getTextContent();

        for (int i=0; i < tags_txt.length(); ++i)
        {
            if(tags_txt.charAt(i) == '\\')
            {
                temptag.append(tags_txt.charAt(i));
                ++i;
                temptag.append(tags_txt.charAt(i));
            }
		else if(tags_txt.charAt(i) == ' ') //space separated tags
            {
                temp_tags_list.addLast(String.valueOf(temptag));
                temptag = new StringBuilder();
            }
		else
            {
                temptag.append(tags_txt.charAt(i));
            }
        }

        if(!temptag.isEmpty())
            temp_tags_list.addLast(String.valueOf(temptag));

        return temp_tags_list;
    }

    private void parseParameters(Node cur) {

        for(int i=0; i<cur.getChildNodes().getLength(); i++){
            Node cur_node = cur.getChildNodes().item(i);
            if(cur_node.getNodeName().equals("def-parameter")){
                parseParameterTypes(cur, cur.getAttributes().getNamedItem("n"));
            } else if(cur_node.getNodeName().equals("delimiter")){
                parseParameterItem(cur_node, "delimiter", "default");
            }
        }
    }

    private void parseParameterTypes(Node cur, Node n) {
        for(int i=0; i<cur.getChildNodes().getLength(); i++){
            Node cur_node = cur.getChildNodes().item(i);
            parseParameterItem(cur_node, cur_node.getNodeName(), n.getTextContent());
        }
    }

    public void parseParameterItem (Node cur, String parameter_name, String parameter_type){
        //parameter_name: detpos, verbal, etc., parameter_type: anaphor, antecedent, etc.
        for(int i=0; i<cur.getChildNodes().getLength(); i++){
            Node cur_node = cur.getChildNodes().item(i);
            if(cur_node.getNodeName().equals("parameter-item")){
                Item temp_item = new Item();
                temp_item.has_tags = parseTags(cur_node.getAttributes().getNamedItem("has-tags"));
                temp_item.exclude_tags = parseTags(cur_node.getAttributes().getNamedItem("exclude-tags"));
                temp_item.lemma = cur_node.getAttributes().getNamedItem("lemma").getTextContent();
                parameters.get(parameter_type).get(parameter_name).addLast(temp_item);
            }
        }
    }

    public Map<String, Map<String, List<Item>>> getParameters() {
        return parameters;
    }
}
