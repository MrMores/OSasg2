package se.lnu.os.ht23.a2.required;

import java.util.*;
import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.interfaces.Memory;

/**Main memory.*/
public class MemoryImpl implements Memory {

  private final int size;
  private final Map<Integer, BlockInterval> allocatedBlocks;
  private final Map<Integer, Integer> blockDimensions;

  /**Constructor.*/
  public MemoryImpl(int size){
    this.size = size;
    this.allocatedBlocks = new HashMap<>();
    this.blockDimensions = new HashMap<>();
  }

  @Override
  public boolean containsBlock(int blockId) {
    return allocatedBlocks.containsKey(blockId);
  }

  @Override
  public List<Integer> blocks() {
    return new ArrayList<>(allocatedBlocks.keySet());
  }

  @Override
   public int blockDimension(int blockId) {
    return allocatedBlocks.getOrDefault(blockId, new BlockInterval(0, 0)).getHighAddress() 
      - allocatedBlocks.getOrDefault(blockId, new BlockInterval(0, 0)).getLowAddress() + 1;
  }

  @Override
  public BlockInterval getBlockInterval(int blockId) {
    return allocatedBlocks.get(blockId);
  }

  @Override
  public Set<Integer> neighboringBlocks(int blockId) {
    Set<Integer> neighbors = new HashSet<>();
    BlockInterval interval = allocatedBlocks.get(blockId);

    if (interval != null) {
      for (BlockInterval block : allocatedBlocks.values()) {
        if (block.getLowAddress() == interval.getHighAddress() + 1 
            || block.getHighAddress() == interval.getLowAddress() - 1) {
          neighbors.add(allocatedBlocks.entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().equals(block))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null));
        }
      }
    }
    return neighbors;
  }

  @Override
  public double fragmentation() {
    int largestBlockSize = 0;

    for (BlockInterval block : allocatedBlocks.values()) {
      int blockSize = block.getHighAddress() - block.getLowAddress() + 1;
      if (blockSize > largestBlockSize) {
        largestBlockSize = blockSize;
      }
    }
    if (largestBlockSize == 0 || allocatedBlocks.size() == 1) {
      return 0.0;
    }
    return 1 - (double) largestBlockSize / (double) size;
  }

  @Override
  public Set<BlockInterval> freeSlots() {
    Set<BlockInterval> freeSlots = new HashSet<>();
    List<BlockInterval> sortedBlocks = new ArrayList<>(allocatedBlocks.values());
    sortedBlocks.sort(Comparator.comparingInt(BlockInterval::getLowAddress));

    if (sortedBlocks.isEmpty()) {
      freeSlots.add(new BlockInterval(0, size - 1));
    } else {
      if (sortedBlocks.get(0).getLowAddress() > 0) {
        freeSlots.add(new BlockInterval(0, sortedBlocks.get(0).getLowAddress() - 1));
      }
      for (int i = 0; i < sortedBlocks.size() - 1; i++) {
        int start = sortedBlocks.get(i).getHighAddress() + 1;
        int end = sortedBlocks.get(i + 1).getLowAddress() - 1;
        if (end >= start) {
          freeSlots.add(new BlockInterval(start, end));
        }
      }
      if (sortedBlocks.get(sortedBlocks.size() - 1).getHighAddress() < size - 1) {
        freeSlots.add(new BlockInterval(sortedBlocks.get(sortedBlocks.size() 
            - 1).getHighAddress() + 1, size - 1));
      }
    }
    return freeSlots;
  }

  @Override
  public boolean equals(Object o) {
    return false;
  }

  @Override
  public String toString() {
    StringBuilder retStr = new StringBuilder("Memory Size = " + size + "\n");
    if (blocks() != null) {
      for (int blockId : blocks()) {
        BlockInterval inter = getBlockInterval(blockId);
        retStr.append("(").append(inter.getLowAddress()).append("-")
        .append(inter.getHighAddress()).append(")")
                        .append(" --> ").append("ID ").append(blockId).append("\n");
      }
    }
    if (freeSlots() != null) {
      for (BlockInterval bi : freeSlots()) {
        retStr.append("(").append(bi.getLowAddress())
        .append("-").append(bi.getHighAddress()).append(")")
                        .append(" --> ").append("EMPTY").append("\n");
      }
    }
    return retStr.toString();
  }

  /**Allocates block.*/
  public void allocateBlock(int blockId, int startAddress, int dimension) {
    int endAddress = startAddress + dimension - 1;

    if (startAddress < 0 || endAddress >= size) {
      throw new IllegalArgumentException("Invalid start address or dimension exceeds memory size");
    }

    BlockInterval interval = new BlockInterval(startAddress, startAddress + dimension - 1);
    allocatedBlocks.put(blockId, interval);
    blockDimensions.put(blockId, dimension);
  }

  public void deallocateBlock(int blockId) {
    this.blockDimensions.remove(blockId);
    this.allocatedBlocks.remove(blockId);
  }

  /**Compacts the memory.*/
  public void compactMemory() {
    List<BlockInterval> sortedBlocks = new ArrayList<>(allocatedBlocks.values());
    sortedBlocks.sort(Comparator.comparingInt(BlockInterval::getLowAddress));
    
    int currentAddress = 0;
    Map<Integer, BlockInterval> newAllocatedBlocks = new HashMap<>();
    
    for (BlockInterval block : sortedBlocks) {
      int dimension = block.getHighAddress() - block.getLowAddress() + 1;
      int blockId = getBlockIdForInterval(block);
      newAllocatedBlocks.put(blockId, new BlockInterval(currentAddress, 
          currentAddress + dimension - 1));
      currentAddress += dimension;
    }
    
    allocatedBlocks.clear();
    allocatedBlocks.putAll(newAllocatedBlocks);
  }
    
  private int getBlockIdForInterval(BlockInterval interval) {
    for (Map.Entry<Integer, BlockInterval> entry : allocatedBlocks.entrySet()) {
      if (entry.getValue().equals(interval)) {
        return entry.getKey();
      }
    }
    throw new IllegalStateException("Block ID not found for interval: " + interval);
  }
}
