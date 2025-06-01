

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
    private static int currStackSize = 0;
    private static int maxStackSize = 0;

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
        generators.put(ArrayOperand.class, this::generateArrayOperand);
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        var code = new StringBuilder();

        var arrayName = arrayOperand.getName();
        var arrayVarDescriptor = currentMethod.getVarTable().get(arrayName);
        var arrayType = arrayVarDescriptor.getVarType();

        var arrayRef = new Operand(arrayName, arrayType);
        code.append(apply(arrayRef));

        code.append(apply(arrayOperand.getIndexOperands().get(0)));

        code.append("iaload").append(NL);
        currStackSize -= 1;

        return code.toString();
    }

    private String generateArrayLength(ArrayLengthInstruction arrayLengthInstruction) {
        var code = new StringBuilder();

        code.append(apply(arrayLengthInstruction.getCaller()));

        code.append("arraylength").append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();
        var operand = unaryOpInstruction.getOperand();
        var opType = unaryOpInstruction.getOperation().getOpType();

        code.append(apply(operand));

        switch (opType) {
            case NOT -> {
                int idT = counter++;
                int idE = counter++;
                String lblT = "notTrue" + idT;
                String lblE = "notEnd" + idE;

                code.append("ifeq ").append(lblT).append(NL);
                currStackSize -= 1;

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);
            }
            case NOTB -> {
                int idT = counter++;
                int idE = counter++;
                String lblT = "notTrue" + idT;
                String lblE = "notEnd" + idE;

                code.append("ifeq ").append(lblT).append(NL);
                currStackSize -= 1;

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);
            }
            default -> throw new NotImplementedException(opType);
        }

        return code.toString();
    }

    private String generateOpCond(OpCondInstruction opCondInstruction) {
        var code = new StringBuilder();
        var cond = opCondInstruction.getCondition();
        var operands = cond.getOperands();
        String label = opCondInstruction.getLabel();

        if (operands.size() != 2) {
            throw new RuntimeException("Expected 2 operands for comparison, got " + operands.size());
        }

        var left = operands.get(0);
        var right = operands.get(1);
        var op = cond.getOperation().getOpType();

        if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
            code.append(apply(left));
            currStackSize -= 1;
            switch (op) {
                case LTH -> code.append("iflt ").append(label).append(NL);
                case GTE -> code.append("ifge ").append(label).append(NL);
                case GTH -> code.append("ifgt ").append(label).append(NL);
                case LTE -> code.append("ifle ").append(label).append(NL);
                case EQ -> code.append("ifeq ").append(label).append(NL);
                case NEQ -> code.append("ifne ").append(label).append(NL);
                default -> throw new NotImplementedException(op);
            }
            return code.toString();
        }

        if (op == OperationType.ANDB) {
            int skipLabel = counter++;
            code.append(apply(left));
            code.append("ifeq skip_and_").append(skipLabel).append(NL); // if left is false -> skip
            currStackSize -= 1;
            code.append(apply(right));
            code.append("ifne ").append(label).append(NL); // if right is true -> jump
            currStackSize -= 1;
            code.append("skip_and_").append(skipLabel).append(":").append(NL);
            return code.toString();
        }

        if (op == OperationType.ORB) {
            code.append(apply(left));
            code.append("ifne ").append(label).append(NL); // if left is true -> jump
            currStackSize -= 1;
            code.append(apply(right));
            code.append("ifne ").append(label).append(NL); // if right is true -> jump
            currStackSize -= 1;
            return code.toString();
        }

        code.append(apply(left));
        code.append(apply(right));
        currStackSize -= 2;

        switch (op) {
            case LTH -> code.append("if_icmplt ").append(label).append(NL);
            case GTH -> code.append("if_icmpgt ").append(label).append(NL);
            case LTE -> code.append("if_icmple ").append(label).append(NL);
            case GTE -> code.append("if_icmpge ").append(label).append(NL);
            case EQ -> code.append("if_icmpeq ").append(label).append(NL);
            case NEQ -> code.append("if_icmpne ").append(label).append(NL);
            default -> throw new NotImplementedException(op);
        }

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        code.append(apply(getFieldInstruction.getObject()));

        var fieldOperand = getFieldInstruction.getField();
        var fieldName = ((Operand) fieldOperand).getName();
        var fieldType = getFieldInstruction.getFieldType();
        var objectType = getFieldInstruction.getObject().getType();

        String className;
        if (objectType.toString().startsWith("OBJECTREF(")) {
            int startIndex = objectType.toString().indexOf('(') + 1;
            int endIndex = objectType.toString().indexOf(')');
            className = objectType.toString().substring(startIndex, endIndex);
        } else {
            className = currentMethod.getOllirClass().getClassName();
        }

        code.append("getfield ")
                .append(className.replace('.', '/'))
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(types.toJasminType(fieldType, false))
                .append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        var code = new StringBuilder();

        code.append(apply(putFieldInstruction.getObject()));

        code.append(apply(putFieldInstruction.getValue()));

        var fieldOperand = putFieldInstruction.getField();
        var fieldName = ((Operand) fieldOperand).getName();
        var fieldType = putFieldInstruction.getFieldType();
        var objectType = putFieldInstruction.getObject().getType();

        String className;
        if (objectType.toString().startsWith("OBJECTREF(")) {
            int startIndex = objectType.toString().indexOf('(') + 1;
            int endIndex = objectType.toString().indexOf(')');
            className = objectType.toString().substring(startIndex, endIndex);
        } else {
            className = currentMethod.getOllirClass().getClassName();
        }

        code.append("putfield ")
                .append(className.replace('.', '/'))
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(types.toJasminType(fieldType, false))
                .append(NL);

        currStackSize -= 2;

        return code.toString();
    }

    private String generateInvokeVirtual(InvokeVirtualInstruction invokeVirtualInstruction) {
        var code = new StringBuilder();
        currStackSize -= 1;
        code.append(apply( invokeVirtualInstruction.getCaller()));
        var params = "";
        for (int i = 0; i < invokeVirtualInstruction.getArguments().size(); i++) {
            var argument = invokeVirtualInstruction.getArguments().get(i);
            params += types.toJasminType(argument.getType(),false);
            code.append(apply(argument));
        }
        var name = ((Operand) invokeVirtualInstruction.getCaller()).getName();
        var className =  invokeVirtualInstruction.getCaller().getType().toString();
        code.append("invokevirtual " + types.getImportPath(name,currentMethod,className) + "/" +
                ((LiteralElement) invokeVirtualInstruction.getMethodName()).getLiteral()
                + "(" + params + ")" + types.toJasminType(invokeVirtualInstruction.getReturnType(),false) + NL);
        return code.toString();
    }

    private String generateInvokeSpecial(InvokeSpecialInstruction invokeSpecialInstruction) {
        var code = new StringBuilder();

        code.append(apply(invokeSpecialInstruction.getCaller()));

        var params = "";
        for (int i = 0; i < invokeSpecialInstruction.getArguments().size(); i++) {
            var argument = invokeSpecialInstruction.getArguments().get(i);
            params += types.toJasminType(argument.getType(), false);
            code.append(apply(argument));
        }

        var methodName = ((LiteralElement) invokeSpecialInstruction.getMethodName()).getLiteral();
        var callerName = ((Operand) invokeSpecialInstruction.getCaller()).getName();
        var className = invokeSpecialInstruction.getCaller().getType().toString();
        var returnType = types.toJasminType(invokeSpecialInstruction.getReturnType(), false);

        code.append("invokespecial ").append(types.getImportPath(callerName, currentMethod, className))
                .append("/").append(methodName)
                .append("(").append(params).append(")").append(returnType).append(NL);

        currStackSize -= (1 + invokeSpecialInstruction.getArguments().size());

        if (!invokeSpecialInstruction.getReturnType().toString().equals("VOID")) {
            currStackSize += 1;
        }

        return code.toString();
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
            var className = types.toJasminType(caller.getType(), true);

            code.append("new ").append(className).append(NL);
            currStackSize += 1;
            if (currStackSize > maxStackSize) maxStackSize = currStackSize;
        }

        return code.toString();
    }

    private String generateInvokeStatic(InvokeStaticInstruction invokeStaticInstruction) {
        var code = new StringBuilder();
        currStackSize -= 1;
        var params = "";
        for (int i = 0; i < invokeStaticInstruction.getArguments().size(); i++) {
            var argument = invokeStaticInstruction.getArguments().get(i);
            params += types.toJasminType(argument.getType(),false);
            code.append(apply(argument));
        }
        var name = ((Operand) invokeStaticInstruction.getCaller()).getName();
        var className = invokeStaticInstruction.getCaller().getType().toString();
        code.append("invokestatic " + types.getImportPath(name,currentMethod, className) + "/" +
                ((LiteralElement) invokeStaticInstruction.getMethodName()).getLiteral()
                + "(" + params + ")" + types.toJasminType(invokeStaticInstruction.getReturnType(),false) + NL);
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
        currStackSize -= 1;

        code.append("ifne ").append(label).append(NL);

        return code.toString();
    }

    private String apply(TreeNode node) {
        var code = new StringBuilder();

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
        currStackSize += 1;
        if (currStackSize > maxStackSize) maxStackSize = currStackSize;

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            if (method.isConstructMethod()) {
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }

    private String generateMethod(Method method) {
        currStackSize = 0;
        maxStackSize = 0;

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
            params += types.toJasminType(param.getType(), false);
        }

        var returnType = types.toJasminType(
                ((ReturnInstruction) method.getInstr(method.getInstructions().size() - 1)).getReturnType(),
                false
        );

        code.append("\n.method ").append(modifier).append(isStatic)
                .append(methodName)
                .append("(").append(params).append(")").append(returnType).append(NL);

        int maxlocals = 0;
        for (var variable : method.getVarTable().keySet()) {
            var descriptor = method.getVarTable().get(variable);
            if (descriptor != null) {
                if (descriptor.getVirtualReg() + 1 > maxlocals) {
                    maxlocals = descriptor.getVirtualReg() + 1;
                }
            }
        }

        StringBuilder methodBody = new StringBuilder();

        for (var inst : method.getInstructions()) {
            var instLabels = method.getLabels(inst);
            for (var lbl : instLabels) {
                methodBody.append(lbl).append(":").append(NL);
            }

            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));
            methodBody.append(instCode);
        }

        methodBody.append(".end method").append(NL);

        code.append(TAB).append(".limit stack ").append(Math.max(maxStackSize, 4)).append(NL);
        code.append(TAB).append(".limit locals ").append(maxlocals).append(NL);
        code.append(methodBody.toString());

        // unset method
        currentMethod = null;
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // checks if it is an array element assignment
        var dest = assign.getDest();
        if (dest instanceof ArrayOperand) {
            var arrayOperand = (ArrayOperand) dest;

            var arrayName = arrayOperand.getName();
            var arrayVarDescriptor = currentMethod.getVarTable().get(arrayName);
            var arrayType = arrayVarDescriptor.getVarType();

            var arrayRef = new Operand(arrayName, arrayType);
            code.append(apply(arrayRef));

            code.append(apply(arrayOperand.getIndexOperands().get(0)));

            code.append(apply(assign.getRhs()));

            code.append("iastore").append(NL);
            currStackSize -= 3; // array ref + index + value

            return code.toString();
        }

        // checks if it is iinc pattern
        if (assign.getRhs() instanceof SingleOpInstruction singleOp &&
                singleOp.getSingleOperand() instanceof Operand tempVar) {

            var instructions = currentMethod.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                if (instructions.get(i) == assign && i > 0) {
                    var prevInst = instructions.get(i - 1);
                    if (prevInst instanceof AssignInstruction prevAssign &&
                            prevAssign.getDest() instanceof Operand prevDest &&
                            prevDest.getName().equals(tempVar.getName()) &&
                            prevAssign.getRhs() instanceof BinaryOpInstruction binOp &&
                            binOp.getOperation().getOpType() == OperationType.ADD) {

                        var leftOperand = binOp.getLeftOperand();
                        var rightOperand = binOp.getRightOperand();
                        var currentVar = (Operand) assign.getDest();

                        boolean leftIsCurrentVar = (leftOperand instanceof Operand leftOp) &&
                                leftOp.getName().equals(currentVar.getName());
                        boolean rightIsCurrentVar = (rightOperand instanceof Operand rightOp) &&
                                rightOp.getName().equals(currentVar.getName());

                        if ((leftIsCurrentVar && rightOperand instanceof LiteralElement) ||
                                (rightIsCurrentVar && leftOperand instanceof LiteralElement)) {

                            var literalElement = leftIsCurrentVar ? (LiteralElement) rightOperand : (LiteralElement) leftOperand;

                            try {
                                int increment = Integer.parseInt(literalElement.getLiteral());
                                if (increment >= -128 && increment <= 127) {
                                    var reg = currentMethod.getVarTable().get(currentVar.getName()).getVirtualReg();
                                    code.append("iinc ").append(reg).append(" ").append(increment).append(NL);
                                    return code.toString();
                                }
                            } catch (NumberFormatException e) {
                                // normal assignment
                            }
                        }
                    }
                    break;
                }
            }
        }

        code.append(apply(assign.getRhs()));

        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = operand.getType().toString();
        System.out.println("type: " + type);
        currStackSize -= 1;
        if (type.contains("[]") || type.contains("OBJECTREF")) {
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
        currStackSize += 1;
        if (currStackSize > maxStackSize) maxStackSize = currStackSize;
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

        currStackSize += 1;
        if (currStackSize > maxStackSize) maxStackSize = currStackSize;

        if (type.contains("[]") || type.contains("array") || type.contains("ARRAY") || type.contains("OBJECTREF")) {
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
        var opType = binaryOp.getOperation().getOpType();

        switch (opType) {
            case ADD -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("iadd").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            case MUL -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("imul").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            case SUB -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("isub").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            case DIV -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("idiv").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            case LTH -> {
                return generateLesserOp(binaryOp);
            }
            case GTH -> {
                var code = new StringBuilder();
                var left = binaryOp.getLeftOperand();
                var right = binaryOp.getRightOperand();

                int idT = counter++;
                int idE = counter++;
                String lblT = "cmpTrue" + idT;
                String lblE = "cmpEnd" + idE;

                code.append(apply(left));
                code.append(apply(right));

                if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
                    code.append("ifgt ").append(lblT).append(NL);
                    currStackSize -= 1;
                } else {
                    code.append("if_icmpgt ").append(lblT).append(NL);
                    currStackSize -= 2;
                }

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);

                return code.toString();
            }
            case LTE -> {
                var code = new StringBuilder();
                var left = binaryOp.getLeftOperand();
                var right = binaryOp.getRightOperand();

                int idT = counter++;
                int idE = counter++;
                String lblT = "cmpTrue" + idT;
                String lblE = "cmpEnd" + idE;

                code.append(apply(left));
                code.append(apply(right));

                if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
                    code.append("ifle ").append(lblT).append(NL);
                    currStackSize -= 1;
                } else {
                    code.append("if_icmple ").append(lblT).append(NL);
                    currStackSize -= 2;
                }

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);

                return code.toString();
            }
            case GTE -> {
                var code = new StringBuilder();
                var left = binaryOp.getLeftOperand();
                var right = binaryOp.getRightOperand();

                int idT = counter++;
                int idE = counter++;
                String lblT = "cmpTrue" + idT;
                String lblE = "cmpEnd" + idE;

                code.append(apply(left));
                code.append(apply(right));

                if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
                    code.append("ifge ").append(lblT).append(NL);
                    currStackSize -= 1;
                } else {
                    code.append("if_icmpge ").append(lblT).append(NL);
                    currStackSize -= 2;
                }

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);

                return code.toString();
            }
            case EQ -> {
                var code = new StringBuilder();
                var left = binaryOp.getLeftOperand();
                var right = binaryOp.getRightOperand();

                int idT = counter++;
                int idE = counter++;
                String lblT = "cmpTrue" + idT;
                String lblE = "cmpEnd" + idE;

                code.append(apply(left));
                code.append(apply(right));

                if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
                    code.append("ifeq ").append(lblT).append(NL);
                    currStackSize -= 1;
                } else {
                    code.append("if_icmpeq ").append(lblT).append(NL);
                    currStackSize -= 2;
                }

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);

                return code.toString();
            }
            case NEQ -> {
                var code = new StringBuilder();
                var left = binaryOp.getLeftOperand();
                var right = binaryOp.getRightOperand();

                int idT = counter++;
                int idE = counter++;
                String lblT = "cmpTrue" + idT;
                String lblE = "cmpEnd" + idE;

                code.append(apply(left));
                code.append(apply(right));

                if (right instanceof LiteralElement lit && "0".equals(lit.getLiteral())) {
                    code.append("ifne ").append(lblT).append(NL);
                    currStackSize -= 1;
                } else {
                    code.append("if_icmpne ").append(lblT).append(NL);
                    currStackSize -= 2;
                }

                code.append("iconst_0").append(NL);
                code.append("goto ").append(lblE).append(NL);

                code.append(lblT).append(":").append(NL);
                code.append("iconst_1").append(NL);
                currStackSize += 1;

                code.append(lblE).append(":").append(NL);

                return code.toString();
            }
            case ANDB -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("iand").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            case ORB -> {
                var code = new StringBuilder();
                code.append(apply(binaryOp.getLeftOperand()));
                code.append(apply(binaryOp.getRightOperand()));
                code.append("ior").append(NL);
                currStackSize -= 1;
                return code.toString();
            }
            default -> throw new NotImplementedException(opType);
        }
    }

    private String generateLesserOp(BinaryOpInstruction binOp) {
        currStackSize += 1;
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
            currStackSize -= 1;
        } else {
            code.append("if_icmplt ").append(lblT).append(NL);
            currStackSize -= 2;
        }

        code.append("iconst_0").append(NL);
        code.append("goto ").append(lblE).append(NL);

        code.append(lblT).append(":").append(NL);
        code.append("iconst_1").append(NL);
        currStackSize += 1;

        code.append(lblE).append(":").append(NL);

        return code.toString();
    }


    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if (returnInst.hasReturnValue()) {
            code.append(apply(returnInst.getOperand().get()));
        }

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
        currStackSize = 0;
        return code.toString();
    }
}
