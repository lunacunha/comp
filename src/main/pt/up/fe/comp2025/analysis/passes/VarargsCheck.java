package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;

public class VarargsCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL.getNodeName(), this::visitMethod);
    }

    private boolean isVararg(Type type) {
        return type.getName().endsWith("...");
    }

    private Void visitMethod(JmmNode method, SymbolTable table) {
        String methodName = method.get("name");
        List<Symbol> parameters = table.getParameters(methodName);
        boolean foundVararg = false;

        for (int i = 0; i < parameters.size(); i++) {
            Symbol param = parameters.get(i);
            Type paramType = param.getType();

            if (isVararg(paramType)) {
                if (foundVararg) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Multiple varargs parameters in method '" + methodName + "'", null));
                }
                if (i != parameters.size() - 1) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter must be last in method '" + methodName + "'", null));
                }
                foundVararg = true;

                // Ensure vararg type is int... or boolean...
                if (!paramType.getName().equals("int...")) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter must be of type int", null));
                }
            }
        }
        return null;
    }
}