package org.kantega.missinglink.missinglinkmavenplugin;

import org.apache.maven.it.Verifier;
import org.junit.Test;

import java.io.File;

public class FindTheMissingLinksMojoTest {

    @Test
    public void projectWithOptionalDependenciesDeclaredInPom() throws Exception {
        System.setProperty("maven.home", "/usr/lib/apache-maven");

        String absolutePath = new File("src/test/resources/unit/noerrors").getAbsolutePath();
        Verifier verifier  = new Verifier(absolutePath);
        verifier.executeGoal( "install" );

        verifier.verifyTextInLog("Running Find the missing link Maven plugin");
        verifier.verifyTextInLog("No missing methods");
        verifier.verifyTextInLog("No missing classes");
    }

}
