package concurrencytest.config;

import concurrencytest.runtime.tree.HeapTree;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.util.Utils;

/**
 * Configures how the tree is represented
 */
public enum TreeMode {

    /**
     * Plain heap implementation that holds the full details of the states reached
     */
    HEAP{
        @Override
        public Tree instantiateTree() {
            return new HeapTree(false);
        }
    },
    /**
     * Plain heap implementation that holds minimal information of the states, just enough to do the exploration
     * but not enough to properly debug this library. It should use minimal heap.
     */
    COMPACT_HEAP {
        @Override
        public Tree instantiateTree() {
            return new HeapTree(true);
        }
    },
    /**
     * Off-heap implementation. Uses {@link java.nio.DirectByteBuffer} to hold information, to try to minimize heap usage while
     * preserving debug information.
     *
     * Currently not implemented.
     */
    OFF_HEAP,
    /**
     * Off-heap implementation that uses {@link java.nio.MappedByteBuffer} to hold information, to try to minimize heap usage while
     * preserving debug information and also be able to share the files with other processes. It should be used for exploring the graph off-process
     * or with {@link ExecutionMode#FORK}
     *
     * Currently not implemented.
     */
    MAPPED_FILE;

    public Tree instantiateTree() {
        return Utils.todo();
    }


}
