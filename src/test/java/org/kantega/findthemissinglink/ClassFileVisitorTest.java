package org.kantega.findthemissinglink;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ClassFileVisitorTest {
    @Test
    public void fileWithNoExternalDependenciesHaveNoErrors() throws IOException, URISyntaxException {
        String asmUrl = "http://opensource.kantega.no/nexus/service/local/repositories/central/content/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar";
        String filename = "asm-all-5.0.3.jar";
        File jarFile = getJarFile(asmUrl, filename);
        Report report = new ClassFileVisitor().generateReportForJar(singletonList(jarFile.getAbsolutePath()));
        Collection<String> methodsMissing = report.getMethodsMissing();
        try(Writer w = Files.newBufferedWriter(Paths.get("methodsvisited.txt"))){
            for (String s : report.getMethodsVisited()) {
                w.write(s);
                w.write('\n');
            }
        }
        try(Writer w = Files.newBufferedWriter(Paths.get("classesvisited.txt"))){
            for (String s : report.getClassesVisited()) {
                w.write(s);
                w.write('\n');
            }
        }
        assertThat(methodsMissing, is((Collection<String>)Collections.<String>emptyList()));
    }

    private File getJarFile(String asmUrl, String filename) throws IOException {
        URL url = new URL(asmUrl);
        File file = new File(System.getProperty("java.io.tmpdir"), filename);
        if (!file.exists()) {
            try(InputStream is = url.openStream(); OutputStream os = new FileOutputStream(file)){
                IOUtils.copy(is, os);
            }
        }
        return file;
    }
}
