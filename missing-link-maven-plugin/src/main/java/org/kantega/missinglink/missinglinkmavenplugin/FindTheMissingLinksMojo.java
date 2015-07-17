package org.kantega.missinglink.missinglinkmavenplugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo( name = "findmissinglinks",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class FindTheMissingLinksMojo extends AbstractMojo {

    public static final String MISSING_LINKS_REPORT = "missing-links-report.txt";
    public static final String MISSING_CLASS_REFERENCES = "missing-classes.json";
    public static final String MISSING_METHOD_REFERENCES = "missing-methods.json";

    public static final String METHOD_REFERENCED = "methods-referenced.txt";
    public static final String CLASSES_REFERENCED = "classes-referenced.txt";
    public static final String METHOD_VISITED = "methods-visited.txt";
    public static final String CLASSES_VISITED = "classes-visited.txt";

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

    /**
     * Directory where report files will be written.
     */
    @Parameter( defaultValue = "${project.build.directory}/missing-link")
    private File reportDirectory;

    /**
     * If this parameter is true, all classes and methods visited and referenced is written to
     * their respetive files in the report directory.
     */
    @Parameter(defaultValue = "false")
    private boolean writeSeenAndVisitedToFile;

    /**
     * Classes and methods starting with any of these packages will not be reported as missing.
     * Both «/» and «.» may be used as package separator.
     */
    @Parameter
    private List<String> ignoredPackages = Collections.emptyList();

    /**
     * Ignore missing classes and methods that are referenced in the listed packages.
     * Both «/» and «.» may be used as package separator.
     */
    @Parameter
    private List<String> ignoreReferencesInPackages = Collections.emptyList();

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
            Report report = new ClassFileVisitor().generateReportForJar(paths, ignoredPackages, ignoreReferencesInPackages);

            Map<String, Set<String>> methodsMissing = report.getMethodsMissing();
            if(methodsMissing.isEmpty()){
                log.info("No missing methods");
            } else {
                log.warn("Missing methods detected. Reports can be found in " + reportDirectory.getAbsolutePath());
            }

            Map<String, Set<String>> classesMissing = report.getClassesMissing();
            if(classesMissing.isEmpty()){
                log.info("No missing classes");
            } else {
                log.warn("Missing classes detected. Reports can be found in " + reportDirectory.getAbsolutePath());
            }
            writeReport(report, classesMissing, methodsMissing);

        } catch (Exception e) {
            throw new MojoExecutionException("IO-problems", e);
        }
    }

    private void writeReport(Report report, Map<String, Set<String>> classesMissing, Map<String, Set<String>> methodsMissing) throws IOException {
        reportDirectory.mkdirs();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(reportDirectory, MISSING_LINKS_REPORT)))){
            writer.write("Find the missing links Maven plugin report");
            writer.newLine();
            writer.newLine();

            writer.write("Packages ignored:");
            writer.newLine();
            for (String ignoredPackage : getIgnoredPackages()) {
                writer.write("   " + ignoredPackage);
                writer.newLine();
            }
            writer.newLine();
            if(!classesMissing.isEmpty()){
                writer.write("Classes missing (see also " + MISSING_CLASS_REFERENCES + "):");
                writer.newLine();

                for (String missingClass : classesMissing.keySet()) {
                    writer.write("   " + missingClass);
                    writer.newLine();
                }
            } else {
                writer.write("No classes missing");
            }

            writer.newLine();
            if(!methodsMissing.isEmpty()){
                writer.write("Methods missing (see also " + MISSING_METHOD_REFERENCES + "):");
                writer.newLine();

                for (String missingMethod : methodsMissing.keySet()) {
                    writer.write("   " + missingMethod);
                    writer.newLine();
                }
            } else {
                writer.write("No methods missing");
            }
        }
        writeJsonToFile(classesMissing, new File(reportDirectory, MISSING_CLASS_REFERENCES));
        writeJsonToFile(methodsMissing, new File(reportDirectory, MISSING_METHOD_REFERENCES));

        if(writeSeenAndVisitedToFile){
            writeToFile(CLASSES_REFERENCED, report.getClassesReferenced());
            writeToFile(METHOD_REFERENCED, report.getMethodsReferenced());
            writeToFile(CLASSES_VISITED, report.getClassesVisited());
            writeToFile(METHOD_VISITED, report.getMethodsVisited());
        }
    }

    private void writeToFile(String targetFile, Set<String> entries) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(reportDirectory, targetFile)))) {
            for (String referencedClass : entries) {
                writer.write(referencedClass);
                writer.newLine();
            }
        }
    }

    private void writeJsonToFile(Map<String, Set<String>> methodsMissing, File resultFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(resultFile, methodsMissing);
    }

    private List<String> getIgnoredPackages() {
        Set<String> ignoredPackages = new HashSet<>(this.ignoredPackages.stream()
                .map(s -> s.replace('.', '/'))
                .collect(Collectors.toList()));
        if(ignoreServletApi){
            ignoredPackages.add("javax/servlet");
        }
        if(ignorePortletApi){
            ignoredPackages.add("javax/portlet");
        }
        return new ArrayList<>(ignoredPackages);
    }

    private void addIfJarOrWar(List<String> paths, File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
            paths.add(file.getAbsolutePath());
        }
    }
}
