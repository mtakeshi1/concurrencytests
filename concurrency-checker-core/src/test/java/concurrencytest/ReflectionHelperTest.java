package concurrencytest;

import concurrencytest.reflection.ReflectionHelper;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;


public class ReflectionHelperTest {

    @Test
    public void testArray() throws Exception {
        String descriptor = "([I[[I[Ljava/lang/String;[J[S[[[C)V";
        Type type = Type.getMethodType(descriptor);
        Assert.assertEquals(6, type.getArgumentTypes().length);
        Type[] types = type.getArgumentTypes();
        Assert.assertEquals(int[].class, ReflectionHelper.resolveType(types[0].getClassName()));
        Assert.assertEquals(int[][].class, ReflectionHelper.resolveType(types[1].getClassName()));
        Assert.assertEquals(String[].class, ReflectionHelper.resolveType(types[2].getClassName()));
        Assert.assertEquals(long[].class, ReflectionHelper.resolveType(types[3].getClassName()));
        Assert.assertEquals(short[].class, ReflectionHelper.resolveType(types[4].getClassName()));
        Assert.assertEquals(char[][][].class, ReflectionHelper.resolveType(types[5].getClassName()));
    }

}
