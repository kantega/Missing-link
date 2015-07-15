package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class FindTheMissingLinksMojoTest {

    @Before
    public void setup(){
        System.setProperty("maven.home", "/usr/lib/apache-maven");
    }

    @Test
    public void projectWithOptionalDependenciesDeclaredInPom() throws Exception {
        String absolutePath = new File("src/test/resources/unit/noerrors").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);
        verifier.executeGoal( "install" );

        verifier.verifyTextInLog("Running Find the missing link Maven plugin");
        verifier.verifyTextInLog("No missing methods");
        verifier.verifyTextInLog("No missing classes");
    }

    @Test
    public void projectWithOptionalDependenciesNOTDeclaredInPom() throws Exception {
        String absolutePath = new File("src/test/resources/unit/optionalDepsNotDeclared").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);
        verifier.executeGoal( "install" );

        verifier.verifyTextInLog("Running Find the missing link Maven plugin");
        verifier.verifyTextInLog("Missing classes: ");
        verifier.verifyTextInLog("Methods missing: ");
        verifier.verifyTextInLog("javax/servlet/ServletContextListener");
        verifier.verifyTextInLog("javax/portlet/ActionRequest");
        verifier.verifyTextInLog("org/apache/commons/io/FileCleaningTracker");
    }


}
