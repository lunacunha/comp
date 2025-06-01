package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    public JasminUtils(OllirResult ollirResult) {
        // Can be useful to have if you expand this class with more methods
        this.ollirResult = ollirResult;
    }


    public String getModifier(AccessModifier accessModifier) {
        return accessModifier != AccessModifier.DEFAULT ?
                accessModifier.name().toLowerCase() + " " :
                "";
    }

    public String getImportPath(String name, Method method, String className) {
        if (name.equals("this")) {
            return method.getOllirClass().getClassName();
        }
        for (var imp : method.getOllirClass().getImports()) {
            if (imp.endsWith("."+name)) {
                return imp.replace('.', '/');
            }
            else if (imp.equals(name)) {
                return imp;
            }
        }
        if (className.startsWith("OBJECTREF(")) {
            int startIndex = className.indexOf('(') + 1;
            int endIndex = className.indexOf(')');
            return className.substring(startIndex, endIndex);
        }
        throw new NotImplementedException(className);
    }

    public String toJasminType(Type type, boolean isNew) {
        if (type instanceof BuiltinType builtinType) {
            switch (builtinType.getKind()) {
                case INT32:
                    return "I";
                case BOOLEAN:
                    return "Z";
                case STRING:
                    return "Ljava/lang/String;";
                case VOID:
                    return "V";
                default:
                    throw new NotImplementedException();
            }
        }  else if (type instanceof ClassType classType) {
            if (isNew) {return classType.getName().replace('.', '/');}
            return "L" + classType.getName().replace('.', '/') + ";";
        } else if (type instanceof ArrayType arrayType) {
            String res = "";
            for (int i = 0; i < arrayType.getNumDimensions(); i++) {
                res += "[";
            }
            res += toJasminType(arrayType.getElementType(), isNew);
            return res;
        } else {
            throw new NotImplementedException();
        }
    }
}
