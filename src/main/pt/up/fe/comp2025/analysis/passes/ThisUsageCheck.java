package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

public class ThisUsageCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL.getNodeName(), this::enterMethod);
        addVisit(Kind.THIS_EXPR.getNodeName(), this::checkThis);
    }

    private Void enterMethod(JmmNode node, SymbolTable table) {
        currentMethod = node.get("name");
        return null;
    }

    private Void checkThis(JmmNode node, SymbolTable table) {
        if ("main".equals(currentMethod)) {
            addReport(Report.newError(Stage.SEMANTIC, node.getLine(), node.getColumn(),
                    "'this' cannot be used in static method 'main'", null));
        }
        return null;
    }
}
