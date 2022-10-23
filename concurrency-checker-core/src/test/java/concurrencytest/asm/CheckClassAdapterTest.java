package concurrencytest.asm;

import concurrencytest.util.ASMUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import sut.RacyActorsFieldAccess;

import java.io.IOException;
import java.util.HashMap;

public class CheckClassAdapterTest {

    @Test
    public void test() throws IOException {
        ClassReader classReader = ASMUtils.readClass(RacyActorsFieldAccess.class);
        Assert.assertNotNull(classReader);
        //SimpleRemapper remapper = getRemapper();
        //                String map = remapper.map(Type.getInternalName(c));
        //                if (map == null) {
        //                    throw new RuntimeException("could not find renamed class name for class: %s".formatted(c.getName()));
        //                }
        //                String classFileName = map + ".class";
        //                return new ClassRemapper(fileWritingVisitor(rootFolder, classFileName), remapper);
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put(Type.getInternalName(RacyActorsFieldAccess.class), Type.getInternalName(RacyActorsFieldAccess.class) + "$1234");
        mapping.put(Type.getInternalName(RacyActorsFieldAccess.ValueHolder.class), Type.getInternalName(RacyActorsFieldAccess.ValueHolder.class));
        SimpleRemapper remapper = new SimpleRemapper(mapping);
        ClassRemapper rem = new ClassRemapper(new CheckClassAdapter(null, true), remapper);
        classReader.accept(rem, ClassReader.EXPAND_FRAMES);
//        RacyActorsFieldAccess.class
    }

}
