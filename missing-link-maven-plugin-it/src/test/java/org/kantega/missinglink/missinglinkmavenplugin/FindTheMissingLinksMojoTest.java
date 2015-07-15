package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FindTheMissingLinksMojoTest {

    @Before
    public void setup(){
        System.setProperty("maven.home", "/usr/lib/apache-maven");
    }

    @Test
    public void projectWithOptionalDependenciesDeclaredInPom() throws Exception {
        String absolutePath = new File("src/test/resources/unit/noerrors").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);
        verifier.executeGoal("install");

        String logText = getLogText(verifier);

        assertThat(logText, containsString("Running Find the missing link Maven plugin"));
        assertThat(logText, containsString("Ignoring Servlet API"));
        assertThat(logText, containsString("Ignoring Portlet API"));
        assertThat(logText, containsString("No missing methods"));
        assertThat(logText, containsString("No missing classes"));
    }

    @Test
    public void projectWithOptionalDependenciesNOTDeclaredInPom() throws Exception {
        String absolutePath = new File("src/test/resources/unit/optionalDepsNotDeclared").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);
        verifier.executeGoal( "install" );

        String logText = getLogText(verifier);
        assertThat(logText, containsString("Running Find the missing link Maven plugin"));
        assertThat(logText, containsString("Missing classes: "));
        assertThat(logText, containsString("Methods missing: "));
        assertThat(logText, containsString("Ignoring Servlet API"));
        assertThat(logText, containsString("Ignoring Portlet API"));
        assertThat(logText, containsString("org/apache/commons/io/FileCleaningTracker"));

        assertThat(logText, not(containsString("javax/servlet/ServletContextListener")));
        assertThat(logText, not(containsString("javax/portlet/ActionRequest")));
    }

    private String getLogText(Verifier verifier) throws IOException {
        File logFile = new File(verifier.getBasedir(), verifier.getLogFileName());
        List<String> logLines = Files.readAllLines(logFile.toPath());
        return join("\n", logLines);
    }
}
