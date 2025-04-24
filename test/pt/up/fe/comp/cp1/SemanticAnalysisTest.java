package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class SemanticAnalysisTest {

    @Test
    public void symbolTable() {

        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/SymbolTable.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varNotDeclared() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarNotDeclared.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void classNotImported() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ClassNotImported.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void intPlusObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntPlusObject.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void boolTimesInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/BoolTimesInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayPlusInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayPlusInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayAccessOnInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessOnInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayIndexNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayIndexNotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void assignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssignIntToBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentFail.jmm"));
        //System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void objectAssignmentPassExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void objectAssignmentPassImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ObjectAssignmentPassImports.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intInIfCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntInIfCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayInWhileCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInWhileCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToUndeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToUndeclaredMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void callToMethodAssumedInExtends() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void callToMethodAssumedInImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallToMethodAssumedInImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void incompatibleArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleArguments.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void incompatibleReturn() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleReturn.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void assumeArguments() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssumeArguments.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargs() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/Varargs.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsWrong() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsWrong.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInit() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInit.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitWrong1() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong1.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void arrayInitWrong2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitWrong2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }


    // SECTION 3.3.1: Types and Declarations Verification

    @Test
    public void testUndeclaredIdentifier() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/UndeclaredIdentifier.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testUndeclaredField() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/UndeclaredField.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testIncompatibleOperandTypes() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleOperandTypes.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testIntPlusBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntPlusBool.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testArrayInArithmeticOperation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInArithmeticOperation.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testArrayAccessOnNonArray() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessOnNonArray.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testArrayAccessWithNonIntIndex() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayAccessWithNonIntIndex.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testIncompatibleAssignment() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IncompatibleAssignment.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testNonBooleanCondition() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/NonBooleanCondition.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testThisInStaticMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisInStaticMethod.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testThisAsObject() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisAsObject.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testThisAsObjectExtends() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisAsObjectExtends.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testThisAsObjectIncompatible() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ThisAsObjectIncompatible.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsNotLastParameter() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsNotLastParameter.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testMultipleVarargsParameters() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MultipleVarargsParameters.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsInFieldDeclaration() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsInFieldDeclaration.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsInMethodReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsInMethodReturn.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsInVariable() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsInVariable.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testArrayInitializerAssignment() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitializerAssignment.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testArrayInitializerInMethodArg() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitializerInMethodArg.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testArrayInitializerInReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ArrayInitializerInReturn.jmm"));
        TestUtils.noErrors(result);
    }

    // SECTION 3.3.2: Method Verification

    @Test
    public void testMethodCallIncompatibleArgumentTypes() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MethodCallIncompatibleArgumentTypes.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testMethodCallWrongNumberOfArguments() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MethodCallWrongNumberOfArguments.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testVarargsMethodCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsMethodCall.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testVarargsMethodCallWithArray() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarargsMethodCallWithArray.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testMethodInSuperClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MethodInSuperClass.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testMethodNotInClassOrSuper() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/MethodNotInClassOrSuper.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testImportedClassMethodCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ImportedClassMethodCall.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testNonImportedClassMethodCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/NonImportedClassMethodCall.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void testCompositeExpressionWithImportedClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CompositeExpressionWithImportedClass.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void testAssignBoolNotInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/AssignBoolNotInt.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void fieldNamedMain() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/FieldNamedMain.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void negateBoolAssignBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/NegateBoolAssignBool.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varNamedLength() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VarNamedLength.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void boolVarCorrect() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/BoolVarCorrect.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void ifConditionCorrect() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IfConditionCorrect.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void whileConditionCorrect() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/WhileConditionCorrect.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void duplicateImports() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/DuplicateImports.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void duplicateVarName() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/DuplicateVarName.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void duplicateMethodSignature() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/DuplicateMethodSignature.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void duplicateParams() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/DuplicateParams.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void lengthCall() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/CallLength.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void lengthInExpression() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LengthInExpression.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void StatementsAfterReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/StatementsAfterReturn.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void ValidClass() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/ValidClass.jmm"));
        TestUtils.noErrors(result);
    }
    @Test
    public void voidWithReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/VoidWithReturn.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void intWithoutReturn() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/IntWithoutReturn.jmm"));
        TestUtils.mustFail(result);
    }
    @Test
    public void LengthWrong() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/LengthWrong.jmm"));
        TestUtils.mustFail(result);
    }

}
