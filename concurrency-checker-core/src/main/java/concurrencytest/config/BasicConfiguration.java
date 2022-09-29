package concurrencytest.config;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

public record BasicConfiguration(Collection<Class<?>> classesToInstrument, Class<?> mainTestClass, File outputFolder) implements Configuration, Serializable {
}
