package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.*;
import java.util.stream.Collectors;

public class UndeclaredMethod extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.LOCAL_METHOD_CALL_EXPR.getNodeName(), this::visitMethodCall);
        addVisit(Kind.METHOD_CALL_EXPR.getNodeName(), this::visitMethodCall);

        addVisit(Kind.IMPORT_DECL.getNodeName(), this::visitImportDecl);
    }

    private Void visitMethodCall(JmmNode node, SymbolTable table) {
        String methodName = node.getKind().equals(Kind.LOCAL_METHOD_CALL_EXPR.getNodeName())
                ? node.get("name")
                : node.get("methodName");

        if (table.getMethods().contains(methodName)) return null;

        if (isMethodAccessible(table, methodName)) return null;

        addReport(Report.newError(Stage.SEMANTIC, node.getLine(), node.getColumn(),
                "Method '" + methodName + "' was not declared", null));

        return null;
    }

    private Void visitImportDecl(JmmNode node, SymbolTable table) {
        // Conta os imports pela lista da SymbolTable
        Map<String, Integer> importCount = new HashMap<>();

        for (String imp : table.getImports()) {
            importCount.put(imp, importCount.getOrDefault(imp, 0) + 1);
        }

        // Agora verifica se algum aparece mais que uma vez
        for (Map.Entry<String, Integer> entry : importCount.entrySet()) {
            if (entry.getValue() > 1) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        node.getLine(),
                        node.getColumn(),
                        "Duplicate import found: '" + entry.getKey() + "'",
                        null
                ));
            }
        }

        return null;
    }



    private boolean isMethodAccessible(SymbolTable table, String methodName) {
        // Métodos de superclasse são sempre acessíveis
        if (table.getSuper() != null && !table.getSuper().isEmpty()) return true;

        // Métodos de classes importadas (assumindo que qualquer método em imports é válido)
        return !table.getImports().isEmpty();
    }
}
