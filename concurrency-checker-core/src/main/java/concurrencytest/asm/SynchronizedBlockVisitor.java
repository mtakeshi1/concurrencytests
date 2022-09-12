package concurrencytest.asm;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.MonitorCheckpoint;
import concurrencytest.util.ClassResolver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

//TODO not sure if we need a monitor match to filter out checkpoints
public class SynchronizedBlockVisitor extends BaseClassVisitor {

    public SynchronizedBlockVisitor(ClassVisitor classVisitor, CheckpointRegister checkpointRegister, Class<?> classUnderEnhancement, ClassResolver classResolver) {
        super(classVisitor, checkpointRegister, classUnderEnhancement, classResolver);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new SynchronizedBlockMethodVisitor(sourceName, delegate, access, name, descriptor);
    }

    private class SynchronizedBlockMethodVisitor extends BaseMethodVisitor {
        public SynchronizedBlockMethodVisitor(String sourceName, MethodVisitor delegate, int accessModifiers, String methodName, String descriptor) {
            super(delegate, checkpointRegister, sourceName, accessModifiers, methodName, descriptor);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.MONITORENTER) {
                monitorEnter();
            } else if (opcode == Opcodes.MONITOREXIT) {
                monitorExit();
            } else {
                super.visitInsn(opcode);
            }
        }

        private void monitorExit() {
            MonitorCheckpoint beforeCheckpoint = checkpointRegister.newMonitorExitCheckpoint(InjectionPoint.BEFORE, classUnderEnhancement, methodName, methodDescriptor, peekStackType(), sourceName, latestLineNumber, InjectionPoint.BEFORE);
            super.visitInsn(Opcodes.DUP);
            super.visitInsn(Opcodes.DUP);
            super.invokeGenericCheckpointWithContext(beforeCheckpoint);
            super.visitInsn(Opcodes.MONITOREXIT);
            MonitorCheckpoint after = checkpointRegister.newMonitorExitCheckpoint(InjectionPoint.AFTER, classUnderEnhancement, methodName, methodDescriptor, peekStackType(), sourceName, latestLineNumber, InjectionPoint.AFTER);
            super.invokeGenericCheckpointWithContext(after);

        }

        private void monitorEnter() {
            MonitorCheckpoint beforeCheckpoint = checkpointRegister.newMonitorEnterCheckpoint(InjectionPoint.BEFORE, classUnderEnhancement, methodName, methodDescriptor, peekStackType(), sourceName, latestLineNumber, InjectionPoint.BEFORE);
            super.visitInsn(Opcodes.DUP);
            super.visitInsn(Opcodes.DUP);
            super.invokeGenericCheckpointWithContext(beforeCheckpoint);
            super.visitInsn(Opcodes.MONITORENTER);
            MonitorCheckpoint after = checkpointRegister.newMonitorEnterCheckpoint(InjectionPoint.AFTER, classUnderEnhancement, methodName, methodDescriptor, peekStackType(), sourceName, latestLineNumber, InjectionPoint.AFTER);
            super.invokeGenericCheckpointWithContext(after);
        }

    }
}
