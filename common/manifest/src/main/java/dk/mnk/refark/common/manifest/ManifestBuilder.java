package dk.mnk.refark.common.manifest;

import com.google.auto.service.AutoService;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.*;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("org.springframework.boot.autoconfigure.SpringBootApplication")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class ManifestBuilder extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element application : roundEnv.getElementsAnnotatedWith(annotation)) {
                try {
                    FileObject manifestFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "manifest.yaml", application);
                    String deploymentName = application.getSimpleName().toString().toLowerCase(Locale.ROOT);
                    try (Writer manifestWriter = manifestFile.openWriter()) {
                        var appLabel= Map.of("app", deploymentName);
                        Deployment deployment = new DeploymentBuilder()
                                .withNewMetadata()
                                    .withName(deploymentName)
                                    .withLabels(appLabel)
                                .endMetadata()
                                .withNewSpec()
                                    .withSelector(new LabelSelector(null, appLabel))
                                    .withNewTemplate()
                                        .withNewMetadata()
                                            .withLabels(appLabel)
                                        .endMetadata()
                                        .withNewSpec()
                                            .addNewContainer().withName(deploymentName).withImage("localhost:5000/app1").endContainer()
                                        .endSpec()
                                    .endTemplate()
                                .endSpec()
                                .build();
                        manifestWriter.append(SerializationUtils.dumpAsYaml(deployment));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }

}
