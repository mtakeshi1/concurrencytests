package concurrencytest.config;

//TODO sketch
public interface ConfigurationBuilder {


    ConfigurationBuilder autodetectClassesToInject(Class<?> from);

    ConfigurationBuilder includeJDKClasses();

    ConfigurationBuilder addClasesToInject(Class<?>...classes);



}
