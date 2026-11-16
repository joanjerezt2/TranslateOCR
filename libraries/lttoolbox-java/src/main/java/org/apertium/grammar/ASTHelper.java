package org.apertium.grammar;

public class ASTHelper implements AutoCloseable {
    private ASTNode c;
    private ASTHelper h;

    public ASTHelper(ASTType type, long line, int b, int e) {
        this.c = ASTContext.curAst.get();
        this.h = ASTContext.curAstHelp.get();

        ASTContext.curAstHelp.set(this);
        if (!ASTContext.parseAst.get()) {
            this.c = null;
            this.h = null;
            return;
        }

        ASTNode child = new ASTNode(type, line, b, e);
        this.c.cs.add(child);
        ASTContext.curAst.set(child);
    }

    public void destroy() {
        if (!ASTContext.parseAst.get()) {
            return;
        }
        ASTContext.curAst.set(c);
        ASTContext.curAstHelp.set(h);
        this.c = null;
        this.h = null;
    }

    // Cierre controlado (Trigger del "destructor" en Java)
    @Override
    public void close() {
        if (c != null || h != null) {
            destroy();
        }
    }

    // Métodos auxiliares equivalentes a las macros AST_CLOSE y AST_CLOSE_ID
    public void closeWithEnd(int p) {
        ASTContext.curAst.get().e = p;
        this.destroy();
    }

    public void closeWithEndAndId(int p, long n) {
        ASTContext.curAst.get().e = p;
        ASTContext.curAst.get().u = n;
        this.destroy();
    }
}

