package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class FindTheMissingLinksMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running Find the missing link Maven plugin");

        Set<Artifact> artifacts = project.getArtifacts();
        List<String> paths = new ArrayList<>(artifacts.size());
        for (Artifact dependencyArtifact : artifacts) {
            File file = dependencyArtifact.getFile();
            addIfJarOrWar(paths, file);
        }
        addIfJarOrWar(paths, project.getArtifact().getFile());

        try {
            Report report = new ClassFileVisitor().generateReportForJar(paths);

            Set<String> methodsMissing = report.getMethodsMissing();
            if(methodsMissing.isEmpty()){
                getLog().info("No missing methods");
            } else {
                getLog().warn("Methods missing: " + methodsMissing);
            }

            Set<String> classesMissing = report.getClassesMissing();
            if(classesMissing.isEmpty()){
                getLog().info("No missing classes");
            } else {
                getLog().warn("Missing classes: " + classesMissing);
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
