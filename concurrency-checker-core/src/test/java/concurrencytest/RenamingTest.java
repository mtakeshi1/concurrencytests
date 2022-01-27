package concurrencytest;

import concurrencytest.agent.OpenClassLoader;
import concurrencytest.agent.RenameClassVisitor;
import concurrencytest.util.ASMUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class RenamingTest {

    @Test
    public void testSimpleRenaming() throws Exception {
        OpenClassLoader loader = new OpenClassLoader();
        Set<Class<?>> renamingTarget = Set.of(RenamingTarget.class, RenamingInterface.class, RenamedException.class);
        String suffix = "_test";
        for (Class cl : renamingTarget) {
            ClassReader reader = ASMUtils.readClass(cl);
            RenameClassVisitor visitor = new RenameClassVisitor(renamingTarget, suffix);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            byte[] bytecode = visitor.getBytecode();
            loader.addClass(cl.getName() + suffix, bytecode);
        }
        Class<?> renamedType = Class.forName(RenamingTarget.class.getName() + suffix, true, loader);
        Object renamedInstance = renamedType.getDeclaredConstructors()[0].newInstance();
        Assert.assertSame(renamedInstance, renamedInstance.getClass().getDeclaredField("targetField").get(renamedInstance));
        Assert.assertSame(renamedInstance, renamedInstance.getClass().getDeclaredField("interfaceField").get(renamedInstance));
        Class<?> renamedInteface = Class.forName(RenamingInterface.class.getName() + suffix, false, loader);
        Assert.assertTrue(renamedInteface.isInstance(renamedInstance));
        Boolean caught = (Boolean) renamedInstance.getClass().getDeclaredMethod("exceptionCaught").invoke(renamedInstance);
        Assert.assertTrue(caught);
        Object array2d = renamedInstance.getClass().getDeclaredMethod("returnArray2d").invoke(renamedInstance);
        Assert.assertEquals(1, Array.getLength(array2d));
        Object array1d = Array.get(array2d, 0);
        Assert.assertEquals(1, Array.getLength(array1d));
        Assert.assertEquals(Boolean.TRUE, renamedInstance.getClass().getDeclaredMethod("selfCheck").invoke(renamedInstance));
        try {
            renamedInstance.getClass().getDeclaredMethod("exceptionThrown").invoke(renamedInstance);
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getTargetException().getClass().getName().endsWith(suffix));
        }
    }

}
