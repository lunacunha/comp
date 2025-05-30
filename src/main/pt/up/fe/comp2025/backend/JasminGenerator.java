package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
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

    private int counter = 0;

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
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(GotoInstruction.class, this::generateGoToInstruction);
        generators.put(InvokeStaticInstruction.class, this::generateInvokeStatic);
        generators.put(InvokeSpecialInstruction.class, this::generateInvokeSpecial);
        generators.put(InvokeVirtualInstruction.class, this::generateInvokeVirtual);
        generators.put(NewInstruction.class, this::generateNew);
        generators.put(ArrayLengthInstruction.class, this::generateArrayLength);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInstruction) {
        var code = new StringBuilder();

        code.append(apply(arrayLengthInstruction.getCaller()));

        code.append("arraylength").append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        //TODO
        return "; " + unaryOpInstruction.toString() + NL;
    }

    private String generateOpCond(OpCondInstruction opCondInstruction) {
        var code      = new StringBuilder();
        var cond      = opCondInstruction.getCondition();
        var operands  = cond.getOperands();
        String label  = opCondInstruction.getLabel();

        if (operands.size() != 2) {
            throw new RuntimeException("Expected 2 operands for comparison, got " + operands.size());
        }
        var left  = operands.get(0);
        var right = operands.get(1);
        var op    = cond.getOperation().getOpType();

        if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
            code.append(apply(left));
            switch (op) {
                case LTH  -> code.append("iflt  ").append(label).append(NL);
                case GTE  -> code.append("ifge  ").append(label).append(NL);
                case GTH  -> code.append("ifgt  ").append(label).append(NL);
                case LTE  -> code.append("ifle  ").append(label).append(NL);
                case EQ   -> code.append("ifeq  ").append(label).append(NL);
                case NEQ  -> code.append("ifne  ").append(label).append(NL);
                default   -> throw new NotImplementedException(op);
            }
            return code.toString();
        }

        code.append(apply(left));
        code.append(apply(right));
        switch (op) {
            case LTH  -> code.append("if_icmplt ").append(label).append(NL);
            case GTH  -> code.append("if_icmpgt ").append(label).append(NL);
            case LTE  -> code.append("if_icmple ").append(label).append(NL);
            case GTE  -> code.append("if_icmpge ").append(label).append(NL);
            case EQ   -> code.append("if_icmpeq ").append(label).append(NL);
            case NEQ  -> code.append("if_icmpne ").append(label).append(NL);
            default   -> throw new NotImplementedException(op);
        }
        return code.toString();
    }
    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        //TODO
        return "; " + getFieldInstruction.toString() + NL;
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        //TODO
        return "; " + putFieldInstruction.toString() + NL;
    }

    private String generateInvokeVirtual(InvokeVirtualInstruction invokeVirtualInstruction) {
        var code = new StringBuilder();
        var params = "";
        for (int i = 0; i < invokeVirtualInstruction.getArguments().size(); i++) {
            var argument = invokeVirtualInstruction.getArguments().get(i);
            params += types.toJasminType(argument.getType());
            code.append(apply(argument));
        }
        var name = ((Operand) invokeVirtualInstruction.getCaller()).getName();
        var className =  invokeVirtualInstruction.getCaller().getType().toString();
        code.append("invokevirtual " + types.getImportPath(name,currentMethod,className) + "/" +
                ((LiteralElement) invokeVirtualInstruction.getMethodName()).getLiteral()
                + "(" + params + ")" + types.toJasminType(invokeVirtualInstruction.getReturnType()) + NL);
        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction invokeSpecialInstruction) {
        //TODO
        return ";" + invokeSpecialInstruction.toString() + NL;
    }

    private String generateNew(NewInstruction newInstruction) {
        var code = new StringBuilder();

        var arguments = newInstruction.getArguments();

        if (arguments != null && arguments.size() > 0) {
            var sizeOperand = arguments.get(0);
            code.append(apply(sizeOperand));

            var returnType = newInstruction.getReturnType().toString();

            if (returnType.contains("INT32") || returnType.contains("i32")) {
                code.append("newarray int").append(NL);
            } else if (returnType.contains("BOOLEAN") || returnType.contains("bool")) {
                code.append("newarray boolean").append(NL);
            } else {
                String elementType = returnType.replace("array.", "").replace("[]", "");
                code.append("anewarray ").append(elementType).append(NL);
            }
        } else {
            var caller = newInstruction.getCaller();
            var className = caller.getType().toString();

            code.append("new ").append(className).append(NL);
            code.append("dup").append(NL); // Duplicate reference for constructor call
        }

        return code.toString();
    }

    private String generateInvokeStatic(InvokeStaticInstruction invokeStaticInstruction) {
        var code = new StringBuilder();
        var params = "";
        for (int i = 0; i < invokeStaticInstruction.getArguments().size(); i++) {
            var argument = invokeStaticInstruction.getArguments().get(i);
            params += types.toJasminType(argument.getType());
            code.append(apply(argument));
        }
        var name = ((Operand) invokeStaticInstruction.getCaller()).getName();
        var className = invokeStaticInstruction.getCaller().getType().toString();
        code.append("invokestatic " + types.getImportPath(name,currentMethod, className) + "/" +
                ((LiteralElement) invokeStaticInstruction.getMethodName()).getLiteral()
                + "(" + params + ")" + types.toJasminType(invokeStaticInstruction.getReturnType()) + NL);
        return code.toString();
    }

    private String generateGoToInstruction(GotoInstruction gotoInstruction) {
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
        //code.append("; ").append(node).append(NL);

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
        var isStatic = "";
        if (method.isStaticMethod()) {
            isStatic = "static ";
        }

        var methodName = method.getMethodName();

        var params = "";
        for (int i = 0; i < method.getParams().size(); i++) {
            var param = method.getParam(i);
            params += types.toJasminType(param.getType());

        }
        var returnType = types.toJasminType(((ReturnInstruction) method.getInstr(method.getInstructions().size() - 1)).getReturnType());

        code.append("\n.method ").append(modifier).append(isStatic)
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
            for (var lbl : method.getLabels(inst)) {
                code.append(TAB + lbl + ":" + NL);
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

        code.append(apply(assign.getRhs()));

        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = operand.getType().toString();

        if (type.contains("[]") || type.contains("array") || type.contains("ARRAY")) {
            // Reference -> astore
            if (reg <= 3) code.append("astore_").append(reg).append(NL);
            else code.append("astore ").append(reg).append(NL);
        } else {
            // int and bool -> istore
            if (reg <= 3) code.append("istore_").append(reg).append(NL);
            else code.append("istore ").append(reg).append(NL);
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        if (Integer.valueOf(literal.getLiteral()) == -1) return "iconst_m1" + NL;
        if (Integer.valueOf(literal.getLiteral()) < 6 && Integer.valueOf(literal.getLiteral()) >= 0) {
            return "iconst_" + literal.getLiteral() + NL;
        }
        else if (Integer.valueOf(literal.getLiteral()) < 128 && Integer.valueOf(literal.getLiteral()) >= -128) {
            return "bipush " + literal.getLiteral() + NL;
        }
        else if (Integer.valueOf(literal.getLiteral()) < 32768 && Integer.valueOf(literal.getLiteral()) >= -32768) {
            return "sipush " + literal.getLiteral() + NL;
        }
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {

        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = operand.getType().toString();

        if (type.contains("[]") || type.contains("array") || type.contains("ARRAY")) {
            // reference -> aload
            if (reg <= 3) return "aload_" + reg + NL;
            return "aload " + reg + NL;
        } else {
            // int and bool -> iload
            if (reg <= 3) return "iload_" + reg + NL;
            return "iload " + reg + NL;
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        switch (binaryOp.getOperation().getOpType()) {
            case ADD  -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("iadd").append(NL);
                return code.toString();
            }
            case MUL  -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("imul").append(NL);
                return code.toString();
            }
            case SUB  -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("isub").append(NL);
                return code.toString();
            }
            case DIV  -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("idiv").append(NL);
                return code.toString();
            }
            case LTH  -> {
                return generateLesserOp(binaryOp);
            }
            default   -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }
    }


    private String generateLesserOp(BinaryOpInstruction binOp) {
        var code     = new StringBuilder();
        var left     = binOp.getLeftOperand();
        var right    = binOp.getRightOperand();

        int  idT     = counter++;
        int  idE     = counter++;
        String lblT  = "cmpTrue" + idT;
        String lblE  = "cmpEnd"  + idE;

        code.append(apply(left));
        code.append(apply(right));

        if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
            code.append("iflt  ").append(lblT).append(NL);
        } else {
            code.append("if_icmplt ").append(lblT).append(NL);
        }

        code.append("iconst_0").append(NL);
        code.append("goto ").append(lblE).append(NL);

        code.append(lblT).append(":").append(NL);
        code.append("iconst_1").append(NL);

        code.append(lblE).append(":").append(NL);

        return code.toString();
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