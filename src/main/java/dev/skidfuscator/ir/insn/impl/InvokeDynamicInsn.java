package dev.skidfuscator.ir.insn.impl;

import dev.skidfuscator.ir.hierarchy.Hierarchy;
import dev.skidfuscator.ir.insn.AbstractInsn;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public class InvokeDynamicInsn extends AbstractInsn<InvokeDynamicInsnNode> {
    public InvokeDynamicInsn(Hierarchy hierarchy, InvokeDynamicInsnNode node) {
        super(hierarchy, node);
        this.node = node;
    }

    @Override
    public String toString() {
        return "push(indy) " + node.name + node.desc;
    }
}
