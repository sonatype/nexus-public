/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.mime;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.view.ContentTypes;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultContentValidatorTest
{
  private DefaultContentValidator testSubject;

  private final byte[] emptyZip = {80, 75, 5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  @Before
  public void setUp() {
    testSubject = new DefaultContentValidator(new DefaultMimeSupport());
  }

  private InputStreamSupplier supplier(byte[] bytes) {
    return () -> new ByteArrayInputStream(bytes);
  }

  @Test
  public void simpleTextNonStrictWithDeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        ContentTypes.TEXT_PLAIN);
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void simpleTextNonStrictWithUndeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        null);
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void simpleTextNonStrictWithWrongDeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        "application/zip");
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void simpleTextStrictWithWrongDeclared() throws IOException {
    String type = testSubject.determineContentType(
        true,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        "application/zip");
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void simpleZipNonStrictWithUndeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.zip",
        null);
    assertThat(type, equalTo("application/zip"));
  }

  @Test
  public void simpleZipNonStrictWithDeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.zip",
        "application/zip");
    assertThat(type, equalTo("application/zip"));
  }

  @Test
  public void simpleZipStrictWithDeclared() throws IOException {
    String type = testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.zip",
        "application/zip");
    assertThat(type, equalTo("application/zip"));
  }

  @Test
  public void simpleZipStrictWithWrongDeclared() throws IOException {
    String type = testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.zip",
        ContentTypes.TEXT_PLAIN);
    assertThat(type, equalTo("application/zip"));
  }

  @Test(expected = InvalidContentException.class)
  public void strictWrongZipContentAsText() throws IOException {
    testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.txt",
        ContentTypes.TEXT_PLAIN);
  }

  @Test(expected = InvalidContentException.class)
  public void strictWrongTextContentAsZip() throws IOException {
    testSubject.determineContentType(
        true,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.zip",
        "application/zip");
  }

  @Test
  public void nonStrictWrongZipContentAsText() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.txt",
        ContentTypes.TEXT_PLAIN);
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void nonStrictWrongTextContentAsZip() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.zip",
        "application/zip");
    assertThat(type, equalTo("application/zip"));
  }

  @Test(expected = InvalidContentException.class)
  public void strictWrongZipContentAsTextUndeclared() throws IOException {
    testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.txt",
        null);
  }

  @Test(expected = InvalidContentException.class)
  public void strictWrongTextContentAsZipUndeclared() throws IOException {
    testSubject.determineContentType(
        true,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.zip",
        null);
  }

  @Test
  public void nonStrictWrongZipContentAsTextUndeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        "test.txt",
        null);
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test
  public void nonStrictWrongTextContentAsZipUndeclared() throws IOException {
    String type = testSubject.determineContentType(
        false,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.zip",
        null);
    assertThat(type, equalTo("application/zip"));
  }

  @Test
  public void declaredCharsetMissing() throws IOException {
    String type = testSubject.determineContentType(
        true,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        ContentTypes.TEXT_PLAIN + "; charset=");
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN));
  }

  @Test(expected = InvalidContentException.class)
  public void completelyInvalid() throws IOException {
    testSubject.determineContentType(
        true,
        supplier("simple text".getBytes()),
        MimeRulesSource.NOOP,
        "test.txt",
        "@#$*(#&%$*(%)k;lasj;klfjsdfas");
  }

  @Test
  public void strictMavenPomHavingContentBodyWithNoXmlDeclarationAndContainingTheTextHtml() throws IOException {
    String content = """
        <project xmlns="http://maven.apache.org/POM/4.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
          <properties>
            <htmlunit.version>2.4</htmlunit.version>
          </properties>
        </project>
        """;

    String type = testSubject.determineContentType(
        true,
        supplier(content.getBytes()),
        MimeRulesSource.NOOP,
        "org/jboss/weld/weld-core-parent/1.1.12.Final/weld-core-parent-1.1.12.Final.pom.xml",
        "text/xml");
    assertThat(type, equalTo(ContentTypes.APPLICATION_XML));
  }

  @Test
  public void binaryContent() throws IOException {
    byte[] binaryFile = {1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    String type = testSubject.determineContentType(
        false,
        supplier(binaryFile),
        MimeRulesSource.NOOP,
        "vim",
        null);
    assertThat(type, equalTo(ContentTypes.APPLICATION_OCTET_STREAM));
  }

  @Test(expected = InvalidContentException.class)
  public void binaryContentStrict() throws IOException {
    byte[] binaryFile = {1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    testSubject.determineContentType(
        true,
        supplier(binaryFile),
        MimeRulesSource.NOOP,
        "vim",
        null);
  }

  @Test
  public void pieExecutableStrictWithSharedlib() throws IOException {
    // ELF header for 64-bit LSB PIE executable (detected as application/x-sharedlib)
    byte[] elfPieExecutable = {
        0x7F, 'E', 'L', 'F', 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 62, 0, 1, 0, 0, 0
    };

    // Test that PIE executables (detected as application/x-sharedlib) are allowed with .exe extension
    String type = testSubject.determineContentType(
        true,
        supplier(elfPieExecutable),
        MimeRulesSource.NOOP,
        "protoc-3.22.0-linux-x86_64.exe",
        null);
    // Should not throw InvalidContentException and should return first expected type
    assertThat(type, equalTo("application/x-executable"));
  }

  @Test
  public void machoExecutableStrictWithMachO() throws IOException {
    // Mach-O header for 64-bit ARM little-endian macOS executable (detected as application/x-mach-o-executable)
    byte[] machoExecutable = {
        (byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE, // magic: MH_CIGAM_64 (64-bit LE)
        (byte) 0x0C, 0x00, 0x00, 0x01, // cputype: ARM64
        0x00, 0x00, 0x00, 0x00, // cpusubtype
        0x02, 0x00, 0x00, 0x00, // filetype: MH_EXECUTE
        0x00, 0x00, 0x00, 0x00, // ncmds
        0x00, 0x00, 0x00, 0x00 // sizeofcmds
    };

    // Test that macOS Mach-O ARM64 executables are allowed with .exe extension (NEXUS-51407)
    String type = testSubject.determineContentType(
        true,
        supplier(machoExecutable),
        MimeRulesSource.NOOP,
        "protoc-4.33.5-osx-aarch_64.exe",
        null);
    // Should not throw InvalidContentException and should return first expected type
    assertThat(type, equalTo("application/x-executable"));
  }
}
