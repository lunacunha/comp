package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;

import java.util.ArrayList;
import java.util.List;


public class ImportUsageCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit("MethodCall", this::visitMethodCall);
    }

    private Void visitMethodCall(JmmNode node, SymbolTable symbolTable) {
        String methodName = node.get("method");

        if (isImportedMethod(methodName, symbolTable)) {
            if (!isUsedCorrectly(node)) {
                addReport(new Report(ReportType.ERROR, Stage.SEMANTIC, node.getLine(),
                        "Método importado " + methodName + " usado incorretamente"));
            }
        }
        return null;
    }

    private boolean isImportedMethod(String methodName, SymbolTable symbolTable) {
        return symbolTable.getImports().stream().anyMatch(importName -> methodName.startsWith(importName + "."));
    }

    private boolean isUsedCorrectly(JmmNode node) {
        return node.getParent().getKind().equals("AssignStatement") ||
                node.getParent().getKind().equals("ExprStatement");
    }
}