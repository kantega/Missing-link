package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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
        defaultPhase = LifecyclePhase.INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class FindTheMissingLinksMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> artifacts = project.getArtifacts();
        List<String> paths = new ArrayList<>(artifacts.size());
        for (Artifact dependencyArtifact : artifacts) {
            File file = dependencyArtifact.getFile();
            String fileName = file.getName();
            if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
                paths.add(file.getAbsolutePath());
            }
        }
        try {
            Report report = new ClassFileVisitor().generateReportForJar(paths);
            getLog().info(report.getMethodsMissing().toString());

        } catch (URISyntaxException | IOException e) {
            throw new MojoExecutionException("IO-problems", e);
        }
    }
}
