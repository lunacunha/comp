package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL.getNodeName(), this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR.getNodeName(), this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRef, SymbolTable table) {
        String name = varRef.get("name");

        // LITERALS não são variáveis
        if (name.equals("true") || name.equals("false")) {
            return null;
        }

        boolean declared = false;
        if (table.getParameters(currentMethod).stream().anyMatch(p -> p.getName().equals(name))) {
            declared = true;
        } else if (table.getLocalVariables(currentMethod).stream().anyMatch(v -> v.getName().equals(name))) {
            declared = true;
        } else if (table.getFields().stream().anyMatch(f -> f.getName().equals(name))) {
            declared = true;
        }

        if (!declared && !name.equals("this")) {
            addReport(Report.newError(Stage.SEMANTIC, varRef.getLine(), varRef.getColumn(),
                    "Variable '" + name + "' does not exist.", null));
        }

        return null;
    }

}
