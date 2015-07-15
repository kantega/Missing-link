package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.kantega.missinglink.findthemissinglink.ClassFileVisitor;
import org.kantega.missinglink.findthemissinglink.Report;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

@Mojo( name = "findmissinglinks",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class FindTheMissingLinksMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Component
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession mavenSession;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Running Find the missing link Maven plugin");

        try {

            List<String> scopes = asList(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);
            Set<Artifact> resolve = projectDependenciesResolver.resolve(project, scopes, scopes, mavenSession);

            Set<Artifact> artifacts = project.getArtifacts();
            List<String> paths = new ArrayList<>(artifacts.size());
            for (Artifact dependencyArtifact : artifacts) {
                    File file = dependencyArtifact.getFile();
                    addIfJarOrWar(paths, file);
            }

            if(log.isDebugEnabled()){
                log.debug("Using dependencies: " + paths);
            }
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

        } catch (Exception e) {
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
