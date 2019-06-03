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
package org.sonatype.nexus.repository.golang.internal.util;

import java.io.InputStream;

import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.view.Payload.UNKNOWN_SIZE;

public class CompressedContentExtractorTest
{
  private static final String SONATYPE_ZIP = "sonatype.zip";

  private static final String ANYPATH = "anypath";

  private static final String GO_MOD = "go.mod";

  private CompressedContentExtractor underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new CompressedContentExtractor();
  }

  @Test
  public void canExtractFile() throws Exception {
    InputStream project = getClass().getResourceAsStream(SONATYPE_ZIP);
    InputStream goMod = getClass().getResourceAsStream(GO_MOD);

    InputStream result = underTest.extractFile(project, GO_MOD);

    String goModFromProject = new ByteSource()
    {
      @Override
      public InputStream openStream() {
        return result;
      }
    }.asCharSource(Charsets.UTF_8).read();

    String goModAsString = new ByteSource()
    {
      @Override
      public InputStream openStream() {
        return goMod;
      }
    }.asCharSource(Charsets.UTF_8).read();

    assertThat(goModFromProject, is(equalTo(goModAsString)));
  }

  @Test
  public void entryNotFound() {
    InputStream project = getClass().getResourceAsStream(SONATYPE_ZIP);

    InputStream response = underTest.extractFile(project, "does_not_exist.txt");

    assertThat(response, is(nullValue()));
  }

  @Test
  public void fileExists() {
    StreamPayload payload = new StreamPayload(() -> getClass().getResourceAsStream(SONATYPE_ZIP),
        UNKNOWN_SIZE, ContentTypes.TEXT_PLAIN);

    assertThat(underTest.fileExists(payload, ANYPATH, GO_MOD), is(true));
  }

  @Test
  public void fileDoesNotExist() {
    StreamPayload payload = new StreamPayload(() -> getClass().getResourceAsStream(SONATYPE_ZIP),
        UNKNOWN_SIZE, ContentTypes.TEXT_PLAIN);

    assertThat(underTest.fileExists(payload, ANYPATH, "does_not_exist.mod"), is(false));
  }
}
