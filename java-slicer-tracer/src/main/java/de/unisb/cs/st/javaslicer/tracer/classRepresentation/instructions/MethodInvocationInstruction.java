package de.unisb.cs.st.javaslicer.tracer.classRepresentation.instructions;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.objectweb.asm.Opcodes;

import de.unisb.cs.st.javaslicer.tracer.classRepresentation.ReadMethod;
import de.unisb.cs.st.javaslicer.tracer.classRepresentation.ReadMethod.MethodReadInformation;

/**
 * Class representing a method invokation instruction (INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
 * INVOKEINTERFACE).
 *
 * @author Clemens Hammacher
 */
public class MethodInvocationInstruction extends AbstractInstruction {

    private final String internalClassName;
    private final String methodName;
    private final String methodDesc;
    private final boolean[] parameterIsLong;
    private final boolean returnValueIsLong;

    public MethodInvocationInstruction(final ReadMethod readMethod, final int opcode,
            final int lineNumber, final String internalClassName, final String methodName,
            final String methodDesc) {
        super(readMethod, opcode, lineNumber);
        assert opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL
            || opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKEINTERFACE;
        this.internalClassName = internalClassName;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        final org.objectweb.asm.Type[] parameterTypes = org.objectweb.asm.Type.getArgumentTypes(methodDesc);
        this.parameterIsLong = new boolean[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            this.parameterIsLong[i] = parameterTypes[i].getSize() == 2;
        }
        this.returnValueIsLong = org.objectweb.asm.Type.getReturnType(methodDesc).getSize() == 2;
    }

    private MethodInvocationInstruction(final ReadMethod readMethod, final int lineNumber, final int opcode,
            final String internalClassName, final String methodName, final String methodDesc, final int index) {
        super(readMethod, opcode, lineNumber, index);
        assert opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL
            || opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKEINTERFACE;
        this.internalClassName = internalClassName;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        final org.objectweb.asm.Type[] parameterTypes = org.objectweb.asm.Type.getArgumentTypes(methodDesc);
        this.parameterIsLong = new boolean[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            this.parameterIsLong[i] = parameterTypes[i].getSize() == 2;
        }
        this.returnValueIsLong = org.objectweb.asm.Type.getReturnType(methodDesc).getSize() == 2;
    }

    public String getInternalClassName() {
        return this.internalClassName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getMethodDesc() {
        return this.methodDesc;
    }

    public int getParameterCount() {
        return this.parameterIsLong.length;
    }

    public boolean parameterIsLong(final int paramIndex) {
        if (paramIndex < 0 || paramIndex >= this.parameterIsLong.length)
            throw new IllegalArgumentException("Parameter count: " + this.parameterIsLong.length + "; requested: " + paramIndex);
        return this.parameterIsLong[paramIndex];
    }

    public boolean returnValueIsLong() {
        return this.returnValueIsLong;
    }

    @Override
    public Type getType() {
        return Type.METHODINVOCATION;
    }

    @Override
    public void writeOut(final DataOutput out) throws IOException {
        super.writeOut(out);
        out.writeUTF(this.internalClassName);
        out.writeUTF(this.methodName);
        out.writeUTF(this.methodDesc);
    }

    public static MethodInvocationInstruction readFrom(final DataInput in, final MethodReadInformation methodInfo, final int opcode, final int index, final int lineNumber) throws IOException {
        final String internalClassName = in.readUTF();
        final String methodName = in.readUTF();
        final String methodDesc = in.readUTF();
        return new MethodInvocationInstruction(methodInfo.getMethod(), lineNumber, opcode, internalClassName, methodName, methodDesc, index);
    }

    @Override
    public String toString() {
        String type;
        switch (getOpcode()) {
        case Opcodes.INVOKEVIRTUAL:
            type = "INVOKEVIRTUAL";
            break;
        case Opcodes.INVOKESPECIAL:
            type = "INVOKESPECIAL";
            break;
        case Opcodes.INVOKESTATIC:
            type = "INVOKESTATIC";
            break;
        case Opcodes.INVOKEINTERFACE:
            type = "INVOKEINTERFACE";
            break;
        default:
            assert false;
            type = "--ERROR--";
        }

        final StringBuilder sb = new StringBuilder(type.length() + this.internalClassName.length() + this.methodName.length() + this.methodDesc.length() + 2);
        sb.append(type).append(' ').append(this.internalClassName).append('.').append(this.methodName).append(this.methodDesc);
        return sb.toString();
    }

}
