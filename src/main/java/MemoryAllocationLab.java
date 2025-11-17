import java.io.*;
import java.util.*;

public class MemoryAllocationLab {

    static class MemoryBlock {
        int start;
        int size;
        String processName;  // null if free

        public MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        public boolean isFree() {
            return processName == null;
        }

        public int getEnd() {
            return start + size - 1;
        }
    }

    static int totalMemory;
    static ArrayList<MemoryBlock> memory;
    static int successfulAllocations = 0;
    static int failedAllocations = 0;

    public static void processRequests(String filename) {
        memory = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename)); // Buffered reader based on the file
            String line = reader.readLine(); // Lines from the reader
            totalMemory = Integer.parseInt(line); // Total memory size from the first line

            System.out.println("Total Memory: " + totalMemory + " KB");
            System.out.println("----------------------------------------\n");

            memory.add(new MemoryBlock(0, totalMemory, null)); // Initialize the free memory as one big large block
            
            System.out.println("Processing requests...\n");
            while ((line = reader.readLine()) != null) // While the line is not null
            {
                String[] lineParts = line.split(" "); // Splits the line by each word into an array.
                String instruction = lineParts[0];

                // Each part of the line parts is assigned a variable, such as 'instruction' for the instruction
                // or 'name' for the name. Although this was not strictly necessary, it helps to reduce
                // confusion when working with these variables.

                if (instruction.equals("REQUEST"))
                {
                    String name = lineParts[1];
                    int size = Integer.parseInt(lineParts[2]);

                    System.out.print(line + " KB -> ");
                    allocate(name, size);
                }
                else if (instruction.equals("RELEASE"))
                {
                    String name = lineParts[1];
                    System.out.print(line + " -> ");
                    deallocate(name);
                }
                else
                {
                    // If no instruction is given, prints an error
                    System.out.println("ERROR: NO INSTRUCTION GIVEN");
                }
            }

            reader.close(); // Close the BufferedReader
        } catch (Exception e) {
            e.printStackTrace(); 
        }

    }

    private static void allocate(String processName, int size) {
        for (int i = 0; i < memory.size(); i++) // Scan the memory list
        {
            MemoryBlock block = memory.get(i);

            if (block.isFree() && block.size >= size)
            {
                if (block.size > size) // If the block size is greater than the total size available
                {
                    // The free block is calculated by taking the end of the allocated block + 1 (to ensure no
                    // overlap), subtracting the size it takes, and setting the name to null. The free part
                    // is replaced with the allocated block, and the free block is added to the end.
                    MemoryBlock allocated = new MemoryBlock(block.start, size, processName);
                    MemoryBlock free = new MemoryBlock(allocated.getEnd() + 1, block.size - size, null);
                    memory.set(i, allocated);
                    memory.add(i + 1, free);
                }
                else
                {
                    block.processName = processName; // If the sizes are completely equal, the process name is just set
                }
                successfulAllocations++;
                System.out.println("SUCCESS");
                return;
            }
        }
        
        failedAllocations++;
        System.out.println("FAILURE");
    }

    private static void deallocate(String processName)
    {
        for (int i = 0; i < memory.size(); i++)
        {
            MemoryBlock block = memory.get(i);

            // If the block is not free and the process names are the same
            if (!block.isFree() && block.processName.equals(processName))
            {
                block.processName = null; // Sets processName to null
                mergeAdjacentBlocks(); // Merges adjacent free space, reducing fragments
                System.out.println("SUCCESS");
                return;
            }
        }

        System.out.println("ERROR: " + processName + " NOT FOUND"); // Error handler if the process name does not exist
    }

    private static void mergeAdjacentBlocks() {
    for (int i = 0; i < memory.size() - 1; i++) {
        MemoryBlock current = memory.get(i);
        MemoryBlock next = memory.get(i + 1);
        
        // If the current position and next position are both free, they are merged into one
        if (current.isFree() && next.isFree()) {
            current.size += next.size;
            memory.remove(i + 1);
            i--;  // Check this position again
        }
    }
}

    public static void displayStatistics() {
        System.out.println("\n========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");

        int blockNum = 1;
        for (MemoryBlock block : memory) {
            String status = block.isFree() ? "FREE" : block.processName;
            String allocated = block.isFree() ? "" : " - ALLOCATED";
            System.out.printf("Block %d: [%d-%d]%s%s (%d KB)%s\n",
                    blockNum++,
                    block.start,
                    block.getEnd(),
                    " ".repeat(Math.max(1, 10 - String.valueOf(block.getEnd()).length())),
                    status,
                    block.size,
                    allocated);
        }

        System.out.println("\n========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");

        int allocatedMem = 0;
        int freeMem = 0;
        int numProcesses = 0;
        int numFreeBlocks = 0;
        int largestFree = 0;

        for (MemoryBlock block : memory) {
            if (block.isFree()) {
                freeMem += block.size;
                numFreeBlocks++;
                largestFree = Math.max(largestFree, block.size);
            } else {
                allocatedMem += block.size;
                numProcesses++;
            }
        }

        double allocatedPercent = (allocatedMem * 100.0) / totalMemory;
        double freePercent = (freeMem * 100.0) / totalMemory;
        double fragmentation = freeMem > 0 ?
                ((freeMem - largestFree) * 100.0) / freeMem : 0;

        System.out.printf("Total Memory:           %d KB\n", totalMemory);
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)\n", allocatedMem, allocatedPercent);
        System.out.printf("Free Memory:            %d KB (%.2f%%)\n", freeMem, freePercent);
        System.out.printf("Number of Processes:    %d\n", numProcesses);
        System.out.printf("Number of Free Blocks:  %d\n", numFreeBlocks);
        System.out.printf("Largest Free Block:     %d KB\n", largestFree);
        System.out.printf("External Fragmentation: %.2f%%\n", fragmentation);

        System.out.println("\nSuccessful Allocations: " + successfulAllocations);
        System.out.println("Failed Allocations:     " + failedAllocations);
        System.out.println("========================================");
    }

    /**
     * Main method (FULLY PROVIDED)
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MemoryAllocationLab <input_file>");
            System.out.println("Example: java MemoryAllocationLab memory_requests.txt");
            return;
        }

        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================\n");
        System.out.println("Reading from: " + args[0]);

        processRequests(args[0]);
        displayStatistics();
    }
}