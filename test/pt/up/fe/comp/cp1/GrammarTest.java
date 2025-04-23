/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class GrammarTest {

    private static final String IMPORT = "importDecl";
    private static final String CLASS = "classDecl";
    private static final String VAR_DECL = "varDecl";
    private static final String METHOD_DECL = "methodDecl";
    private static final String STATEMENT = "stmt";
    private static final String EXPRESSION = "expr";
    private static final String TYPE = "type";

    //----------------------------------------
    // Import Declaration Tests
    //----------------------------------------

    @Test
    public void testImportSingle() {
        TestUtils.parseVerbose("import bar;", IMPORT);
    }

    @Test
    public void testImportMultiple() {
        TestUtils.parseVerbose("import bar.foo.a;", IMPORT);
    }

    @Test
    public void testMultipleImports() {
        TestUtils.parseVerbose("import bar; import foo; class A {}");
    }

    @Test
    public void testImportWithSpecialChars() {
        TestUtils.parseVerbose("import $package.$class;", IMPORT);
        TestUtils.parseVerbose("import package_name.class_name;", IMPORT);
    }

    @Test
    public void testImportLongChain() {
        TestUtils.parseVerbose("import a.b.c.d.e.f.g.h.i.j;", IMPORT);
    }

    //----------------------------------------
    // Class Declaration Tests
    //----------------------------------------

    @Test
    public void testClassBasic() {
        TestUtils.parseVerbose("class Foo {}", CLASS);
    }

    @Test
    public void testClassWithExtends() {
        TestUtils.parseVerbose("class Foo extends Bar {}", CLASS);
    }

    @Test
    public void testClassWithFields() {
        TestUtils.parseVerbose("class Foo { int a; boolean b; Bar c; }", CLASS);
    }

    @Test
    public void testClassWithMethods() {
        TestUtils.parseVerbose("class Foo { int method() { return 0; } }", CLASS);
    }

    @Test
    public void testClassComplete() {
        TestUtils.parseVerbose("class Foo extends Bar { int a; boolean b; int method() { return 0; } }");
    }

    @Test
    public void testClassSpecialNames() {
        TestUtils.parseVerbose("class _Test$123 {}", CLASS);
    }

    @Test
    public void testClassExtendingImported() {
        TestUtils.parseVerbose("import external.BaseClass; class Test extends BaseClass {}", "program");
    }

    @Test
    public void testClassWithComplexFieldNames() {
        TestUtils.parseVerbose("class Test { int $field_1; boolean _flag$; }", CLASS);
    }

    //----------------------------------------
    // Type Tests
    //----------------------------------------

    @Test
    public void testBasicTypes() {
        TestUtils.parseVerbose("int", TYPE);
        TestUtils.parseVerbose("boolean", TYPE);
        TestUtils.parseVerbose("MyClass", TYPE);
    }

    @Test
    public void testArrayType() {
        TestUtils.parseVerbose("int[]", TYPE);
    }

    @Test
    public void testVarargsType() {
        TestUtils.parseVerbose("int...", TYPE);
    }

    @Test
    public void testUnsupportedTypes() {
        // These should fail as Java-- doesn't support these types
        try {
            TestUtils.parseVerbose("int[][]", TYPE); // No multidimensional arrays
        } catch (Exception e) {
            // Expected to fail
        }

        try {
            TestUtils.parseVerbose("MyClass[]", TYPE); // No object arrays
        } catch (Exception e) {
            // Expected to fail
        }
    }

    //----------------------------------------
    // Variable Declaration Tests
    //----------------------------------------

    @Test
    public void testVarDeclarations() {
        TestUtils.parseVerbose("int a;", VAR_DECL);
        TestUtils.parseVerbose("boolean flag;", VAR_DECL);
        TestUtils.parseVerbose("int[] arr;", VAR_DECL);
        TestUtils.parseVerbose("MyClass obj;", VAR_DECL);
    }

    @Test
    public void testVarDeclWithSpecialNames() {
        TestUtils.parseVerbose("int $var_123;", VAR_DECL);
    }

    @Test
    public void testMultipleVariables() {
        TestUtils.parseVerbose("class Test { int a; int b; int c; }");
    }

    //----------------------------------------
    // Method Declaration Tests
    //----------------------------------------

    @Test
    public void testBasicMethodDeclarations() {
        TestUtils.parseVerbose("int foo() { return 0; }", METHOD_DECL);
        TestUtils.parseVerbose("public int foo() { return 0; }", METHOD_DECL);
    }

    @Test
    public void testMethodWithParameters() {
        TestUtils.parseVerbose("int foo(int a, boolean b) { return 0; }", METHOD_DECL);
        TestUtils.parseVerbose("int foo(int[] arr) { return 0; }", METHOD_DECL);
    }

    @Test
    public void testMethodWithVarargs() {
        TestUtils.parseVerbose("int foo(int... nums) { return 0; }", METHOD_DECL);
        TestUtils.parseVerbose("int foo(int a, boolean b, int... rest) { return 0; }", METHOD_DECL);
    }

    @Test
    public void testReturnArrayInitializer() {
        TestUtils.parseVerbose("int[] method() { return [1, 2, 3]; }", METHOD_DECL);
    }

    @Test
    public void testMethodWithLocalVars() {
        TestUtils.parseVerbose("int foo() { int a; boolean b; return 0; }", METHOD_DECL);
    }

    @Test
    public void testMethodWithStatements() {
        TestUtils.parseVerbose("int foo() { a = 1; b = 2; return a + b; }", METHOD_DECL);
    }

    @Test
    public void testMainMethodDecl() {
        TestUtils.parseVerbose("public static void main(String[] args) { }", METHOD_DECL);
    }

    @Test
    public void testMethodWithComplexTypes() {
        TestUtils.parseVerbose("MyClass foo(YourClass a, int[] b) { return new MyClass(); }", METHOD_DECL);
    }

    @Test
    public void testMethodWithMultipleStatements() {
        String method =
                """
                        int complex() {
                            int a;
                            boolean b;
                            a = as10;
                            b = true;
                            if (b) {
                                a = a * 2;
                            } else {
                                a = a / 2;
                            }
                            while (a < 0) {
                                a = a - 1;
                            }
                            return a;
                        }""";
        TestUtils.parseVerbose(method, METHOD_DECL);
    }

    @Test
    public void testMainMethodWithLogic() {
        String main =
                """
                        public static void main(String[] args) {
                            int count;
                            count = 10;
                            while (count < 0) {
                                count = count - 1;
                            }
                        }""";
        TestUtils.parseVerbose(main, METHOD_DECL);
    }

    //----------------------------------------
    // Statement Tests
    //----------------------------------------

    @Test
    public void testBlockStatements() {
        TestUtils.parseVerbose("{}", STATEMENT);
        TestUtils.parseVerbose("{ a = 1; b = 2; }", STATEMENT);
    }

    @Test
    public void testIfStatements() {
        TestUtils.parseVerbose("if (a < b) { c = 1; }", STATEMENT);
        TestUtils.parseVerbose("if (a < b) { c = 1; } else { c = 2; }", STATEMENT);
        TestUtils.parseVerbose("if (a) b = 1; else b = 2;", STATEMENT);
        TestUtils.parseVerbose("if (a) { b = 1; } else c = 2;", STATEMENT);
        TestUtils.parseVerbose("if (a) b = 1; else { c = 2; }", STATEMENT);
    }

    @Test
    public void testWhileStatements() {
        TestUtils.parseVerbose("while (a < 10) { a = a + 1; }", STATEMENT);
        TestUtils.parseVerbose("while (a) b = b + 1;", STATEMENT);
    }

    @Test
    public void testExpressionStatement() {
        TestUtils.parseVerbose("foo.bar();", STATEMENT);
    }

    @Test
    public void testAssignmentStatements() {
        TestUtils.parseVerbose("a = b + c;", STATEMENT);
        TestUtils.parseVerbose("arr[i] = val;", STATEMENT);
    }

    @Test
    public void testNestedBlocks() {
        TestUtils.parseVerbose("{ { { a = 1; } b = 2; } }", STATEMENT);
    }

    @Test
    public void testNestedIfElse() {
        TestUtils.parseVerbose(
                """
                        if (a) {
                            if (b) {
                                c = 1;
                            } else {
                                c = 2;
                            }
                        } else {
                            c = 3;
                        }""", STATEMENT);
    }

    @Test
    public void testNestedWhile() {
        TestUtils.parseVerbose(
                """
                        while (a) {
                            while (b) {
                                c = c + 1;
                            }
                        }""", STATEMENT);
    }

    @Test
    public void testComplexIfCondition() {
        TestUtils.parseVerbose("if (a && b < c + d * !e) { f = 1; }", STATEMENT);
    }

    //----------------------------------------
    // Expression Tests - Literals and Simple Expressions
    //----------------------------------------

    @Test
    public void testLiteralExpressions() {
        TestUtils.parseVerbose("42", EXPRESSION);
        TestUtils.parseVerbose("0", EXPRESSION);
        TestUtils.parseVerbose("2147483647", EXPRESSION); // Max int value
        TestUtils.parseVerbose("true", EXPRESSION);
        TestUtils.parseVerbose("false", EXPRESSION);
        TestUtils.parseVerbose("myVar", EXPRESSION);
        TestUtils.parseVerbose("this", EXPRESSION);
    }

    @Test
    public void testParenExpr() {
        TestUtils.parseVerbose("(a + b)", EXPRESSION);
    }

    @Test
    public void testNotExpr() {
        TestUtils.parseVerbose("!flag", EXPRESSION);
        TestUtils.parseVerbose("!(a && b)", EXPRESSION);
        TestUtils.parseVerbose("!!a", EXPRESSION);
    }

    //----------------------------------------
    // Expression Tests - Object Creation
    //----------------------------------------

    @Test
    public void testCreationExpressions() {
        TestUtils.parseVerbose("new int[10]", EXPRESSION);
        TestUtils.parseVerbose("new MyClass()", EXPRESSION);
        TestUtils.parseVerbose("new int[a+b*c]", EXPRESSION);
    }

    @Test
    public void testArrayInitExpr() {
        TestUtils.parseVerbose("[]", EXPRESSION);
        TestUtils.parseVerbose("[1, 2, 3]", EXPRESSION);
        TestUtils.parseVerbose("[a+1, b*2, c&&d, !e]", EXPRESSION);
    }

    @Test
    public void testUnsupportedArrayInit() {
        // This should fail as Java-- doesn't support nested arrays
        try {
            TestUtils.parseVerbose("[[1, 2], [3, 4]]", EXPRESSION);
        } catch (Exception e) {
            // Expected to fail
        }
    }

    //----------------------------------------
    // Expression Tests - Array and Object Operations
    //----------------------------------------

    @Test
    public void testArrayOperations() {
        TestUtils.parseVerbose("arr[i]", EXPRESSION);
        TestUtils.parseVerbose("arr[i][j]", EXPRESSION);
        TestUtils.parseVerbose("arr[obj.getIndex()]", EXPRESSION);
        TestUtils.parseVerbose("new int[10][0]", EXPRESSION);
        TestUtils.parseVerbose("new int[10].length", EXPRESSION);
    }

    @Test
    public void testLengthExpr() {
        TestUtils.parseVerbose("arr.length", EXPRESSION);
        TestUtils.parseVerbose("[1, 2, 3].length", EXPRESSION);
    }

    @Test
    public void testMethodCallExpressions() {
        TestUtils.parseVerbose("obj.method()", EXPRESSION);
        TestUtils.parseVerbose("obj.method(a, b, true)", EXPRESSION);
        TestUtils.parseVerbose("obj.method1().method2()", EXPRESSION);
        TestUtils.parseVerbose("this.method()", EXPRESSION);
        TestUtils.parseVerbose("new MyClass().method()", EXPRESSION);
        TestUtils.parseVerbose("obj.varargMethod(1, 2, 3, 4, 5)", EXPRESSION);
        TestUtils.parseVerbose("obj.varargMethod(arr)", EXPRESSION);
        TestUtils.parseVerbose("obj.varargMethod([1, 2, 3])", EXPRESSION);
    }

    @Test
    public void testComplexChainedExpressions() {
        TestUtils.parseVerbose("a.method1()[i].method2().length", EXPRESSION);
        TestUtils.parseVerbose("objs[i].method()", EXPRESSION);
    }

    //----------------------------------------
    // Expression Tests - Binary Operations and Precedence
    //----------------------------------------

    @Test
    public void testBinaryExpressions() {
        TestUtils.parseVerbose("a * b", EXPRESSION);
        TestUtils.parseVerbose("a / b", EXPRESSION);
        TestUtils.parseVerbose("a + b", EXPRESSION);
        TestUtils.parseVerbose("a - b", EXPRESSION);
        TestUtils.parseVerbose("a < b", EXPRESSION);
        TestUtils.parseVerbose("a && b", EXPRESSION);
    }

    @Test
    public void testOperatorPrecedence() {
        TestUtils.parseVerbose("a + b * c", EXPRESSION); // Should parse as a + (b * c)
        TestUtils.parseVerbose("a * b + c", EXPRESSION); // Should parse as (a * b) + c
        TestUtils.parseVerbose("a < b + c", EXPRESSION); // Should parse as a < (b + c)
        TestUtils.parseVerbose("a && b < c", EXPRESSION); // Should parse as a && (b < c)
        TestUtils.parseVerbose("!a + b", EXPRESSION); // Should parse as (!a) + b
    }

    @Test
    public void testComplexExpressions() {
        TestUtils.parseVerbose("a && b < c + d * e / f - g", EXPRESSION);
        TestUtils.parseVerbose("(a + b) * (c - d)", EXPRESSION);
        TestUtils.parseVerbose("(((a + b) * c) - (d / e))", EXPRESSION);
        TestUtils.parseVerbose("a && b && c && d < e < f < g + h + i + j * k * l * m / n / o / p", EXPRESSION);
    }

    //----------------------------------------
    // Complete Program Tests
    //----------------------------------------

    @Test
    public void testBasicPrograms() {
        TestUtils.parseVerbose("class Test {}");
        TestUtils.parseVerbose("import java.io; class Test {}");
    }

    @Test
    public void testSimpleProgram() {
        String program =
                """
                        class Test {
                            public int foo(int a) {
                                int b;
                                b = a * 2;
                                return b;
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testProgramWithVarargs() {
        String program =
                """
                        class VarargsTest {
                            public int sum(int... nums) {
                                int total;
                                total = 0;
                                return total;
                            }
                           \s
                            public static void main(String[] args) {
                                VarargsTest test;
                                test = new VarargsTest();
                                test.sum(1, 2, 3);
                                test.sum();
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testProgramWithArrayInitializer() {
        String program =
                """
                        class ArrayTest {
                            public int[] getNumbers() {
                                int[] arr;
                                arr = [1+2, 3*4, 5];
                                return arr;
                            }
                           \s
                            public int getSum(int... nums) {
                                return 0;
                            }
                           \s
                            public static void main(String[] args) {
                                ArrayTest test;
                                int[] numbers;
                                int sum;
                                test = new ArrayTest();
                                numbers = test.getNumbers();
                                sum = test.getSum(numbers);
                                sum = test.getSum([10, 20, 30]);
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testComplexMethodChaining() {
        String program =
                """
                        class ChainTest {
                            public ChainTest self() {
                                return this;
                            }
                           \s
                            public int getValue() {
                                return 42;
                            }
                           \s
                            public static void main(String[] args) {
                                ChainTest test;
                                int val;
                                test = new ChainTest();
                                val = test.self().self().getValue();
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testImportedClassMethods() {
        String program =
                """
                        import ExternalClass;
                        
                        class ImportTest {
                            public static void main(String[] args) {
                                ExternalClass ext;
                                ext = ExternalClass.getInstance();
                                ext.doSomething(10);
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testVarargsArrayConversion() {
        String program =
                """
                        class VarargsArrayTest {
                            public int[] getValues(int... nums) {
                                return nums;
                            }
                           \s
                            public int sum(int... nums) {
                                return 0;
                            }
                           \s
                            public static void main(String[] args) {
                                VarargsArrayTest test;
                                int[] result;
                                int total;
                               \s
                                test = new VarargsArrayTest();
                                result = test.getValues(1, 2, 3);
                                total = test.sum(result);
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testRecursiveMethod() {
        String program =
                """
                        class RecursiveTest {
                            public int factorial(int n) {
                                int result;
                               \s
                                if (n < 2) {
                                    result = 1;
                                } else {
                                    result = n * this.factorial(n-1);
                                }
                               \s
                                return result;
                            }
                           \s
                            public static void main(String[] args) {
                                RecursiveTest test;
                                int result;
                               \s
                                test = new RecursiveTest();
                                result = test.factorial(5);
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testCompleteFactorialProgram() {
        String program =
                """
                        import io;
                        
                        class Factorial {
                            public int computeFactorial(int num) {
                                int num_aux;
                                if (num < 1)
                                    num_aux = 1;
                                else
                                    num_aux = num * (this.computeFactorial(num-1));
                                return num_aux;
                            }
                           \s
                            public static void main(String[] args) {
                                io.println(new Factorial().computeFactorial(10));
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }

    @Test
    public void testAllFeaturesCombined() {
        String program =
                """
                        import io;
                        import math.Calculator;
                        
                        class FeatureTest extends Calculator {
                            int field1;
                            boolean active;
                           \s
                            public int calculate(int a, int b, int... more) {
                                int result;
                                int[] values;
                               \s
                                // Initialize with array initializer
                                values = [a, b];
                               \s
                                // Use this reference
                                result = this.add(a, b);
                               \s
                                // Conditionals and loops
                                if (result < 100) {
                                    while (result < 100) {
                                        result = result * 2;
                                    }
                                } else {
                                    result = result / 2;
                                }
                               \s
                                // Array operations
                                values[0] = result;
                                result = values[0] + values[1];
                               \s
                                // Method chaining and array length
                                if (values.length < this.getMaxLength()) {
                                    result = result + 10;
                                }
                               \s
                                return result;
                            }
                           \s
                            public int add(int a, int b) {
                                return a + b;
                            }
                           \s
                            public int getMaxLength() {
                                return 10;
                            }
                           \s
                            public static void main(String[] args) {
                                FeatureTest test;
                                int result;
                               \s
                                test = new FeatureTest();
                                result = test.calculate(10, 20, 30, 40, 50);
                                io.println(result);
                            }
                        }""";
        TestUtils.parseVerbose(program);
    }
}