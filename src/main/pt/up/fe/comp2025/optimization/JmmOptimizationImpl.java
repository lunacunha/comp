package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.InstructionType;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.Instruction;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2025.CompilerConfig;

import java.util.*;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        //System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        if (!CompilerConfig.getOptimize(semanticsResult.getConfig()))
            return semanticsResult;

        AstOptimizationVisitor visitor = new AstOptimizationVisitor();

        // fixed-point loop: enquanto otimizar algo, volta a correr o visitor
        do {
            visitor.resetOptimized();                            // limpa flag
            visitor.visit(semanticsResult.getRootNode(), false); // 1 passada de fold+prop
        } while (visitor.hasOptimized());

        return semanticsResult;
    }

    class Liveness {
        Set<String> use = new HashSet<>();
        Set<String> def = new HashSet<>();
        Set<String> in = new HashSet<>();
        Set<String> out = new HashSet<>();
    }

    public Map<String, Set<String>> removeNode(String node, Map<String, Set<String>> map) {
        if (map.containsKey(node)){
            map.remove(node);
            for (String key : map.keySet()){
                map.get(key).remove(node);
            }
        }
        return map;
    }
    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        if (CompilerConfig.getRegisterAllocation(ollirResult.getConfig()) == -1)
            return ollirResult;
        // CFG
        ollirResult.getOllirClass().buildCFGs();

        //Liveness
        Map<Instruction, Liveness> livenessMap = new HashMap<>();
        for (var method : ollirResult.getOllirClass().getMethods()) {
            for (var instruction : method.getInstructions()) {
                livenessMap.put(instruction, livenessVisit(instruction, method));
            }
        } // DEF USE
        boolean changed;
        do {
            changed = false;

            for (var method : ollirResult.getOllirClass().getMethods()) {
                List<Instruction> instructions = method.getInstructions();

                for (Instruction instr : instructions) {
                    Liveness info = livenessMap.get(instr);

                    Set<String> oldOut = new HashSet<>(info.out);

                    info.out.clear();
                    for (Instruction succ : instr.getSuccessorsAsInst()) {
                        Liveness succInfo = livenessMap.get(succ);
                        if (succInfo != null) {
                            info.out.addAll(succInfo.in);
                        }
                    }

                    Set<String> newIn = new HashSet<>(info.out);
                    newIn.removeAll(info.def);         // OUT - DEF
                    newIn.addAll(info.use);            // USE ∪ (OUT - DEF)

                    if (!info.in.equals(newIn)) {
                        info.in = newIn;
                        changed = true;
                    }

                    if (!info.out.equals(oldOut)) {
                        changed = true;
                    }

                }
            }
        } while (changed); // IN OUT

        //Interference Graph
        Map<String, Set<String>> interferenceGraph = new HashMap<>();
        for (var method : ollirResult.getOllirClass().getMethods()) {
            for (var instr : method.getInstructions()) {
                Liveness info = livenessMap.get(instr);

                for (String defVar : info.def) {
                    interferenceGraph.putIfAbsent(defVar, new HashSet<>());

                    for (String outVar : info.out) {
                        if (!defVar.equals(outVar)) {
                            interferenceGraph.get(defVar).add(outVar);
                            interferenceGraph.putIfAbsent(outVar, new HashSet<>());
                            interferenceGraph.get(outVar).add(defVar);
                        }
                    }
                }
            }
        }

        //Graph Coloring
        int k = 1;
        Stack<String> finalColoringOrder;

        while (true) {
            Map<String, Set<String>> tmpGraph = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : interferenceGraph.entrySet()) {
                tmpGraph.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }

            Stack<String> tmpStack = new Stack<>();
            boolean removed;

            do {
                removed = false;
                List<String> nodesToRemove = new ArrayList<>();

                for (String var : tmpGraph.keySet()) {
                    if (tmpGraph.get(var).size() < k) {
                        nodesToRemove.add(var);
                    }
                }

                for (String var : nodesToRemove) {
                    removeNode(var, tmpGraph);
                    tmpStack.push(var);
                    removed = true;
                }
            } while (removed && !tmpGraph.isEmpty());

            if (tmpGraph.isEmpty()) {
                finalColoringOrder = tmpStack;
                break;
            }

            k++;
        }
        Collections.reverse(finalColoringOrder);

        for (var method : ollirResult.getOllirClass().getMethods()) {
            var varTable = method.getVarTable();

            for (String varName : finalColoringOrder) {
                var descriptor = varTable.get(varName);


                if (descriptor != null) {
                    descriptor.setVirtualReg(0);
                    Set<String> neighborsNames = interferenceGraph.get(varName);
                    List<Integer> neighbors = new ArrayList<>();
                    for (String neighborName : neighborsNames) {
                        if (varTable.get(neighborName) != null) {
                            neighbors.add(varTable.get(neighborName).getVirtualReg());
                        }
                    }

                    int register = findAvailableRegister(neighbors,method.getParams().size()+1);


                    descriptor.setVirtualReg(register);
                }
            }
        }

        return ollirResult;
    }

    // Find the first available register starting from 'start' index, considering assigned and neighbor registers
    private int findAvailableRegister(List<Integer> neighbors, int start) {
        int registerIndex = start;

        while (neighbors.contains(registerIndex)) {
            registerIndex++;
        }

        return registerIndex;
    }


    public Liveness livenessVisit(Instruction instruction, Method method) {
        Liveness liveness = new Liveness();
        // DEF
        if (instruction.getInstType().equals(InstructionType.ASSIGN) &&
                !method.getParams().contains(((Operand) instruction.getChildren().get(0)).getName())) {
            liveness.def.add(((Operand) instruction.getChildren().get(0)).getName());
        }
        for (int i = 0; i < instruction.getDescendants().size(); i++) {
            if (instruction.getDescendants().get(i) instanceof Operand &&
                    !method.getParams().contains(((Operand) instruction.getChildren().get(0)).getName())) {
                if (!liveness.def.contains(((Operand) instruction.getDescendants().get(i)).getName())) {
                    liveness.use.add(((Operand) instruction.getDescendants().get(i)).getName());
                    liveness.in.add(((Operand) instruction.getDescendants().get(i)).getName());
                }
            }
        }
        return liveness;
    }

}
