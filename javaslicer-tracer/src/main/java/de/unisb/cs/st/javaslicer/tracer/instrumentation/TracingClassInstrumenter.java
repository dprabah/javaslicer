/** License information:
 *    Component: javaslicer-tracer
 *    Package:   de.unisb.cs.st.javaslicer.tracer.instrumentation
 *    Class:     TracingClassInstrumenter
 *    Filename:  javaslicer-tracer/src/main/java/de/unisb/cs/st/javaslicer/tracer/instrumentation/TracingClassInstrumenter.java
 *
 * This file is part of the JavaSlicer tool, developed by Clemens Hammacher at Saarland University.
 * See http://www.st.cs.uni-saarland.de/javaslicer/ for more information.
 *
 * JavaSlicer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSlicer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSlicer. If not, see <http://www.gnu.org/licenses/>.
 */
package de.unisb.cs.st.javaslicer.tracer.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import de.unisb.cs.st.javaslicer.common.classRepresentation.ReadClass;
import de.unisb.cs.st.javaslicer.common.classRepresentation.ReadMethod;
import de.unisb.cs.st.javaslicer.common.classRepresentation.instructions.AbstractInstruction;
import de.unisb.cs.st.javaslicer.tracer.ThreadTracer;
import de.unisb.cs.st.javaslicer.tracer.Tracer;
import de.unisb.cs.st.javaslicer.tracer.instrumentation.TracingMethodInstrumenter.FixedInstructionIterator;

public class TracingClassInstrumenter implements Opcodes {

    private final Tracer tracer;
    private final ReadClass readClass;

    public TracingClassInstrumenter(final ReadClass readClass, final Tracer tracer) {
        this(readClass, tracer, true);
    }

    protected TracingClassInstrumenter(final ReadClass readClass, final Tracer tracer,
            final boolean printDebug) {
        if (tracer.debug && printDebug)
            System.out.println("instrumenting " + readClass.getName());
        this.tracer = tracer;
        this.readClass = readClass;
    }

    public static String mainClassName;

    public static String getMainClassName() {
        if (mainClassName == null) {
            for (Entry<String, String> e : System.getenv().entrySet()) {
                if (e.getKey().startsWith("JAVA_MAIN_CLASS"))
                    mainClassName = e.getValue().replace('.', '/');
            }
            if(mainClassName == null)
                mainClassName = System.getProperty("sun.java.command").split(" ")[0].replace('.', '/');
        }
        return mainClassName;
    }
    @SuppressWarnings("unchecked")
    public void transform(final ClassNode classNode) {
        final ListIterator<MethodNode> methodIt = classNode.methods.listIterator();
        boolean needToGenerateClinit = classNode.name.equals(getMainClassName());
        
        while (methodIt.hasNext()) {
            MethodNode next =  methodIt.next();
            if(next.name.equals("<clinit>"))
                needToGenerateClinit = false;
            transformMethod(classNode, next, methodIt);
        }
        
        if(needToGenerateClinit)
        {
            MethodNode mn = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            FixedInstructionIterator instructionIterator = new FixedInstructionIterator(mn.instructions);
            instructionIterator.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Tracer.class), "getInstance", "()L" + Type.getInternalName(Tracer.class) + ";", false));
            instructionIterator.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(Tracer.class), "getThreadTracer", "()L" + Type.getInternalName(ThreadTracer.class) + ";", false));
            instructionIterator.add(new MethodInsnNode(INVOKEINTERFACE, Type.getInternalName(ThreadTracer.class), "mainStarting", "()V", true));
            instructionIterator.add(new InsnNode(RETURN));
            methodIt.add(mn);
        }
        
        this.readClass.ready();
    }

    protected void transformMethod(final ClassNode classNode, final MethodNode method, final ListIterator<MethodNode> methodIt) {
        final ReadMethod readMethod = new ReadMethod(this.readClass, method.access,
                method.name, method.desc, AbstractInstruction.getNextIndex());
        this.readClass.addMethod(readMethod);
        method.maxLocals = 0;
        method.maxStack = 0;

        MethodNode oldMethod;
        // only copy the old method if it has more than 2000 instructions
        if (method.instructions.size() > 2000) {
            oldMethod = new MethodNode();
            copyMethod(method, oldMethod);
        } else {
            oldMethod = null;
        }

        new TracingMethodInstrumenter(this.tracer, readMethod, classNode, method).transform(methodIt);

        // test the size of the instrumented method
        final ClassWriter testCW = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        {
        	@Override
        	protected String getCommonSuperClass(String type1, String type2) {
        		return "Java/lang/Object";
        	}  
        };
        testCW.visit(V1_7, Opcodes.ACC_PUBLIC, readClass.getInternalClassName(), null, "java/lang/Object", new String[0]);
        method.accept(testCW);
        try{
        	 testCW.toByteArray();
        }
        catch(RuntimeException ex)
        {
        	int byteCodeSize = 1 << 16 + 1;
	        if (byteCodeSize >= 1 << 16) {
	            System.err.format("WARNING: instrumented method \"%s.%s%s\" is larger than 64k bytes. undoing instrumentation.%n",
	                this.readClass.getName(), readMethod.getName(), readMethod.getDesc());
	            if (oldMethod == null) {
	                System.err.println("ERROR: uninstrumented method had less than 2000 instructions, so we cannot roll back the instrumentation...");
	            } else {
	                System.err.format("#instructions old: %d; #instructions new: %d; size new: %d%n",
	                    oldMethod.instructions.size(), method.instructions.size(), byteCodeSize);
	                copyMethod(oldMethod, method);
	            }
	        }
        }
        // reset the labels
        final Iterator<?> insnIt = method.instructions.iterator();
        while (insnIt.hasNext()) {
            final Object insn = insnIt.next();
            if (insn instanceof LabelNode)
                ((LabelNode)insn).resetLabel();
        }

    }

    @SuppressWarnings("unchecked")
    private static void copyMethod(final MethodNode from, final MethodNode to) {
        to.access = from.access;
        to.annotationDefault = from.annotationDefault;
        to.attrs = from.attrs == null ? null : new ArrayList<Attribute>(from.attrs);
        to.desc = from.desc;
        to.exceptions = from.exceptions == null ? null : new ArrayList<String>(from.exceptions);
        to.instructions.clear();
        final Iterator<?> insnIt = from.instructions.iterator();
        final Map<LabelNode, LabelNode> labelsMap = new HashMap<LabelNode, LabelNode>() {
            private static final long serialVersionUID = 6883684625241587713L;

            @Override
            public LabelNode get(final Object key) {
                LabelNode label = super.get(key);
                if (label == null)
                    put((LabelNode) key, label = new LabelNode());
                return label;
            }
        };
        while (insnIt.hasNext()) {
            final AbstractInsnNode insn = (AbstractInsnNode) insnIt.next();
            to.instructions.add(insn.clone(labelsMap ));
        }
        to.invisibleAnnotations = from.invisibleAnnotations == null ? null :
            new ArrayList<AnnotationNode>(from.invisibleAnnotations);
        if (from.invisibleParameterAnnotations == null) {
            to.invisibleParameterAnnotations = null;
        } else {
            to.invisibleParameterAnnotations = new List[from.invisibleParameterAnnotations.length];
            for (int i = 0; i < from.invisibleParameterAnnotations.length; ++i) {
                to.invisibleParameterAnnotations[i] = from.invisibleParameterAnnotations[i] == null
                    ? null : new ArrayList<AnnotationNode>(from.invisibleParameterAnnotations[i]);
            }
        }
        if (from.localVariables == null) {
            to.localVariables = null;
        } else {
            to.localVariables = new ArrayList<LocalVariableNode>(from.localVariables.size());
            for (final Object lvObj: from.localVariables) {
                final LocalVariableNode lv = (LocalVariableNode) lvObj;
                to.localVariables.add(new LocalVariableNode(
                    lv.name, lv.desc, lv.signature, labelsMap.get(lv.start),
                    labelsMap.get(lv.end), lv.index));
            }
        }
        to.maxLocals = from.maxLocals;
        to.maxStack = from.maxStack;
        to.name = from.name;
        to.signature = from.signature;
        if (from.tryCatchBlocks == null) {
            to.tryCatchBlocks = null;
        } else {
            to.tryCatchBlocks = new ArrayList<TryCatchBlockNode>(from.tryCatchBlocks.size());
            for (final Object tcbObj: from.tryCatchBlocks) {
                final TryCatchBlockNode tcb = (TryCatchBlockNode) tcbObj;
                to.tryCatchBlocks.add(new TryCatchBlockNode(
                    labelsMap.get(tcb.start), labelsMap.get(tcb.end),
                    labelsMap.get(tcb.handler), tcb.type));
            }
        }
        to.visibleAnnotations = from.visibleAnnotations == null ? null :
            new ArrayList<AnnotationNode>(from.visibleAnnotations);
        if (from.visibleParameterAnnotations == null) {
            to.visibleParameterAnnotations = null;
        } else {
            to.visibleParameterAnnotations = new List[from.visibleParameterAnnotations.length];
            for (int i = 0; i < from.visibleParameterAnnotations.length; ++i) {
                to.visibleParameterAnnotations[i] = from.visibleParameterAnnotations[i] == null
                    ? null : new ArrayList<AnnotationNode>(from.visibleParameterAnnotations[i]);
            }
        }
    }

}
