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
    }

    @Test
    public void projectWithOptionalDependenciesDefault() throws Exception {
        String absolutePath = new File("src/test/resources/unit/optionalDepsNotDeclared").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);

        verifier.executeGoal("install");

        verifier.verifyTextInLog("Running Find the missing link Maven plugin");
        verifier.verifyTextInLog("Missing classes: ");
        verifier.verifyTextInLog("Methods missing: ");
    }

    @Test
    public void projectWithOptionalDependenciesNOTIncluded() throws Exception {
        String absolutePath = new File("src/test/resources/unit/optionalDepsNotDeclared").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);

        verifier.setSystemProperty("includeOptional", "false");
        verifier.executeGoal("install");

        verifier.verifyTextInLog("Running Find the missing link Maven plugin");
        verifier.verifyTextInLog("Missing classes: ");
        verifier.verifyTextInLog("Methods missing: ");
    }
}
