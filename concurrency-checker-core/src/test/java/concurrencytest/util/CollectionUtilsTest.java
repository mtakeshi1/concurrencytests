package concurrencytest.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CollectionUtilsTest {

    @Test
    public void testRemoveFirst() {
        var original = List.of("1", "2", "3");
        var list = CollectionUtils.removeFirst(original, "2"::equals);
        Assert.assertEquals(List.of("1", "3"), list);
        list = CollectionUtils.removeFirst(list, "2"::equals);
        Assert.assertEquals(List.of("1", "3"), list);


        original = List.of("1", "1", "1");
        list = CollectionUtils.removeFirst(original, "1"::equals);
        Assert.assertEquals(List.of("1", "1"), list);
    }

    @Test
    public void testCopyAndAdd() {
        var original = List.of("1", "2", "3");
        var list = CollectionUtils.copyAndAdd(original, "4");
        Assert.assertEquals(List.of("1", "2", "3", "4"), list);
    }
}