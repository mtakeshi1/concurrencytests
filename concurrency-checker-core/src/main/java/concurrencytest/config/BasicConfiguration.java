package concurrencytest.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BasicConfiguration implements Configuration {
    private final Collection<Class<?>> classesToInstrument;
    private final Class<?> mainTestClass;
    private final File outputFolder;

    public BasicConfiguration(Collection<Class<?>> classesToInstrument, Class<?> mainTestClass, File outputFolder) {
        this.classesToInstrument = classesToInstrument;
        this.mainTestClass = mainTestClass;
        this.outputFolder = outputFolder;
    }

    public BasicConfiguration(Collection<Class<?>> classesToInstrument, Class<?> mainTestClass) {
        this(classesToInstrument, mainTestClass, temporaryFolder(mainTestClass.getName()));
    }

    public BasicConfiguration(Class<?> mainTestClass) {
        this(List.of(mainTestClass), mainTestClass);
    }

    private static File temporaryFolder(String clName) {
        String substring = clName.substring(clName.lastIndexOf('.') + 1);
        try {
            return Files.createTempDirectory("test_" + substring).toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static BasicConfiguration singleTestClass(Class<?> testClass) {
        return new BasicConfiguration(List.of(testClass), testClass);
    }

    public Collection<Class<?>> classesToInstrument() {
        return classesToInstrument;
    }

    public Class<?> mainTestClass() {
        return mainTestClass;
    }

    public File outputFolder() {
        return outputFolder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BasicConfiguration) obj;
        return Objects.equals(this.classesToInstrument, that.classesToInstrument) &&
                Objects.equals(this.mainTestClass, that.mainTestClass) &&
                Objects.equals(this.outputFolder, that.outputFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classesToInstrument, mainTestClass, outputFolder);
    }

    @Override
    public String toString() {
        return "BasicConfiguration[" +
                "classesToInstrument=" + classesToInstrument + ", " +
                "mainTestClass=" + mainTestClass + ", " +
                "outputFolder=" + outputFolder + ']';
    }

}
