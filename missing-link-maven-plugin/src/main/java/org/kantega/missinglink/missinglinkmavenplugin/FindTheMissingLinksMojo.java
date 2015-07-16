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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Most webapplication have the Servlet API as a dependency. It should be declared with scope «provided»,
     * and should not be found on the build classpath.
     * This parameter determines whether classes in package «javax.servlet» should be ignored.
     * Default value is true.
     */
    @Parameter(defaultValue = "true")
    private boolean ignoreServletApi;

    /**
     * When a project depends on the Portlet API, it should be declared with scope «provided»,
     * and should not be found on the build classpath.
     * This parameter determines whether classes in package «javax.portlet» should be ignored.
     * Default value is true.
     */
    @Parameter(defaultValue = "true")
    private boolean ignorePortletApi;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Running Find the missing link Maven plugin");
        if(ignoreServletApi) log.info("Ignoring Servlet API");
        if(ignorePortletApi) log.info("Ignoring Portlet API");
        try {
            Set<Artifact> artifacts = project.getArtifacts();
            List<String> paths = new ArrayList<>(artifacts.size());
            for (Artifact dependencyArtifact : artifacts) {
                    File file = dependencyArtifact.getFile();
                    addIfJarOrWar(paths, file);
            }

            if(log.isDebugEnabled()){
                log.debug("Using dependencies: " + paths);
            }
            List<String> ignoredPackages = getIgnoredPackages();
            Report report = new ClassFileVisitor().generateReportForJar(paths, ignoredPackages);

            Map<String, Set<String>> methodsMissing = report.getMethodsMissing();
            if(methodsMissing.isEmpty()){
                log.info("No missing methods");
            } else {
                log.warn("Methods missing: " + methodsMissing);
            }

            Map<String, Set<String>> classesMissing = report.getClassesMissing();
            if(classesMissing.isEmpty()){
                log.info("No missing classes");
            } else {
                log.warn("Missing classes: " + classesMissing);
            }

        } catch (Exception e) {
            throw new MojoExecutionException("IO-problems", e);
        }
    }

    private List<String> getIgnoredPackages() {
        List<String> ignoredPackages = new ArrayList<>();
        if(ignoreServletApi){
            ignoredPackages.add("javax/servlet");
        }
        if(ignorePortletApi){
            ignoredPackages.add("javax/portlet");
        }
        return ignoredPackages;
    }

    private void addIfJarOrWar(List<String> paths, File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
            paths.add(file.getAbsolutePath());
        }
    }
}
