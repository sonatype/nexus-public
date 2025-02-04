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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.io.DirectoryHelper.mkdir;

public class JvmLogCustomizerTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryWorkDirectory = new TemporaryFolder();

  @Mock
  private LogManager mockLogManager;

  @Mock
  private ApplicationDirectories mockApplicationDirectories;

  private JvmLogCustomizer underTest;

  @Before
  public void setup() throws IOException {
    when(mockApplicationDirectories.getWorkDirectory()).thenReturn(temporaryWorkDirectory.getRoot());
    mkdir(temporaryWorkDirectory.getRoot(), "log");
    underTest = new JvmLogCustomizer(mockLogManager);
  }

  @Test
  public void customizeJvmLog() throws Exception {
    File file = createLogFile();
    when(mockLogManager.getLogFile(anyString())).thenReturn(file);
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("-Dnexus.password=nxrm -Dnexus.token=123");
    }
    SupportBundle supportBundle = new SupportBundle();
    underTest.customize(supportBundle);

    List<ContentSource> list = supportBundle.getSources();
    assertThat(list.size(), equalTo(1));

    ContentSource contentSource = list.get(0);
    contentSource.prepare();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(contentSource.getContent()))) {
      assertThat(reader.readLine(), equalTo("-Dnexus.password=**** -Dnexus.token=****"));
    }
  }

  private File createLogFile() throws IOException {
    File file = new File(temporaryWorkDirectory.getRoot(), "/log/jvm.log");
    file.createNewFile();
    file.setLastModified(ZonedDateTime.now().minusMinutes(0L).toInstant().toEpochMilli());
    return file;
  }

}
