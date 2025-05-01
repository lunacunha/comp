package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

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

        //TODO: Do your AST-based optimizations here

        System.out.println("\nOptimizing AST..." + semanticsResult.getRootNode().getChildren());

        JmmNode rootNode = semanticsResult.getRootNode();

        // Criar o visitor de otimização
        AstOptimizationVisitor visitor = new AstOptimizationVisitor();

        // Aplicar otimizações em um loop até que não possam ser feitas mais melhorias
        boolean madeChanges;
        int iteration = 0;

        do {
            System.out.println("Optimization iteration: " + iteration);
            visitor.resetOptimized();
            visitor.visit(rootNode, false);
            madeChanges = visitor.hasOptimized();
            System.out.println("Made changes? " + madeChanges);
            iteration++;

            // Optional: prevent infinite loop in case of error
            if (iteration > 100) {
                System.out.println("Warning: exceeded 100 optimization iterations — possible infinite loop");
                break;
            }

        } while (madeChanges);

        System.out.println("Finished AST optimization after " + iteration + " iteration(s).");

        return new JmmSemanticsResult(
                rootNode,                            	// AST otimizada
                semanticsResult.getSymbolTable(),    	// mesma symbol table
                semanticsResult.getReports(),        	// mesmos reports
                semanticsResult.getConfig()          	// mesma config
        );

        //return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }


}
