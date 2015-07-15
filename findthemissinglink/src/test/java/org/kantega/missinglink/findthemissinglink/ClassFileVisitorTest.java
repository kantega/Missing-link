package org.kantega.missinglink.findthemissinglink;

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
import java.util.Collections;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class ClassFileVisitorTest {
    @Test
    public void fileWithNoExternalDependenciesHaveNoErrors() throws IOException, URISyntaxException {
        String asmUrl = "http://opensource.kantega.no/nexus/service/local/repositories/central/content/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar";
        String filename = "asm-all-5.0.3.jar";
        File jarFile = getJarFile(asmUrl, filename);
        Report report = new ClassFileVisitor().generateReportForJar(singletonList(jarFile.getAbsolutePath()));
        //writeReport(report);
        assertThat(report.getMethodsMissing(), is(Collections.<String>emptySet()));
        assertThat(report.getClassesMissing(), is(Collections.<String>emptySet()));
    }

    @Test
    public void fileWithDependencyHaveMissingMethodsWhenDependencyNotIncluded() throws IOException, URISyntaxException {
        // commons-dbcp-1.4 depends on commons-pool:commons-pool:1.5.4 and org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1 (optional)
        File dbcpFile = getJarFile("http://opensource.kantega.no/nexus/service/local/repositories/central/content/commons-dbcp/commons-dbcp/1.4/commons-dbcp-1.4.jar", "commons-dbcp-1.4.jar");
        Report report = new ClassFileVisitor().generateReportForJar(singletonList(dbcpFile.getAbsolutePath()));

        assertThat(report.getMethodsMissing(), hasItems("org/apache/commons/pool/impl/GenericKeyedObjectPool.setMaxIdle(I)V", "javax/transaction/Transaction.getStatus()I"));

        File poolFile = getJarFile("http://opensource.kantega.no/nexus/service/local/repositories/central/content/commons-pool/commons-pool/1.5.4/commons-pool-1.5.4.jar", "commons-pool-1.5.4.jar");
        report = new ClassFileVisitor().generateReportForJar(asList(dbcpFile.getAbsolutePath(), poolFile.getAbsolutePath()));

        Set<String> methodsMissing = report.getMethodsMissing();
        Set<String> classesMissing = report.getClassesMissing();

        assertThat(methodsMissing, hasItems("javax/transaction/Transaction.getStatus()I"));
        assertThat(classesMissing, hasItems("javax/transaction/Transaction"));
        assertThat(methodsMissing, not(hasItems("org/apache/commons/pool/impl/GenericKeyedObjectPool.setMaxIdle(I)V")));
        assertThat(classesMissing, not(hasItems("org/apache/commons/pool/impl/GenericKeyedObjectPool")));

        File geronimoFile = getJarFile("http://opensource.kantega.no/nexus/service/local/repositories/central/content/org/apache/geronimo/specs/geronimo-jta_1.1_spec/1.1.1/geronimo-jta_1.1_spec-1.1.1.jar", "geronimo-jta_1.1_spec-1.1.1.jar");
        report = new ClassFileVisitor().generateReportForJar(asList(dbcpFile.getAbsolutePath(), poolFile.getAbsolutePath(), geronimoFile.getAbsolutePath()));

        methodsMissing = report.getMethodsMissing();
        classesMissing = report.getClassesMissing();

        assertThat(methodsMissing, not(hasItems("javax/transaction/Transaction.getStatus()I")));
        assertThat(methodsMissing, not(hasItems("org/apache/commons/pool/impl/GenericKeyedObjectPool.setMaxIdle(I)V")));
        assertThat(methodsMissing, is(Collections.<String>emptySet()));
        assertThat(classesMissing, is(Collections.<String>emptySet()));
    }

    @Test
    public void missingPackageIgnored() throws IOException, URISyntaxException {
        // commons-dbcp-1.4 depends on commons-pool:commons-pool:1.5.4 and org.apache.geronimo.specs:geronimo-jta_1.1_spec:1.1.1 (optional)
        File dbcpFile = getJarFile("http://opensource.kantega.no/nexus/service/local/repositories/central/content/commons-dbcp/commons-dbcp/1.4/commons-dbcp-1.4.jar", "commons-dbcp-1.4.jar");
        Report report = new ClassFileVisitor().generateReportForJar(singletonList(dbcpFile.getAbsolutePath()), singletonList("javax/transaction"));

        assertThat(report.getMethodsMissing(), not(hasItems("javax/transaction/Transaction.getStatus()I")));
        assertThat(report.getMethodsMissing(), hasItems("org/apache/commons/pool/impl/GenericKeyedObjectPool.setMaxIdle(I)V"));


    }

    private void writeReport(Report report) throws IOException {
        writeLines("methodsvisited.txt", report.getMethodsVisited());
        writeLines("classesvisited.txt", report.getClassesVisited());
        writeLines("missing.txt", report.getMethodsMissing());
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
