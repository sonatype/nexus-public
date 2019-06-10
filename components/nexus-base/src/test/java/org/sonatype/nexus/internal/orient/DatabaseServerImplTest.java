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
package org.sonatype.nexus.internal.orient;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.entity.EntityHook;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class DatabaseServerImplTest
  extends TestSupport
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Mock
  public ApplicationDirectories applicationDirectories;

  @Mock
  public NodeAccess nodeAccess;

  @Mock
  public EventManager eventManager;

  private DatabaseServerImpl underTest;

  @Before
  public void setup() {
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(tempDir.getRoot());

    underTest = new DatabaseServerImpl(
        applicationDirectories,
        Collections.emptyList(),
        Collections.emptyList(),
        ClassLoader.getSystemClassLoader(),
        false, false, false,
        "2424-2430", "2480-2490",
        nodeAccess,
        new EntityHook(eventManager));
  }

  @Test
  public void testOnlyOurDatabasesAreReported() {
    List<String> candidates = asList(
        "audit",
        "analytics",
        "component",
        "component.orig",
        "component.bak",
        "config",
        "security",
        "accesslog",
        "OSystem");

    candidates.forEach(this::createDatabase);

    assertThat(underTest.databases(), containsInAnyOrder("component", "config", "security"));
  }

  @Test
  public void testNoDatabasesAreReportedForNewInstance() {

    // mimic new instance, don't create any databases

    assertThat(underTest.databases(), is(empty()));
  }

  @Test
  public void testDatabasesAreReportedAsTheyAppear() {

    assertThat(underTest.databases(), is(empty()));

    createDatabase("config");

    assertThat(underTest.databases(), containsInAnyOrder("config"));

    createDatabase("component");

    assertThat(underTest.databases(), containsInAnyOrder("component", "config"));

    createDatabase("security");

    assertThat(underTest.databases(), containsInAnyOrder("component", "config", "security"));
  }

  private void createDatabase(final String name) {
    try {
      File ocfSourceFile = new File(tempDir.newFolder(name), "database.ocf");
      Files.createFile(ocfSourceFile.toPath());
      assertThat(ocfSourceFile.exists(), is(true));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
