package concurrencytest.tree;

import java.util.List;

public interface TreeNode {

    List<ThreadState> threads();

    TreeNode advance(ThreadState selectedToProceed);

    default boolean canAdvance() {
        return threads().stream().anyMatch(ThreadState::runnable);
    }


}
