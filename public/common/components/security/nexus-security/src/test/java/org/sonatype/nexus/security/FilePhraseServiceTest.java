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
package org.sonatype.nexus.security;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link FilePhraseService}.
 */
public class FilePhraseServiceTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testPhraseIsTheTrimmedFileContent() throws Exception {
    File phraseFile = phraseFile("fake-phrase\n");

    assertThat(new FilePhraseService(phraseFile).getPhrase("default"), is("fake-phrase"));
  }

  @Test
  public void testPhraseIsReadOnceAndMemoized() throws Exception {
    File phraseFile = phraseFile("first-fake-phrase");
    FilePhraseService service = new FilePhraseService(phraseFile);

    assertThat(service.getPhrase("default"), is("first-fake-phrase"));

    Files.write(phraseFile.toPath(), "second-fake-phrase".getBytes(StandardCharsets.UTF_8));

    assertThat(service.getPhrase("default"), is("first-fake-phrase"));
  }

  @Test
  public void testPhraseIsNotReadUntilNeeded() throws Exception {
    File phraseFile = phraseFile("first-fake-phrase");
    FilePhraseService service = new FilePhraseService(phraseFile);

    Files.write(phraseFile.toPath(), "second-fake-phrase".getBytes(StandardCharsets.UTF_8));

    assertThat(service.getPhrase("default"), is("second-fake-phrase"));
  }

  @Test
  public void testUnreadableFileFailsWithTheIoExceptionAsCause() {
    File missing = new File(temporaryFolder.getRoot(), "missing-phrase-file");
    FilePhraseService service = new FilePhraseService(missing);

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.getPhrase("default"));

    assertThat(thrown.getCause(), instanceOf(IOException.class));
  }

  @Test
  public void testWithoutPhraseFileTheDefaultPhraseIsUsed() {
    assertThat(new FilePhraseService(null).getPhrase("default"), is("default"));
  }

  private File phraseFile(final String content) throws IOException {
    File file = temporaryFolder.newFile("phrase-file");
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return file;
  }
}
