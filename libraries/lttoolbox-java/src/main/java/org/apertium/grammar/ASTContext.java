package org.apertium.grammar;

import java.io.PrintWriter;
import java.util.EnumSet;

public class ASTContext {
    // Equivalentes a thread_local
    public static final ThreadLocal<Boolean> parseAst = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<ASTNode> ast = ThreadLocal.withInitial(ASTNode::new);
    public static final ThreadLocal<ASTNode> curAst = ThreadLocal.withInitial(ast::get);
    public static final ThreadLocal<ASTHelper> curAstHelp = ThreadLocal.withInitial(() -> null);

    // Tipos de nodo que imprimen texto en XML
    private static final EnumSet<ASTType> TEXT_NODES = EnumSet.of(
            ASTType.AST_AnchorName, ASTType.AST_ContextMod, ASTType.AST_ContextPos,
            ASTType.AST_IncludeFilename, ASTType.AST_MappingPrefix, ASTType.AST_Option,
            ASTType.AST_RuleAddcohortWhere, ASTType.AST_RuleDirection, ASTType.AST_RuleExternalCmd,
            ASTType.AST_RuleExternalType, ASTType.AST_RuleFlag, ASTType.AST_RuleMoveType,
            ASTType.AST_RuleName, ASTType.AST_RuleType, ASTType.AST_RuleWordform,
            ASTType.AST_SetName, ASTType.AST_SetOp, ASTType.AST_SubReadingsDirection,
            ASTType.AST_Tag, ASTType.AST_TemplateName, ASTType.AST_TemplateRef
    );

    // xml_encode mapeado eficientemente utilizando subcadenas e índices
    public static String xmlEncode(String fullText, int b, int e) {
        if (b >= e || b < 0 || e > fullText.length()) return "";

        StringBuilder buf = new StringBuilder(e - b);
        for (int i = b; i < e; i++) {
            char c = fullText.charAt(i);
            switch (c) {
                case '&'  -> buf.append("&amp;");
                case '"'  -> buf.append("&quot;");
                case '\'' -> buf.append("&apos;");
                case '<'  -> buf.append("&lt;");
                case '>'  -> buf.append("&gt;");
                default   -> buf.append(c);
            }
        }
        return buf.toString();
    }

    // Impresión recursiva del AST
    public static void printAst(PrintWriter out, String fullText, int baseOffset, int indentSize, ASTNode node) {
        String indent = " ".repeat(indentSize);

        // Conversión aritmética de punteros (node.b - b) -> resta de índices absolutos
        int relativeB = node.b - baseOffset;
        int relativeE = node.e - baseOffset;

        out.printf("%s<%s l=\"%d\" b=\"%d\" e=\"%d\"",
                indent, node.type.toString(), node.line, relativeB, relativeE);

        if (TEXT_NODES.contains(node.type)) {
            out.printf(" t=\"%s\"", xmlEncode(fullText, node.b, node.e));
        }
        if (node.u != 0) {
            out.printf(" u=\"%d\"", node.u);
        }
        if (node.cs.isEmpty()) {
            out.print("/>\n");
            return;
        }
        out.print(">\n");
        for (ASTNode child : node.cs) {
            if (child.type == ASTType.AST_Grammar) {
                printAst(out, fullText, child.b, indentSize + 1, child);
            } else {
                printAst(out, fullText, baseOffset, indentSize + 1, child);
            }
        }
        out.printf("%s</%s>\n", indent, node.type.toString());
    }
}

