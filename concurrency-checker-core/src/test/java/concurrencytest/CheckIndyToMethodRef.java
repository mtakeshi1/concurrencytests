package concurrencytest;

import concurrencytest.util.ASMUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;
import sut.RacyIndySynchronizedMethodRef;

import java.io.PrintWriter;

public class CheckIndyToMethodRef {

    public static void main(String[] args) throws Exception {
        ASMifier asMifier = new ASMifier();
        TraceClassVisitor visitor = new TraceClassVisitor(null, asMifier, new PrintWriter(System.out));
        ClassReader reader = ASMUtils.readClass(RacyIndySynchronizedMethodRef.class);
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
    }
}
