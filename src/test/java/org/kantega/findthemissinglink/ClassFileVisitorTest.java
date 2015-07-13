package org.kantega.findthemissinglink;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
        writeLines("methodsvisited.txt", report.getMethodsVisited());
        writeLines("classesvisited.txt", report.getClassesVisited());
        writeLines("missing.txt", new LinkedHashSet<>(methodsMissing));
        assertThat(methodsMissing, is((Collection<String>)Collections.<String>emptyList()));
    }

    private void writeLines(String file, Set<String> content) throws IOException {
        try(Writer w = Files.newBufferedWriter(Paths.get(file))){
            for (String s : content) {
                w.write(s);
                w.write('\n');
            }
        }
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
