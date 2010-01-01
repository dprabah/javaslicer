package de.unisb.cs.st.javaslicer.traceResult;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import de.hammacher.util.maps.IntegerMap;
import de.hammacher.util.maps.IntegerToLongMap;
import de.unisb.cs.st.javaslicer.common.classRepresentation.Instruction;
import de.unisb.cs.st.javaslicer.common.classRepresentation.InstructionInstance;
import de.unisb.cs.st.javaslicer.common.classRepresentation.InstructionInstanceFactory;
import de.unisb.cs.st.javaslicer.common.classRepresentation.TraceIterator;
import de.unisb.cs.st.javaslicer.common.exceptions.TracerException;
import de.unisb.cs.st.javaslicer.traceResult.traceSequences.ConstantTraceSequence.ConstantIntegerTraceSequence;
import de.unisb.cs.st.javaslicer.traceResult.traceSequences.ConstantTraceSequence.ConstantLongTraceSequence;

public class ForwardTraceIterator<InstanceType extends InstructionInstance>
        implements Iterator<InstructionInstance>, TraceIterator {

    private long backwardInstrNr;
    private int nextIndex;
    private int stackDepth;

    private int nextJumpNr;
    private final long[] jumpInstrNrs;
    private final int[] jumps;
    private final byte[] stackDepthChanges;

    private final IntegerToLongMap occurrences;
    private final ThreadTraceResult threadTraceResult;
    private final IntegerMap<ListIterator<Integer>> integerSequenceIterators;
    private final IntegerMap<ListIterator<Long>> longSequenceIterators;
    private final InstructionInstanceFactory<? extends InstanceType> instanceFactory;

    // for progress approximation
	private long numCrossedLabels = 0;

    public ForwardTraceIterator(ThreadTraceResult threadTraceResult,
            ForwardIterationInformation forwInfo, InstructionInstanceFactory<? extends InstanceType> instanceFactory) {
        this.threadTraceResult = threadTraceResult;
        this.backwardInstrNr = forwInfo.instrCount;
        this.nextIndex = forwInfo.firstInstrIndex;
        this.jumpInstrNrs = forwInfo.jumpInstrNrs;
        this.jumps = forwInfo.jumps;
        this.stackDepthChanges = forwInfo.stackDepthChanges;
        this.stackDepth = 1;
        this.nextJumpNr = forwInfo.jumpInstrNrs.length-1;
        this.occurrences = new IntegerToLongMap();
        this.integerSequenceIterators = new IntegerMap<ListIterator<Integer>>();
        this.longSequenceIterators = new IntegerMap<ListIterator<Long>>();
        this.instanceFactory = instanceFactory;
    }

    public boolean hasNext() {
        return this.backwardInstrNr != 0;
    }

    public InstructionInstance next() {
        if (!hasNext())
            throw new NoSuchElementException();
        --this.backwardInstrNr;

        while (true) {
            final Instruction instr = this.threadTraceResult.findInstruction(this.nextIndex);
            final InstructionInstance inst = instr.getNextInstance(this, this.stackDepth, this.backwardInstrNr, this.instanceFactory);
            if (inst == null) {
                ++this.nextIndex;
                continue;
            }

            if (this.nextJumpNr != -1 && this.backwardInstrNr == this.jumpInstrNrs[this.nextJumpNr]) {
                this.nextIndex += this.jumps[this.nextJumpNr];
                this.stackDepth += this.stackDepthChanges[this.nextJumpNr--];
            } else {
                ++this.nextIndex;
            }

            return inst;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public long getNextInstructionOccurenceNumber(int index) {
        return this.occurrences.incrementAndGet(index, 1) - 1;
    }

    public long getNextLong(int seqIndex) throws TracerException {
        ListIterator<Long> it = this.longSequenceIterators.get(seqIndex);
        if (it == null) {
            try {
                it = ((ConstantLongTraceSequence)this.threadTraceResult.sequences.get(seqIndex)).iterator();
            } catch (final IOException e) {
                throw new TracerException(e);
            }
            this.longSequenceIterators.put(seqIndex, it);
        }
        if (!it.hasNext())
            throw new TracerException("corrupted data (cannot trace backwards)");
        return it.next();
    }

    public int getNextInteger(int seqIndex) throws TracerException {
        ListIterator<Integer> it = this.integerSequenceIterators.get(seqIndex);
        if (it == null) {
            try {
                it = ((ConstantIntegerTraceSequence)this.threadTraceResult.sequences.get(seqIndex)).iterator();
            } catch (final IOException e) {
                throw new TracerException(e);
            }
            this.integerSequenceIterators.put(seqIndex, it);
        }
        if (!it.hasNext())
            throw new TracerException("corrupted data (cannot trace backwards)");
        return it.next();
    }

	public void incNumCrossedLabels() {
		++this.numCrossedLabels;
	}

	public double getPercentageDone() {
        return this.threadTraceResult.numCrossedLabels == 0 ? 0 : (100. * this.numCrossedLabels / this.threadTraceResult.numCrossedLabels);
    }

    public long getNumCrossedLabels() {
        return this.numCrossedLabels;
    }

    public long getTotalNumCrossedLabels() {
        return this.threadTraceResult.numCrossedLabels;
    }

}