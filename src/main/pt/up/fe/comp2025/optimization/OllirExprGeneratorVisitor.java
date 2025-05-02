package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.Ollir;
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
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(NEGATION_EXPR, this::visitNegationExpr);
        addVisit(PARENTHESES_EXPR, this::visitParenthesesExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(THIS_EXPR, this::visitThisExpr);



        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult defaultVisit(JmmNode jmmNode, Void unused) {
        return new OllirExprResult("");
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this" + node.getAncestor(CLASS_DECL).get().get("name"));
    }

    private OllirExprResult visitIntegerLiteral(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        var boolType = TypeUtils.newBooleanType();
        String ollirType = ollirTypes.toOllirType(boolType);
        String code = (node.get("value").equals("true") ? 1 : 0) + ollirType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParenthesesExpr(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    private OllirExprResult visitNegationExpr(JmmNode node, Void unused) {
        OllirExprResult exprResult = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder();
        computation.append(exprResult.getComputation());
        Type exprType = types.getExprType(node);
        String type = ollirTypes.toOllirType(exprType);
        String temp = ollirTypes.nextTemp();

        computation.append(temp + type + SPACE + ASSIGN + type + SPACE + "!" + type + SPACE + exprResult.getCode() + END_STMT);

        return new OllirExprResult(temp+type, computation);
    }

    private String getArrayType(JmmNode node) {
        String method = node.getAncestor(METHOD_DECL).get().get("name");
        String arrayName = node.get("name");
        for (int i = 0; i < table.getLocalVariables(method).size(); i++) {
            if (table.getLocalVariables(method).get(i).getName().equals(arrayName)) {
                return ollirTypes.toOllirType(table.getLocalVariables(method).get(i).getType());
            }
        }
        for (int i = 0; i < table.getParameters(method).size(); i++) {
            if (table.getParameters(method).get(i).getName().equals(arrayName)) {
                return ollirTypes.toOllirType(table.getParameters(method).get(i).getType());
            }
        }
        for (int i = 0; i < table.getFields().size(); i++) {
            if (table.getFields().get(i).getName().equals(arrayName)) {
                return ollirTypes.toOllirType(table.getFields().get(i).getType());
            }
        }
        return null;
    }

    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String type = ollirTypes.toOllirType(types.getExprType(node));
        String tmp = ollirTypes.nextTemp() + type;

        computation.append(tmp + SPACE + ASSIGN + type + SPACE + "new(" + type + ")" + type  + END_STMT + "invokespecial("
                    + tmp + ", \"<init>\").V;\n");

        return new OllirExprResult(tmp, computation);
    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        OllirExprResult arr = visit(node.getChild(0));
        StringBuilder computation = new StringBuilder();
        computation.append(arr.getComputation());
        String tmp = ollirTypes.nextTemp() + ".array.i32";

        String arrayCode = "new(array, " + arr.getCode() + ").array.i32";

        computation.append(tmp +" :=.array.i32 " + arrayCode + END_STMT);

        return new OllirExprResult(tmp, computation.toString());
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        OllirExprResult arr = visit(node.getChild(0));
        OllirExprResult index = visit(node.getChild(1));

        String type = getArrayType(node.getChild(0));
        String tmp = ollirTypes.nextTemp() + type;

        computation.append(arr.getComputation());
        computation.append(index.getComputation());
        computation.append(tmp + SPACE + ASSIGN + type + SPACE + arr.getCode() + "[" + index.getCode() + "]" + type + END_STMT);

        return new OllirExprResult(tmp, computation);
    }

    private OllirExprResult visitFieldAccess(JmmNode node, Void unused) {
        OllirExprResult arr = visit(node.getChild(0));

        String tempVar = ollirTypes.nextTemp() + ".i32";

        StringBuilder computation = new StringBuilder();
        computation.append(arr.getComputation());

        computation.append(tempVar + SPACE + ASSIGN + ".i32 array"+node.get("name")+"("+arr.getCode()+").i32" + END_STMT);

        return new OllirExprResult(tempVar, computation.toString());
    }

    private OllirExprResult visitBinaryExpr(JmmNode node, Void unused) {
        OllirExprResult left = visit(node.getChild(0));
        OllirExprResult right = visit(node.getChild(1));
        String op = node.get("op");
        if (op.equals("&&")) {
            return visitAndExpr(node, unused);
        }
        else {
            StringBuilder computation = new StringBuilder();
            computation.append(left.getComputation());
            computation.append(right.getComputation());
            Type exprType = types.getExprType(node);
            String type = ollirTypes.toOllirType(exprType);
            String temp = ollirTypes.nextTemp();

            computation.append(temp).append(type).append(SPACE).append(ASSIGN).append(type)
                    .append(SPACE).append(left.getCode()).append(SPACE).append(op).append(SPACE).append(type).append(SPACE)
                    .append(right.getCode()).append(END_STMT);
            return new OllirExprResult(temp+type,computation);
        }
    }

    private OllirExprResult visitAndExpr(JmmNode node, Void unused) {
        OllirExprResult left = visit(node.getChild(0));
        OllirExprResult right = visit(node.getChild(1));
        StringBuilder computation = new StringBuilder();
        computation.append(left.getComputation());
        computation.append(right.getComputation());
        String andTemp = ollirTypes.nextTemp("andTmp");
        String thenLabel = ollirTypes.nextTemp("then");
        String endifLabel = ollirTypes.nextTemp("endif");
        String type = ".bool";

        computation.append("if (").append(left.getCode()).append(") goto ").append(thenLabel).append(END_STMT);
        computation.append(andTemp + type + ASSIGN + type + " 0.bool" + END_STMT + "goto " + endifLabel + END_STMT);
        computation.append(thenLabel + ":\n" + andTemp + type + ASSIGN + type +SPACE+ right.getCode() + END_STMT);
        computation.append(endifLabel + ":\n");

        return new OllirExprResult(andTemp+type, computation);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        OllirExprResult caller = visit(node.getChild(0));
        String methodName = node.get("name");
        OllirExprResult args = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();
        computation.append(caller.getComputation());
        computation.append(args.getComputation());
        String temp = ollirTypes.nextTemp();
        String type = ollirTypes.toOllirType(types.getExprType(node));


        if (node.getParent().getKind().equals("AssignStatement")) {
            computation.append(temp + type + SPACE + ASSIGN + type + SPACE);
        }

        computation.append("invokestatic(" + caller.getCode() + ", \"" + methodName + "\"");

        if (args != null) {
            computation.append(", ");
            computation.append(String.join(", ", args.getCode()));
        }

        computation.append(").V" + END_STMT);

        return new OllirExprResult(temp+type,computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = types.getExprType(node);
        String ollirType = ollirTypes.toOllirType(type);
        if (isField(node)) {
            StringBuilder computation = new StringBuilder();
            String tmp = ollirTypes.nextTemp()+ollirType;
            computation.append(tmp+SPACE+ASSIGN+ollirType+SPACE+"getfield(this, "+id+ollirType+")"+ollirType+END_STMT);
            return new OllirExprResult(tmp,computation);
        }
        for (var local : table.getLocalVariables(node.getAncestor(METHOD_DECL).get().get("name"))) {
            if (local.getName().equals(id)) {
                return new OllirExprResult(id + ollirType);
            }
        }
        return new OllirExprResult(id);
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


}
