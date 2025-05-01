package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

import java.util.Optional;

public class TypeUtils {

    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type newIntArrayType() {
        return new Type("int", true);
    }

    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    public static Type newBooleanArrayType() {
        return new Type("boolean", true);
    }

    public static Type convertType(JmmNode typeNode) {
        switch (typeNode.getKind()) {
            case "IntType":
                return newIntType();
            case "BooleanType":
                return newBooleanType();
            case "IntArrayType":
                return newIntArrayType();
            case "BooleanArrayType":
                return newBooleanArrayType();
            case "ClassType":
                return new Type(typeNode.get("name"), false);
            case "VarArgInt", "VarargParam", "VarArgsTest":
                return new Type("int...", true);
            case "VoidType":
                return new Type("void", false);
            default:
                return new Type(typeNode.get("name"), false);
        }
    }

    public static boolean isInt(Type t) {
        return t.getName().startsWith("int") && !t.isArray();
    }

    public static boolean isBoolean(Type t) {
        return t.getName().equals("boolean") && !t.isArray();
    }

    public static boolean isValidVararg(Type type) {
        return type.getName().equals("int...");
    }

    public static boolean isPrimitive(String typeName) {
        return typeName.equals("int") || typeName.equals("boolean") || typeName.equals("void");
    }

    public boolean isCompatible(Type expected, Type actual) {
        if (expected.equals(actual)) return true;

        if (actual.getName().equals(table.getClassName()) && expected.getName().equals(table.getSuper())) {
            return true;
        }
        if (expected.getName().startsWith("VarArg") && actual.equals(new Type("int...",true))) return true;

        boolean expectedImported = table.getImports().stream().anyMatch(imp -> imp.endsWith("." + expected.getName()));
        boolean actualImported = table.getImports().stream().anyMatch(imp -> imp.endsWith("." + actual.getName()));
        if (expectedImported && actualImported && expected.getName().equals(actual.getName())) {
            return true;
        }

        if (isPrimitive(expected.getName()) && isPrimitive(actual.getName())) {
            return expected.getName().equals(actual.getName());
        }

        return false;
    }


    public boolean canAssignThisTo(Type targetType) {
        String className = table.getClassName();
        String superClass = table.getSuper();

        return targetType.getName().equals(className) ||
                (superClass != null && targetType.getName().equals(superClass));
    }

    public Type getVarType(String name, String methodName) {
        Optional<Symbol> symbol =
                table.getLocalVariables(methodName).stream().filter(s -> s.getName().equals(name)).findFirst()
                        .or(() -> table.getParameters(methodName).stream().filter(s -> s.getName().equals(name)).findFirst())
                        .or(() -> table.getFields().stream().filter(s -> s.getName().equals(name)).findFirst());

        return symbol.map(Symbol::getType).orElse(new Type("unknown", false));
    }

    public boolean hasVarargs(String methodName) {
        var params = table.getParameters(methodName);
        return !params.isEmpty() && params.get(params.size() - 1).getType().isArray();
    }

    public boolean isValidArrayLiteralAssignment(Type target, Type source) {
        return source.isArray() && source.getName().equals("int") && target.isArray() && target.getName().equals("int");
    }

    public Type getExprType(JmmNode node) {

        Type ret =  switch (node.getKind()) {
            case "IntegerLiteral" -> {
                yield TypeUtils.newIntType();
            }
            case "FieldAccess" -> {
                String fieldName = node.get("name");

                if (fieldName.equals("length")) {
                    JmmNode target = node.getChild(0);
                    Type targetType = getExprType(target);

                    // Verifica se estamos a aceder ao length de um array
                    if (targetType.isArray()) {
                        yield new Type("int", false); // `.length` retorna sempre int
                    } else {
                        yield new Type("unknown", false);
                    }
                } else {
                    yield new Type("unknown", false); // you can later add support for object fields
                }
            }
            case "BooleanLiteral" -> TypeUtils.newBooleanType();
            case "VarRefExpr" -> {
                String varName = node.get("name");

                if (table.getMethods().contains(varName)) {
                    yield new Type("unknown", false); // ignora
                }
                if (varName.equals("true") || varName.equals("false")) {
                    yield TypeUtils.newBooleanType();
                }
                try {
                    yield getVarType(varName, node.getAncestor("MethodDecl").get().get("name"));
                } catch (RuntimeException e) {
                    yield new Type("unknown", false);
                }
            }
            case "ThisExpr" -> {
                if ("main".equals(node.getAncestor("MethodDecl").get().get("name"))) {
                    yield new Type("unknown", false);
                }
                else yield new Type(table.getClassName(), false);
            }
            case "NewArray" -> TypeUtils.newIntArrayType();
            case "NewObject" -> {
                if (node.get("name").startsWith("VarArgs")) yield new Type("int...", true);
                else yield new Type(node.get("name"), false);
            }
            case "ArrayAccess" -> {
                Type arr = getExprType(node.getChild(0));
                if (arr.getName().equals("int...")) yield new Type("int",false);
                else yield arr.isArray() ? new Type(arr.getName(), false) : new Type("unknown", false);
            }
            case "BinaryExpr" -> {
                String op = node.get("op");
                yield (op.equals("&&") || op.equals("<") || op.equals("==")) ?
                        TypeUtils.newBooleanType() : TypeUtils.newIntType();
            }

            case "ArrayInit" -> {
                Type elemType = getExprType(node.getChild(0));
                if (node.getChildren().isEmpty()) yield TypeUtils.newIntArrayType();
                else yield new Type(elemType.getName(), true);
            }
            case "VarArgs", "VarArg", "VarArgInt","VarArgsTest" -> new Type("int...", true);
            case "VarArgBool" -> new Type("boolean", true);
            case "NegationExpr" -> TypeUtils.newBooleanType();
            case "MethodCall", "LocalMethodCall" -> {
                String method = node.hasAttribute("methodName") ? node.get("methodName") : node.get("name");
                if (!table.getMethods().contains(method)) yield new Type("unknown", false);
                else yield table.getReturnType(method);
            }

            default -> new Type("unknown", false);
        };
        return ret;
    }
}
