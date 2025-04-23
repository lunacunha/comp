package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static pt.up.fe.comp2025.ast.Kind.TYPE;

/**
 * Utility methods related to the optimization middle-end.
 */
public class OptUtils {

    private final AccumulatorMap<String> temporaries;
    private final TypeUtils types;

    public OptUtils(TypeUtils types) {
        this.types = types;
        this.temporaries = new AccumulatorMap<>();
    }

    public String nextTemp() {
        return nextTemp("tmp");
    }

    public String nextTemp(String prefix) {
        var nextTempNum = temporaries.add(prefix) - 1;
        return prefix + nextTempNum;
    }

    public String toOllirType(JmmNode typeNode) {
        TYPE.checkOrThrow(typeNode);
        return toOllirType(types.convertType(typeNode));
    }

    public String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array." + switch (type.getName()) {
                case "int" -> "i32";
                case "boolean" -> "bool";
                default -> throw new NotImplementedException("Unsupported array type: " + type.getName());
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
