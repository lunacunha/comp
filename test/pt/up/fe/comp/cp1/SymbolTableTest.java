package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.specs.util.SpecsIo;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test variable lookup.
 */
public class SymbolTableTest {

    static JmmSemanticsResult getSemanticsResult(String filename) {
        return TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/" + filename));
    }

    static JmmSemanticsResult test(String filename, boolean fail) {
        var semantics = getSemanticsResult(filename);
        if (fail) {
            TestUtils.mustFail(semantics.getReports());
        } else {
            TestUtils.noErrors(semantics.getReports());
        }
        return semantics;
    }


    /**
     * Test if fields are not being accessed from static methods.
     */
    @Test
    public void NumImports() {
        var semantics = test("symboltable/Imports.jmm", false);
        assertEquals(2, semantics.getSymbolTable().getImports().size());

        System.out.println(semantics.getSymbolTable().toString());
    }

    @Test
    public void ClassAndSuper() {
        var semantics = test("symboltable/Super.jmm", false);
        assertEquals("Super", semantics.getSymbolTable().getClassName());
        assertEquals("UltraSuper", semantics.getSymbolTable().getSuper());

        System.out.println(semantics.getSymbolTable().toString());
    }

    @Test
    public void Fields() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var fields = semantics.getSymbolTable().getFields();
        assertEquals(3, fields.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;

        for (var f : fields) {
            switch (f.getType().getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    break;
                case "boolean":
                    checkBool++;
                    break;
                case "int":
                    checkInt++;
                    break;
            }
        }

        assertEquals("Field of type int", 1, checkInt);
        assertEquals("Field of type boolean", 1, checkBool);
        assertEquals("Field of type object", 1, checkObj);

        System.out.println(semantics.getSymbolTable().toString());
    }

    @Test
    public void Methods() {
        var semantics = test("symboltable/MethodsAndFields.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(5, methods.size());
        var checkInt = 0;
        var checkBool = 0;
        var checkObj = 0;
        var checkAll = 0;

        for (var m : methods) {
            var ret = st.getReturnType(m);
            var numParameters = st.getParameters(m).size();
            switch (ret.getName()) {
                case "MethodsAndFields":
                    checkObj++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "boolean":
                    checkBool++;
                    assertEquals("Method " + m + " parameters", 0, numParameters);
                    break;
                case "int":
                    if (ret.isArray()) {
                        checkAll++;
                        assertEquals("Method " + m + " parameters", 3, numParameters);
                    } else {
                        checkInt++;
                        assertEquals("Method " + m + " parameters", 0, numParameters);
                    }
                    break;

            }
        }

        assertEquals("Method with return type int", 1, checkInt);
        assertEquals("Method with return type boolean", 1, checkBool);
        assertEquals("Method with return type object", 1, checkObj);
        assertEquals("Method with three arguments", 1, checkAll);

        System.out.println(semantics.getSymbolTable().toString());
    }

    @Test
    public void Parameters() {
        var semantics = test("symboltable/Parameters.jmm", false);
        var st = semantics.getSymbolTable();
        var methods = st.getMethods();
        assertEquals(1, methods.size());

        var parameters = st.getParameters(methods.getFirst());
        assertEquals(3, parameters.size());
        assertEquals("Parameter 1", "int", parameters.get(0).getType().getName());
        assertEquals("Parameter 2", "boolean", parameters.get(1).getType().getName());
        assertEquals("Parameter 3", "Parameters", parameters.get(2).getType().getName());

        System.out.println(semantics.getSymbolTable().toString());
    }

    /**
     * Tests that imports are correctly added to the symbol table
     */
    @Test
    public void testImports() {
        var semantics = test("symboltable/ImportTest.jmm", false);
        var table = semantics.getSymbolTable();

        // Check number of imports
        assertEquals("Should have 3 imports", 3, table.getImports().size());

        // Check specific import names
        assertTrue("Should import io", table.getImports().contains("io"));
        assertTrue("Should import List", table.getImports().contains("List"));
        assertTrue("Should import MyClass", table.getImports().contains("MyClass"));

        System.out.println(table);
    }

    /**
     * Tests that class information is correctly added to the symbol table
     */
    @Test
    public void testClassInfo() {
        var semantics = test("symboltable/ClassInfoTest.jmm", false);
        var table = semantics.getSymbolTable();

        assertEquals("TestClass", table.getClassName());
        assertEquals("ParentClass", table.getSuper());

        System.out.println(table);
    }

    /**
     * Tests that fields are correctly added to the symbol table
     */
    @Test
    public void testFields() {
        var semantics = test("symboltable/FieldsTest.jmm", false);
        var table = semantics.getSymbolTable();

        assertEquals("Should have 4 fields", 4, table.getFields().size());

        List<Symbol> fields = table.getFields();

        verifyField(fields, "intField", "int", false);
        verifyField(fields, "boolField", "boolean", false);
        verifyField(fields, "objectField", "SomeClass", false);
        verifyField(fields, "intArrayField", "int", true);

        System.out.println(table);
    }

    /**
     * Tests that methods are correctly added to the symbol table
     */
    @Test
    public void testMethods() {
        var semantics = test("symboltable/MethodsTest.jmm", false);
        var table = semantics.getSymbolTable();

        assertEquals("Should have 5 methods (including main)", 5, table.getMethods().size());

        List<String> methods = table.getMethods();
        assertTrue(methods.contains("main"));
        assertTrue(methods.contains("intMethod"));
        assertTrue(methods.contains("boolMethod"));
        assertTrue(methods.contains("objectMethod"));
        assertTrue(methods.contains("arrayMethod"));

        verifyMethodReturnType(table, "intMethod", "int", false);
        verifyMethodReturnType(table, "boolMethod", "boolean", false);
        verifyMethodReturnType(table, "objectMethod", "MethodsTest", false);
        verifyMethodReturnType(table, "arrayMethod", "int", true);
        verifyMethodReturnType(table, "main", "void", false);

        System.out.println(table);
    }

    /**
     * Tests that method parameters are correctly added to the symbol table
     */
    @Test
    public void testParameters() {
        var semantics = test("symboltable/ParametersTest.jmm", false);
        var table = semantics.getSymbolTable();

        List<Symbol> params1 = table.getParameters("simpleMethod");
        assertEquals("simpleMethod should have 1 parameter", 1, params1.size());
        verifySymbol(params1.getFirst(), "param1", "int", false);

        List<Symbol> params2 = table.getParameters("mixedMethod");
        assertEquals("mixedMethod should have 3 parameters", 3, params2.size());
        verifySymbol(params2.get(0), "param1", "int", false);
        verifySymbol(params2.get(1), "param2", "boolean", false);
        verifySymbol(params2.get(2), "param3", "ParametersTest", false);

        List<Symbol> params3 = table.getParameters("arrayMethod");
        assertEquals("arrayMethod should have 2 parameters", 2, params3.size());
        verifySymbol(params3.get(0), "param1", "int", true);
        verifySymbol(params3.get(1), "param2", "boolean", false);

        System.out.println(table);
    }

    /**
     * Tests that varargs parameters are correctly added to the symbol table
     */
    @Test
    public void testVarArgsParameters() {
        var semantics = test("symboltable/VarArgsTest.jmm", false);
        var table = semantics.getSymbolTable();

        List<Symbol> params1 = table.getParameters("varArgsOnly");
        assertEquals("varArgsOnly should have 1 parameter", 1, params1.size());

        List<Symbol> params2 = table.getParameters("mixedParams");
        assertEquals("mixedParams should have 2 parameters", 2, params2.size());
        verifySymbol(params2.get(0), "start", "int", false);

        System.out.println(table);
    }

    /**
     * Tests that local variables are correctly added to the symbol table
     */
    @Test
    public void testLocalVariables() {
        var semantics = test("symboltable/LocalsTest.jmm", false);
        var table = semantics.getSymbolTable();

        List<Symbol> mainLocals = table.getLocalVariables("main");
        assertEquals("main should have 3 local variables", 3, mainLocals.size());
        verifySymbol(findSymbol(mainLocals, "intVar"), "intVar", "int", false);
        verifySymbol(findSymbol(mainLocals, "boolVar"), "boolVar", "boolean", false);
        verifySymbol(findSymbol(mainLocals, "objVar"), "objVar", "LocalsTest", false);

        List<Symbol> testLocals = table.getLocalVariables("testMethod");
        assertEquals("testMethod should have 2 local variables", 2, testLocals.size());
        verifySymbol(findSymbol(testLocals, "arrVar"), "arrVar", "int", true);
        verifySymbol(findSymbol(testLocals, "simpleVar"), "simpleVar", "int", false);

        System.out.println(table);
    }

    /**
     * Tests a complete program with multiple features to check proper table construction
     */
    @Test
    public void testCompleteProgram() {
        var semantics = test("symboltable/CompleteProgram.jmm", false);
        var table = semantics.getSymbolTable();

        assertEquals("Should have 2 imports", 2, table.getImports().size());
        assertTrue(table.getImports().contains("io"));
        assertTrue(table.getImports().contains("MathUtils"));

        assertEquals("CompleteProgram", table.getClassName());
        assertEquals("BaseProgram", table.getSuper());

        assertEquals("Should have 3 fields", 3, table.getFields().size());
        verifyField(table.getFields(), "counter", "int", false);
        verifyField(table.getFields(), "active", "boolean", false);
        verifyField(table.getFields(), "data", "int", true);

        assertEquals("Should have 4 methods", 4, table.getMethods().size());
        assertTrue(table.getMethods().contains("main"));
        assertTrue(table.getMethods().contains("calculate"));
        assertTrue(table.getMethods().contains("processArray"));
        assertTrue(table.getMethods().contains("sum"));

        verifyMethodReturnType(table, "main", "void", false);
        verifyMethodReturnType(table, "calculate", "int", false);
        verifyMethodReturnType(table, "processArray", "int", true);
        verifyMethodReturnType(table, "sum", "int", false);

        assertEquals(1, table.getParameters("main").size());
        assertEquals(2, table.getParameters("calculate").size());
        assertEquals(1, table.getParameters("processArray").size());
        assertEquals(1, table.getParameters("sum").size());

        verifySymbol(table.getParameters("sum").getFirst(), "numbers", "int...", true);

        assertTrue(!table.getLocalVariables("calculate").isEmpty());
        assertTrue(!table.getLocalVariables("main").isEmpty());

        System.out.println(table);
    }

    // Helper methods for verification

    private void verifyField(List<Symbol> fields, String name, String typeName, boolean isArray) {
        Symbol field = findSymbol(fields, name);
        assertNotNull("Field " + name + " should exist", field);
        verifySymbol(field, name, typeName, isArray);
    }

    private void verifyMethodReturnType(pt.up.fe.comp.jmm.analysis.table.SymbolTable table, String methodName, String typeName, boolean isArray) {
        Type returnType = table.getReturnType(methodName);
        assertNotNull("Return type for " + methodName + " should not be null", returnType);
        assertEquals("Return type name for " + methodName, typeName, returnType.getName());
        assertEquals("Return type array status for " + methodName, isArray, returnType.isArray());
    }

    private void verifySymbol(Symbol symbol, String name, String typeName, boolean isArray) {
        assertNotNull("Symbol should not be null", symbol);
        assertEquals("Symbol name", name, symbol.getName());
        assertEquals("Symbol type name", typeName, symbol.getType().getName());
        assertEquals("Symbol type array status", isArray, symbol.getType().isArray());
    }

    private Symbol findSymbol(List<Symbol> symbols, String name) {
        return symbols.stream()
                .filter(symbol -> symbol.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
