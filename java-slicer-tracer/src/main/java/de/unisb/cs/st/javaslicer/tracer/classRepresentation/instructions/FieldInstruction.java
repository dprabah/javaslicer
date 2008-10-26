package de.unisb.cs.st.javaslicer.tracer.classRepresentation.instructions;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.objectweb.asm.Opcodes;

import de.unisb.cs.st.javaslicer.tracer.classRepresentation.ReadMethod;
import de.unisb.cs.st.javaslicer.tracer.classRepresentation.ReadMethod.MethodReadInformation;
import de.unisb.cs.st.javaslicer.tracer.exceptions.TracerException;
import de.unisb.cs.st.javaslicer.tracer.traceResult.ThreadTraceResult.BackwardInstructionIterator;


/**
 * Class representing a field instruction (GETFIELD, PUTFIELD, GETSTATIC or PUTSTATIC).
 *
 * @author Clemens Hammacher
 */
public class FieldInstruction extends AbstractInstruction {

    public static class Instance extends AbstractInstance {

        private final long objectId;

        public Instance(final FieldInstruction fieldInstr, final long occurenceNumber, final int stackDepth, final long objectId) {
            super(fieldInstr, occurenceNumber, stackDepth);
            this.objectId = objectId;
        }

        public long getObjectId() {
            return this.objectId;
        }

    }

    private final String ownerInternalClassName;
    private final String fieldName;
    private final String fieldDesc;
    private final int objectTraceSeqIndex;
    private final boolean longValue;

    public FieldInstruction(final ReadMethod readMethod, final int opcode,
            final int lineNumber, final String ownerInternalClassName,
            final String fieldName, final String fieldDesc,
            final int objectTraceSeqIndex) {
        super(readMethod, opcode, lineNumber);
        this.ownerInternalClassName = ownerInternalClassName;
        this.fieldName = fieldName.intern();
        this.fieldDesc = fieldDesc;
        this.objectTraceSeqIndex = objectTraceSeqIndex;
        this.longValue = org.objectweb.asm.Type.getType(fieldDesc).getSize() == 2;
    }

    private FieldInstruction(final ReadMethod readMethod, final int opcode, final int lineNumber,
            final String ownerInternalClassName, final String fieldName,
            final String fieldDesc, final int objectTraceSeqIndex, final int index) {
        super(readMethod, opcode, lineNumber, index);
        this.ownerInternalClassName = ownerInternalClassName;
        this.fieldName = fieldName.intern();
        this.fieldDesc = fieldDesc;
        this.objectTraceSeqIndex = objectTraceSeqIndex;
        this.longValue = org.objectweb.asm.Type.getType(fieldDesc).getSize() == 2;
    }

    public String getOwnerInternalClassName() {
        return this.ownerInternalClassName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public String getFieldDesc() {
        return this.fieldDesc;
    }

    public boolean isLongValue() {
        return this.longValue;
    }

    @Override
    public Type getType() {
        return Type.FIELD;
    }

    @Override
    public Instance getNextInstance(final BackwardInstructionIterator backwardInstructionIterator) throws TracerException {
        final long objectId = this.objectTraceSeqIndex == -1 ? -1 :
            backwardInstructionIterator.getNextLong(this.objectTraceSeqIndex);
        return new Instance(this, backwardInstructionIterator.getNextInstructionOccurenceNumber(getIndex()),
                backwardInstructionIterator.getStackDepth(), objectId);
    }

    @Override
    public String toString() {
        String type;
        switch (getOpcode()) {
        case Opcodes.PUTSTATIC:
            type = "PUTSTATIC";
            break;
        case Opcodes.GETSTATIC:
            type = "GETSTATIC";
            break;

        case Opcodes.GETFIELD:
            type = "GETFIELD";
            break;

        case Opcodes.PUTFIELD:
            type = "PUTFIELD";
            break;

        default:
            assert false;
            type = "--ERROR--";
            break;
        }

        final StringBuilder sb = new StringBuilder(type.length() + this.ownerInternalClassName.length() + this.fieldName.length() + this.fieldDesc.length() + 3);
        sb.append(type).append(' ').append(this.ownerInternalClassName).append('.').append(this.fieldName).append(' ').append(this.fieldDesc);
        return sb.toString();
    }

    @Override
    public void writeOut(final DataOutput out) throws IOException {
        super.writeOut(out);
        out.writeUTF(this.fieldDesc);
        out.writeUTF(this.fieldName);
        out.writeInt(this.objectTraceSeqIndex);
        out.writeUTF(this.ownerInternalClassName);
    }

    public static FieldInstruction readFrom(final DataInput in, final MethodReadInformation methodInfo, final int opcode, final int index, final int lineNumber) throws IOException {
        final String fieldDesc = in.readUTF();
        final String fieldName = in.readUTF();
        final int objectTraceSeqIndex = in.readInt();
        final String ownerInternalClassName = in.readUTF();
        return new FieldInstruction(methodInfo.getMethod(), opcode, lineNumber, ownerInternalClassName, fieldName, fieldDesc, objectTraceSeqIndex, index);
    }

}
