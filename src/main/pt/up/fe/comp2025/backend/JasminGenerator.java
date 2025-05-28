package pt.up.fe.comp2025.backend;

import com.sun.jdi.VoidType;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final JasminUtils types;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        types = new JasminUtils(ollirResult);

        this.generators = new FunctionClassMap<>();

        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(GotoInstruction.class, this::generateGoToInstruction);
        generators.put(InvokeStaticInstruction.class, this::generateInvokeStatic);
    }

    private String generateInvokeStatic(InvokeStaticInstruction invokeStaticInstruction) {
        return null;
    }

    private String generateGoToInstruction(GotoInstruction gotoInstruction) {
        //TODO : LABELS
        return "goto " + gotoInstruction.getLabel() + NL;
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInstruction) {
        var code = new StringBuilder();
        var condition = singleOpCondInstruction.getCondition().getSingleOperand();
        String label = singleOpCondInstruction.getLabel();

        code.append(apply(condition));

        code.append("ifne ").append(label).append("\n");

        return code.toString();
    }

    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        code.append("; ").append(node).append(NL);

        code.append(generators.apply(node));

        return code.toString();
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: When you support 'extends', this must be updated
        var fullSuperClass = "java/lang/Object";

        code.append(".super ").append(fullSuperClass).append(NL);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """.formatted(fullSuperClass);
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        var params = "I";
        var returnType = "I";

        code.append("\n.method ").append(modifier)
                .append(methodName)
                .append("(" + params + ")" + returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        int maxlocals = 0;
        for (var variable : method.getVarTable().keySet()) {
            var descriptor = method.getVarTable().get(variable);
            if (descriptor != null) {
                if (descriptor.getVirtualReg() + 1 > maxlocals) {
                    maxlocals = descriptor.getVirtualReg() + 1;
                }
            }
        }
        code.append(TAB).append(".limit locals ").append(maxlocals).append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            if (method.getLabels(inst) != null) {
                code.append(TAB).append(".label ").append(instCode).append(NL);
            }
            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName());

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("istore_").append(reg.getVirtualReg()).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        //TODO : ONLY SUPPORTS INTS
        if (Integer.valueOf(literal.getLiteral()) == -1) return "iconst_m1" + NL;
        if (Integer.valueOf(literal.getLiteral()) < 6 && Integer.valueOf(literal.getLiteral()) >= 0) {
            return "iconst_" + literal.getLiteral() + NL;
        }
        else if (Integer.valueOf(literal.getLiteral()) < 128 && Integer.valueOf(literal.getLiteral()) >= -128) {
            return "bipush " + literal.getLiteral() + NL;
        }
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName());

        // TODO: Hardcoded for int type, needs to be expanded
        return "iload " + reg.getVirtualReg() + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(apply(binaryOp.getLeftOperand()));
        code.append(apply(binaryOp.getRightOperand()));


        // apply operation
        switch (binaryOp.getOperation().getOpType()) {
            case ADD -> code.append("iadd" + NL);
            case MUL -> code.append("imul" + NL);
            case LTH,GTE -> code.append(generateLesserOp(binaryOp));
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        return code.toString();
    }

    private String generateLesserOp(BinaryOpInstruction binaryOp) {
        return null;
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        switch (returnInst.getReturnType().toString()) {
            case "VOID":
                code.append("return").append(NL);
                break;
            case "INT32":
                code.append("ireturn").append(NL);
                break;
            default:
                code.append("areturn").append(NL);
                break;
        }

        return code.toString();
    }
}