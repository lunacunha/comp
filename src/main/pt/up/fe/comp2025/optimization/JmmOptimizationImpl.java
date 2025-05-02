package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2025.CompilerConfig;

import java.util.Collections;
import java.util.HashMap;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        if (!CompilerConfig.getOptimize(semanticsResult.getConfig())) return semanticsResult;

        AstOptimizationVisitor visitor = new AstOptimizationVisitor();

        System.out.println("=== constFoldSequence START ===");

        visitor.visit(semanticsResult.getRootNode(), false);

        System.out.println("AST antes do folding:\n" +
                semanticsResult.getRootNode().toTree()  // ou toString() se não tiver toTree()
        );

        boolean changed = visitor.visit(semanticsResult.getRootNode(), false);

        System.out.println("visitor.visit returned: " + changed);
        System.out.println("visitor.hasOptimized(): " + visitor.hasOptimized());

        // imprime a AST depois da dobra
        System.out.println("AST depois do folding:\n" +
                semanticsResult.getRootNode().toTree()
        );
        System.out.println("=== constFoldSequence END ===");

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }


}
