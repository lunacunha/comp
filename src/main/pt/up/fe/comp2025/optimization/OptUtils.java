package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;


/**
 * Utility methods related to the optimization middle-end.
 */
public class OptUtils {

    private static final AccumulatorMap<String> temporaries = new AccumulatorMap<>();
    private final TypeUtils types;

    public OptUtils(TypeUtils types) {
        this.types = types;
    }

    public static String nextTemp() {
        return nextTemp("tmp");
    }

    public static String nextTemp(String prefix) {
        var nextTempNum = temporaries.add(prefix) - 1;
        return prefix + nextTempNum;
    }

    public String toOllirType(JmmNode typeNode) {
        String ret = "";
        switch (typeNode.getKind()){
            //TODO: Faltam tipos
            case "IntType", "IntegerLiteral":
                ret += ".i32";
                break;
            case "ClassArrayType":
                ret += ".array." + typeNode.get("name");
                break;
            case "IntArrayType":
                ret += ".array.i32";
                break;
            case "BooleanLiteral","BooleanType":
                ret += ".bool";
                break;
            case "BinaryExpr":
                if (typeNode.get("op").equals("&&") || typeNode.get("op").equals("<")) {
                    ret += ".bool";
                    break;
                }
                ret += ".i32";
                break;
            default:
                ret += ".V";
                break;
        }
        return ret;
    }

    public String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array." + switch (type.getName()) {
                case "int","int..." -> "i32";
                case "boolean" -> "bool";
                default -> type.getName();
            };
        }

        return "." + switch (type.getName()) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "V";
            default -> type.getName(); // tipo de classe
        };
    }
}
