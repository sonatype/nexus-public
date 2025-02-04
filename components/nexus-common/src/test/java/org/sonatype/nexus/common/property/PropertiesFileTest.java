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
package org.sonatype.nexus.common.property;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;

import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PropertiesFile}
 */
public class PropertiesFileTest
    extends TestSupport
{
  private File file;

  private PropertiesFile underTest;

  @Before
  public void setUp() {
    file = util.createTempFile();
    log("File: $file");
    underTest = new PropertiesFile(file);
  }

  @Test
  public void testStoreProperties() throws IOException {
    underTest.setProperty("foo", "bar");
    underTest.store();
    assertTrue(file.exists());
    assertThat(Files.readString(file.toPath()).length(), not(is(0)));

    // expect the first "comment" line of the file to contain a Date
    String firstLine = Files.readAllLines(file.toPath()).get(0);
    assertTrue(
        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSSZ").parseDateTime(firstLine.substring(1)).isBeforeNow());

    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file.toPath())) {
      props.load(in);
    }

    assertThat(props.keySet(), hasSize(1));
    assertThat(props.getProperty("foo"), is("bar"));
  }

  @Test
  public void testStoreProperties_withExplicitComment() throws IOException {
    String comment = "At the hundredth meridian where the great plains begin";
    underTest.store(comment);
    assertTrue(file.exists());
    assertThat(Files.readString(file.toPath()).length(), not(is(0)));

    // expect the first "comment" line of the file to contain the specified message
    String firstLine = Files.readAllLines(file.toPath()).get(0);
    assertThat(firstLine, is("#" + comment));
  }

  @Test
  public void testLoadProperties() throws IOException {
    Properties props = new Properties();
    props.setProperty("foo", "bar");

    try (OutputStream out = Files.newOutputStream(file.toPath())) {
      props.store(out, null);
    }

    underTest.load();
    assertThat(props.keySet(), hasSize(1));
    assertThat(props.getProperty("foo"), is("bar"));
  }
}
