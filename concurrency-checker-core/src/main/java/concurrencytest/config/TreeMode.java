package concurrencytest.config;

import concurrencytest.runtime.tree.HeapTree;
import concurrencytest.runtime.tree.Tree;
import concurrencytest.util.Utils;

public enum TreeMode {

    HEAP{
        @Override
        public Tree instantiateTree() {
            return new HeapTree(false);
        }
    }, COMPACT_HEAP {
        @Override
        public Tree instantiateTree() {
            return new HeapTree(true);
        }
    }, OFF_HEAP, MAPPED_FILE;

    public Tree instantiateTree() {
        return Utils.todo();
    }


}
