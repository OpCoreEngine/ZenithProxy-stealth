package com.zenith.plugin.api;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.zenith.plugin.api.Plugin"})
public class PluginAnnotationProcessor extends AbstractProcessor {

    private ProcessingEnvironment environment;
    private String pluginClassFound;
    private boolean warnedAboutMultiplePlugins;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.environment = processingEnv;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized boolean process(Set<? extends TypeElement> annotations,
                                        RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Plugin.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                environment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with "
                        + Plugin.class.getCanonicalName());
                return false;
            }

            TypeElement typeElement = (TypeElement) element;
            Name qualifiedName = typeElement.getQualifiedName();

            if (Objects.equals(pluginClassFound, qualifiedName.toString())) {
                if (!warnedAboutMultiplePlugins) {
                    environment.getMessager()
                        .printMessage(Diagnostic.Kind.WARNING, "UNSUPPORTED: Multiple Plugin classes found");
                    warnedAboutMultiplePlugins = true;
                }
                return false;
            }

            Plugin plugin = element.getAnnotation(Plugin.class);

            if (!PluginInfo.ID_PATTERN.matcher(plugin.id()).matches()) {
                environment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Plugin ID '" + plugin.id() + "' is invalid. Must be all lowercase letters, numbers, '-', or '_' and start with a letter");
                return false;
            }

            if (!Version.validate(plugin.version())) {
                environment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Plugin version '" + plugin.version() + "' is invalid. Must match pattern: [0-9].[0-9].[0-9]");
                return false;
            }

            // All good, generate the plugin.json
            PluginInfo pluginJson = new PluginInfo(
                qualifiedName.toString(),
                plugin.id(),
                new Version(plugin.version()),
                plugin.description(),
                plugin.url(),
                Arrays.stream(plugin.authors()).filter(a -> !a.isBlank()).toList(),
                Arrays.stream(plugin.mcVersions()).filter(a -> !a.isBlank()).toList()
            );
            try {
                FileObject object = environment.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "zenithproxy.plugin.json");
                var objectMapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
                var pp = new DefaultPrettyPrinter();
                pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
                try (Writer writer = new BufferedWriter(object.openWriter())) {
                    objectMapper.writer(pp).writeValue(writer, pluginJson);
                }
                pluginClassFound = qualifiedName.toString();
            } catch (IOException e) {
                environment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Unable to generate plugin file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }
}

