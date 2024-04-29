package se.lnu.os.ht23.a2.required;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import se.lnu.os.ht23.a2.provided.abstract_.Instruction;
import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.data.StrategyType;
import se.lnu.os.ht23.a2.provided.exceptions.InstructionException;
import se.lnu.os.ht23.a2.provided.instructions.AllocationInstruction;
import se.lnu.os.ht23.a2.provided.instructions.CompactInstruction;
import se.lnu.os.ht23.a2.provided.instructions.DeallocationInstruction;
import se.lnu.os.ht23.a2.provided.interfaces.Memory;
import se.lnu.os.ht23.a2.provided.interfaces.SimulationInstance;

/**Represents an instance of a memory management simulation.*/
public class SimulationInstanceImpl implements SimulationInstance {
  private Queue<Instruction> remainingInstructions;
  private final Memory memory;
  private final StrategyType strategyType;
  private List<InstructionException> instructionExceptions;

  /**Constructor.*/
  public SimulationInstanceImpl(Queue<Instruction> instructions, Memory memory, 
      StrategyType strategyType) {
    this.remainingInstructions = instructions;
    this.memory = memory;
    this.strategyType = strategyType;
    this.instructionExceptions = new ArrayList<>();
  }

  @Override
  public void runAll() {
    while (!remainingInstructions.isEmpty()) {
      executeInstruction(remainingInstructions.poll());
    }
  }

  @Override
  public void run(int steps) {
    for (int i = 0; i < steps && !remainingInstructions.isEmpty(); i++) {
      executeInstruction(remainingInstructions.poll());
    }
  }

  private void executeInstruction(Instruction instruction) {
    if (instruction instanceof AllocationInstruction) {
      executeAllocationInstruction((AllocationInstruction) instruction);
    } else if (instruction instanceof DeallocationInstruction) {
      executeDeallocationInstruction((DeallocationInstruction) instruction);
    } else if (instruction instanceof CompactInstruction) {
      executeCompactInstruction();
    }
  }

  private void executeAllocationInstruction(AllocationInstruction instruction) {
    int blockId = instruction.getBlockId();
    int dimension = instruction.getDimension();

    try {
      if (memory.containsBlock(blockId)) {
        throw new InstructionException(instruction, 10);
      }
      if (strategyType == StrategyType.FIRST_FIT) {
        allocateFirstFit(blockId, dimension);
      } else if (strategyType == StrategyType.BEST_FIT) {
        allocateBestFit(blockId, dimension);
      } else if (strategyType == StrategyType.WORST_FIT) {
        allocateWorstFit(blockId, dimension);
      } 
    } catch (InstructionException e) {
      instructionExceptions.add(e);
    }
  }

  private void executeDeallocationInstruction(DeallocationInstruction instruction) {
    int blockId = instruction.getBlockId();

    try {
      if (!memory.containsBlock(blockId)) {
        throw new InstructionException(instruction, 10);
      }
      ((MemoryImpl) memory).deallocateBlock(blockId);
    } catch (InstructionException e) {
      instructionExceptions.add(e);
    }
  }

  private void executeCompactInstruction() {
    ((MemoryImpl) memory).compactMemory();
  }

  private void allocateFirstFit(int blockId, int dimension) {
    List<BlockInterval> freeSlots = new ArrayList<>(memory.freeSlots());
    freeSlots.sort(Comparator.comparingInt(BlockInterval::getLowAddress));

    for (BlockInterval freeSlot : freeSlots) {
      if (freeSlot.getHighAddress() - freeSlot.getLowAddress() + 1 >= dimension) {
        ((MemoryImpl) memory).allocateBlock(blockId, freeSlot.getLowAddress(), dimension);
        return;
      }
    }
    throw new InstructionException(new AllocationInstruction(blockId, dimension), 10);
  }

  private void allocateBestFit(int blockId, int dimension) {
    List<BlockInterval> freeSlots = new ArrayList<>(memory.freeSlots());
    freeSlots.sort(Comparator.comparingInt(slot -> slot.getHighAddress() 
        - slot.getLowAddress() + 1));

    for (BlockInterval freeSlot : freeSlots) {
      if (freeSlot.getHighAddress() - freeSlot.getLowAddress() + 1 >=  dimension) {
        ((MemoryImpl) memory).allocateBlock(blockId, freeSlot.getLowAddress(), dimension);
        return;
      }
    }
    throw new InstructionException(new AllocationInstruction(blockId, dimension), 10);
  }
    
  private void allocateWorstFit(int blockId, int dimension) {
    List<BlockInterval> freeSlots = new ArrayList<>(memory.freeSlots());
    freeSlots.sort(Comparator.comparingInt(slot -> -(slot.getHighAddress() 
        - slot.getLowAddress() + 1)));

    for (BlockInterval freeSlot : freeSlots) {
      if (freeSlot.getHighAddress() - freeSlot.getLowAddress() + 1 >= dimension) {
        ((MemoryImpl) memory).allocateBlock(blockId, freeSlot.getLowAddress(), dimension);
        return;
      }
    }
    throw new InstructionException(new AllocationInstruction(blockId, dimension), 10);
  }

  @Override
  public Memory getMemory() {
    return this.memory;
  }

  @Override
  public Queue<Instruction> getInstructions() {
    return this.remainingInstructions;
  }

  @Override
  public StrategyType getStrategyType() {
    return this.strategyType;
  }

  @Override
  public List<InstructionException> getExceptions() {
    return this.instructionExceptions;
  }

  @Override
  public String toString() {
    return "Simulation Details:\n" 
      + "Strategy: " + strategyType + "\n" 
      + "List of Remaining Instructions: " + remainingInstructions + "\n" 
      + "Current Memory Structure:\n\n" + memory + "\n" 
        + "List of Occurred Exceptions: " + instructionExceptions;
  }
}
