package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.kantega.missinglink.findthemissinglink.ClassFileVisitor;
import org.kantega.missinglink.findthemissinglink.Report;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mojo( name = "findmissinglinks",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class FindTheMissingLinksMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Determines whether optional dependencies should be included when scanning the class path.
     * Default value is true, so transitive dependencies that are marked as optional is not included
     * if not explicitly declared.
     */
    @Parameter(defaultValue = "false", property = "includeOptional")
    private boolean includeOptional;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Running Find the missing link Maven plugin");
        log.info("Include optional dependencies: " + includeOptional);
        Set<Artifact> artifacts = project.getArtifacts();
        List<String> paths = new ArrayList<>(artifacts.size());
        for (Artifact dependencyArtifact : artifacts) {
            boolean ignoreDependency = dependencyArtifact.isOptional() && !includeOptional;

            if (!ignoreDependency) {
                File file = dependencyArtifact.getFile();
                addIfJarOrWar(paths, file);
            } else {
                if(log.isDebugEnabled()){
                    log.debug("Excluding " + dependencyArtifact);
                }
            }
        }

        if(log.isDebugEnabled()){
            log.debug("Using dependencies: " + paths);
        }
        try {
            Report report = new ClassFileVisitor().generateReportForJar(paths);

            Set<String> methodsMissing = report.getMethodsMissing();
            if(methodsMissing.isEmpty()){
                log.info("No missing methods");
            } else {
                log.warn("Methods missing: " + methodsMissing);
            }

            Set<String> classesMissing = report.getClassesMissing();
            if(classesMissing.isEmpty()){
                log.info("No missing classes");
            } else {
                log.warn("Missing classes: " + classesMissing);
            }

        } catch (URISyntaxException | IOException e) {
            throw new MojoExecutionException("IO-problems", e);
        }
    }

    private void addIfJarOrWar(List<String> paths, File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
            paths.add(file.getAbsolutePath());
        }
    }
}
