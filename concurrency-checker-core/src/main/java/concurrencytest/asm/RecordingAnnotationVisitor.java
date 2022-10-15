package concurrencytest.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RecordingAnnotationVisitor extends AnnotationVisitor {

    private final List<Consumer<AnnotationVisitor>> commands = new ArrayList<>();

    public RecordingAnnotationVisitor(int api, AnnotationVisitor annotationVisitor) {
        super(api, annotationVisitor);
    }

    protected RecordingAnnotationVisitor(int api) {
        super(api);
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        commands.add(av -> av.visit(name, value));
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        super.visitEnum(name, descriptor, value);
        commands.add(av -> av.visitEnum(name, descriptor, value));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        AnnotationVisitor delegate = super.visitAnnotation(name, descriptor);
        if (delegate == null) {
            return null;
        }
        RecordingAnnotationVisitor rec = new RecordingAnnotationVisitor(Opcodes.ASM9, delegate);
        commands.add(av -> {
            var inner = av.visitAnnotation(name, descriptor);
            if (inner != null) {
                rec.replay(inner);
            }
        });
        return rec;
    }

    private void replay(AnnotationVisitor av) {
        for (var cmd : commands) {
            cmd.accept(av);
        }
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor delegate = super.visitArray(name);
        if (delegate == null) {
            return null;
        }
        RecordingAnnotationVisitor rec = new RecordingAnnotationVisitor(Opcodes.ASM9, delegate);
        commands.add(av -> {
            var inner = av.visitArray(name);
            if (inner != null) {
                rec.replay(inner);
            }
        });
        return rec;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        commands.add(AnnotationVisitor::visitEnd);
    }
}
