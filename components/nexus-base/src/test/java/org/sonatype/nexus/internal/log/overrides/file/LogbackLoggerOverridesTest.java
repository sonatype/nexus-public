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
package org.sonatype.nexus.internal.log.overrides.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.log.LoggerLevel;

import ch.qos.logback.classic.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Tests for {@link LogbackLoggerOverrides}.
 */
public class LogbackLoggerOverridesTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File file;

  private LogbackLoggerOverrides underTest;

  @Before
  public void setUp() throws Exception {
    file = temporaryFolder.newFile("logback-overrides.tmp");
    underTest = new LogbackLoggerOverrides(file);
  }

  @Test
  public void testSave_writtenInLogbackXMLFormat() throws Exception {
    underTest.set(Logger.ROOT_LOGGER_NAME, LoggerLevel.WARN);
    underTest.set("foo", LoggerLevel.ERROR);
    underTest.set("bar", LoggerLevel.INFO);
    underTest.save();

    assertThat(file.exists(), is(true));

    String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

    log("XML:\n{}", xml);

    assertThat(xml, is(getExpectedXml(false)));
  }

  @Test
  public void testLoad_fromLogbackXMLFormat() throws Exception {
    String xmlString =
        "<included>" +
            System.lineSeparator() +
            "<logger name=\"foo\" level=\"ERROR\"/>" +
            System.lineSeparator() +
            "<logger name=\"bar\" level=\"INFO\"/>" +
            System.lineSeparator() +
            "</included>";
    Files.write(file.toPath(), xmlString.getBytes(StandardCharsets.UTF_8));

    log("XML:\n{}", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));

    underTest.load();

    assertThat(underTest.get("foo"), is(LoggerLevel.ERROR));
    assertThat(underTest.get("bar"), is(LoggerLevel.INFO));
  }

  @Test
  public void testReset_removesLoggerElements() throws IOException {
    underTest.set("foo", LoggerLevel.ERROR);
    underTest.set("bar", LoggerLevel.INFO);
    underTest.save();
    assertThat(file.exists(), is(true));

    underTest.reset();
    assertThat(file.exists(), is(true));

    String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

    log("XML:\n{}", xml);

    assertThat(xml, is(getExpectedXml(true)));
  }

  private String getExpectedXml(boolean afterReset) {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version='1.0' encoding='UTF-8'?>");
    xml.append(System.lineSeparator());
    xml.append(System.lineSeparator());
    xml.append("<!--");
    xml.append(System.lineSeparator());
    xml.append("DO NOT EDIT - Automatically generated; User-customized logging levels");
    xml.append(System.lineSeparator());
    xml.append("-->");
    xml.append(System.lineSeparator());
    xml.append(System.lineSeparator());
    xml.append("<included>");
    xml.append(System.lineSeparator());
    if (!afterReset) {
      xml.append("  <logger name='bar' level='INFO'/>");
      xml.append(System.lineSeparator());
      xml.append("  <property name='root.level' value='WARN'/>");
      xml.append(System.lineSeparator());
      xml.append("  <logger name='foo' level='ERROR'/>");
      xml.append(System.lineSeparator());
    }
    xml.append("</included>");
    xml.append(System.lineSeparator());

    return xml.toString();
  }
}
