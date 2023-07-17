package dev.skidfuscator.ir.method.impl;

import dev.skidfuscator.ir.FunctionNode;
import dev.skidfuscator.ir.hierarchy.Hierarchy;
import dev.skidfuscator.ir.insn.Insn;
import dev.skidfuscator.ir.insn.impl.IntInsn;
import dev.skidfuscator.ir.insn.impl.InvokeInsn;
import dev.skidfuscator.ir.insn.impl.LdcInsn;
import dev.skidfuscator.ir.insn.impl.TypeInsn;
import dev.skidfuscator.ir.klass.KlassNode;
import dev.skidfuscator.ir.method.FunctionGroup;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ResolvedFunctionNode implements FunctionNode {
    private final MethodNode node;
    private final Hierarchy hierarchy;

    private KlassNode parent;
    private FunctionGroup group;

    private List<Insn> instructions;
    private int access;

    public ResolvedFunctionNode(MethodNode node, Hierarchy hierarchy) {
        this.node = node;
        this.hierarchy = hierarchy;
    }

    @Override
    public void dump() {

    }

    @Override
    public FunctionGroup getGroup() {
        return group;
    }

    @Override
    public void setGroup(FunctionGroup group) {
        if (this.group != null && group != null) {
            throw new IllegalStateException(String.format(
                    "Conflict! Groups intersected: %s v %s",
                    this.getName() + this.getDesc(),
                    group.getName() + group.getDesc()
            ));
        }
    }

    @Override
    public void resolve() {
        this.access = node.access;
        this.instructions = new ArrayList<>();

        for (AbstractInsnNode instruction : this.node.instructions) {
            final Insn insn;
            if (instruction instanceof MethodInsnNode) {
                insn = new InvokeInsn(
                        hierarchy,
                        (MethodInsnNode) instruction
                );
            }

            else if (instruction instanceof LdcInsnNode) {
                insn = new LdcInsn(
                        hierarchy,
                        (LdcInsnNode) instruction
                );
            }

            else if (instruction instanceof TypeInsnNode) {
                insn = new TypeInsn(
                        hierarchy,
                        (TypeInsnNode) instruction
                );
            }

            else if (instruction instanceof IntInsnNode) {
                insn = new IntInsn(
                        hierarchy,
                        (IntInsnNode) instruction
                );
            }

            this.instructions.add(insn);
        }

        this.instructions.forEach(Insn::resolve);

        FunctionGroup group = null;

        /*
         * Only non-static methods need to resolve
         * groups. Static methods may simply just
         * create their groups without worrying
         * about conflicts.
         */
        if (!isStatic()) {
            final Stack<KlassNode> stack = new Stack<>();
            stack.add(parent.getParent());
            stack.addAll(parent.getInterfaces());

            while (!stack.isEmpty()) {
                final KlassNode klass = stack.pop();

                final FunctionNode similar = hierarchy.findMethod(
                        klass.getName(),
                        this.node.name,
                        this.node.desc
                );

                if (similar == null) {
                    stack.add(klass.getParent());
                    stack.addAll(klass.getInterfaces());
                    continue;
                }

                if (similar.getGroup() == null) {
                    throw new IllegalStateException(String.format(
                            "Parent of klass %s has similar method but method group is unresolved! " +
                                    "This means that the class path has not been resolved from tree root. Failing."
                            , klass.getName()));
                }

                if (group != null && !similar.getGroup().equals(group)) {
                    throw new IllegalStateException(String.format(
                            "Parent of klass %s has similar method but method group is conflictual! " +
                                    "This means that the class path has not been resolved from tree root. Failing."
                            , klass.getName()));
                }

                group = similar.getGroup();
            }
        }


        this.group = group == null
                ? new FunctionGroup(this.node.name, this.node.desc)
                : group;
    }

    @Override
    public List<Insn> getInstructions() {
        return instructions;
    }

    @Override
    public KlassNode getParent() {
        return parent;
    }

    @Override
    public void setParent(KlassNode node) {
        this.parent = node;
    }

    @Override
    public String getName() {
        return this.group.getName();
    }

    @Override
    public void setName(final String name) {
        this.group.setName(name);
    }

    @Override
    public String getDesc() {
        return this.group.getDesc();
    }

    @Override
    public boolean isStatic() {
        return (this.access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public boolean isConstructor() {
        return this.getName().equals("<init>");
    }
}