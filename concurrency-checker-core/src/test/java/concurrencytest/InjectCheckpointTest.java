package concurrencytest;

import concurrencytest.agent.InjectCheckpointVisitor;
import concurrencytest.annotations.CheckpointInjectionPoint;
import concurrencytest.util.ASMUtils;
import org.jgroups.protocols.TP;
import org.jgroups.util.RingBuffer;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

public class InjectCheckpointTest {

    @Test
    public void testInjectRingBufferWithLocks() throws Exception {
        ClassReader reader = ASMUtils.readClass(RingBuffer.class);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        InjectCheckpointVisitor outer = new InjectCheckpointVisitor(writer, RingBuffer.class.getName(), () -> 1L, new HashMap<>(), new HashSet<>(), EnumSet.allOf(CheckpointInjectionPoint.class));
        reader.accept(outer, ClassReader.EXPAND_FRAMES);

        byte[] bytecode = writer.toByteArray();
        ClassReader resultsReader = new ClassReader(bytecode);
        TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
        resultsReader.accept(visitor, ClassReader.EXPAND_FRAMES);
    }

    @Test
    public void testInjectTP() throws Exception {
        ClassReader reader = ASMUtils.readClass(TP.class);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        InjectCheckpointVisitor outer = new InjectCheckpointVisitor(writer, TP.class.getName(), () -> 1L, new HashMap<>(), new HashSet<>(), EnumSet.allOf(CheckpointInjectionPoint.class));
        reader.accept(outer, ClassReader.EXPAND_FRAMES);
        byte[] bytecode = writer.toByteArray();
        ClassReader resultsReader = new ClassReader(bytecode);
        TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
        resultsReader.accept(visitor, ClassReader.EXPAND_FRAMES);
    }

    public static void main(String[] args) throws Exception {
        ClassReader reader = new ClassReader(new FileInputStream("/tmp/io.netty.handler.codec.mqtt.MqttFixedHeader.class"));
        TraceClassVisitor visitor = new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out));
        CheckClassAdapter adapter = new CheckClassAdapter(visitor, true);
        reader.accept(adapter, ClassReader.EXPAND_FRAMES);
    }

}
