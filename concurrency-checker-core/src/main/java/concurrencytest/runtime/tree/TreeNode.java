package concurrencytest.runtime.tree;

import concurrencytest.runtime.RuntimeState;

import java.util.List;

public interface TreeNode {

    List<ThreadState> threads();

    TreeNode advanced(ThreadState selectedToProceed, RuntimeState next);

}
