package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.List;
import java.util.stream.Collectors;

public class ReturnStatementCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL.getNodeName(), this::checkReturn);
    }

    private Void checkReturn(JmmNode method, SymbolTable table) {
        String methodName = method.get("name");
        Type methodType = table.getReturnType(methodName);


        if ("main".equals(methodName)) return null; // ignore main

        List<JmmNode> stmts = method.getChildren().stream()
                .filter(c -> c.getKind().endsWith("Statement"))
                .collect(Collectors.toList());

        if ("void".equals(methodType.getName())) {
            return null;
        }

        if (stmts.isEmpty() || !stmts.get(stmts.size() - 1).getKind().equals("ReturnStatement")) {
            addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                    "Method '" + methodName + "' must end with a return statement", null));
        }

        return null;
    }
}


