package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
    }


    @Override
    protected void buildVisitor() {
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(FIELD_ACCESS, this::visitFieldAccess);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        addVisit(ARRAY_INIT, this::visitArrayInit);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(NEGATION_EXPR, this::visitNegationExpr);
        addVisit(PARENTHESES_EXPR, this::visitParenthesesExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(THIS_EXPR, this::visitThisExpr);



//        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this");
    }

    private OllirExprResult visitIntegerLiteral(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        var boolType = TypeUtils.newBooleanType();
        String ollirIntType = ollirTypes.toOllirType(boolType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParenthesesExpr(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitNegationExpr(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitBinaryExpr(JmmNode node, Void unused) {
        return null;
    }


    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitFieldAccess(JmmNode node, Void unused) {
        return null;
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        return null;
    }



    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    public boolean isField(JmmNode node) {
        String varName = node.get("name");
        String method = node.getAncestor(METHOD_DECL).get().get("name");
        boolean isField = false;
        for (int i = 0; i < table.getFields().size(); i++) {
            if (table.getFields().get(i).getName().equals(varName)) {
                isField = true;
                break;
            }
        }
        if (!isField) return false;
        for (var local : table.getLocalVariables(method)) {
            if (local.getName().equals(varName)) {
                return false;
            }
        }
        for (var param : table.getParameters(method)) {
            if (param.getName().equals(varName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
