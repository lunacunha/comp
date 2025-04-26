package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final TypeUtils types;
    private final OptUtils ollirTypes;


    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(NORMAL_PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        var varCode = left.get("name") + typeString;


        code.append(varCode);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // TODO: Hardcoded for int type, needs to be expanded
        Type retType = TypeUtils.newIntType();


        StringBuilder code = new StringBuilder();


        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;


        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    //TODO: Não é para por tudo no visitMethodDecl ?
    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");
        boolean isPublic = node.hasAttribute("pub") && node.get("pub").equals("public");
        boolean isStatic = node.hasAttribute("stat") && node.get("stat").equals("static");

        if (isPublic) {
            code.append("public ");
        }
        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        List<String> paramsList = new ArrayList<>();
        for (int i = 1; i < node.getNumChildren(); i++) {
            if (node.getChild(i).getKind().equals("NormalParam")) {
                paramsList.add(node.getChild(i).get("name") + ollirTypes.toOllirType(node.getChild(i).getChild(0)));
            }
        }
        String paramsCode = String.join(", ", paramsList);
        code.append("(" + paramsCode + ")");

        // type
        code.append(ollirTypes.toOllirType(node.getChild(0)));
        code.append(L_BRACKET);


        // rest of its children stmts
        //TODO: E se não forem assignments ou returns ?
        for (int i = 0; i < node.getNumChildren(); i++){
            if (node.getChild(i).getKind().equals("AssignStatement")) {
                code.append(node.getChild(i).get("name") + ollirTypes.toOllirType(node.getChild(i).getChild(0))
                        + " := " + ollirTypes.toOllirType(node.getChild(i).getChild(0)) + " " +
                        node.getChild(i).getChild(0).get("value") +
                        ollirTypes.toOllirType(node.getChild(i).getChild(0)) + ";\n");
            }

        }
        getRefType("a",node.get("name"));
        for (int i = 0; i < node.getNumChildren(); i++){
            if (node.getChild(i).getKind().equals("ReturnStatement")) {
                //TODO : E se não for AdditionExpr ou VarRef ?
                if (node.getChild(i).getChild(0).getKind().equals("VarRefExpr")){
                    code.append("ret");
                    code.append(printVarRef(node.getChild(i).getChild(0).get("name"),node.get("name")));
                    code.append(";\n");
                }
                else if (node.getChild(i).getChild(0).getKind().equals("AdditionExpr")){
                    String currentTemp = ollirTypes.nextTemp();
                    code.append(currentTemp);
                    code.append(ollirTypes.toOllirType(node.getChild(0)) + " :=");
                    for (int j = 0;j < node.getChild(i).getChild(0).getChildren().size(); j++) {
                        JmmNode child = node.getChild(i).getChild(0).getChild(j);
                        System.out.println(child);
                        if (child.getKind().equals("VarRefExpr")){
                            //TODO : E se não for VarRef ?
                            code.append(printVarRef(child.get("name"),node.get("name")) + " ");
                        }
                        if (j == node.getChild(i).getChild(0).getChildren().size() - 1) code.append(";\n");
                        else code.append("+");
                    }
                    //TODO : E se não for só o tipo de return ?
                    code.append("ret" + ollirTypes.toOllirType(node.getChild(0)) + " " + currentTemp + ollirTypes.toOllirType(node.getChild(0)));
                    code.append(";\n");
                }
            }

        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String printVarRef(String var, String method) {
        Type type = getRefType(var, method);
        String str = ollirTypes.toOllirType(type) + " " + var + ollirTypes.toOllirType(type);
        return str;
    }

    private Type getRefType(String var, String method) {
        for (int i = 0; i < table.getLocalVariables(method).size(); i++) {
            if (table.getLocalVariables(method).get(i).getName().equals(var)) {
                return table.getLocalVariables(method).get(i).getType();
            }
        }
        return null;
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
