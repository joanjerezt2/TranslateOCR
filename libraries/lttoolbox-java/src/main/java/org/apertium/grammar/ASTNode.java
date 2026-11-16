package org.apertium.grammar;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    public ASTType type;
    public long line = 0;
    public int b = 0; // Índice de inicio en el String global
    public int e = 0; // Índice de fin en el String global
    public long u = 0; // Entero sin signo equivalente a uint32_t
    public List<ASTNode> cs = new ArrayList<>();

    public ASTNode() {
        this.type = ASTType.AST_Unknown;
    }

    public ASTNode(ASTType type, long line, int b, int e) {
        this.type = type;
        this.line = line;
        this.b = b;
        this.e = e;
    }
}
