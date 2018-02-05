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
package org.sonatype.nexus.blobstore.file;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobMetrics;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class FileBlobAttributesTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testPersistence() throws Exception {
    Path path = temporaryFolder.newFile().toPath();

    Map<String, String> headers = ImmutableMap.of("hello", "world");
    BlobMetrics metrics = new BlobMetrics(new DateTime(987654321), "0123456789ABCDEF", 42);
    FileBlobAttributes original = new FileBlobAttributes(path, headers, metrics);

    original.store();

    assertTrue(Files.isRegularFile(original.getPath()));

    Properties properties = new Properties();
    try (FileReader reader = new FileReader(original.getPath().toFile())) {
      properties.load(reader);
    }

    assertThat(properties.remove("@hello"), is("world"));
    assertThat(properties.remove("creationTime"), is("987654321"));
    assertThat(properties.remove("sha1"), is("0123456789ABCDEF"));
    assertThat(properties.remove("size"), is("42"));
    assertThat(properties.keySet(), is(empty()));

    original.setDeleted(true);
    original.store();

    try (FileReader reader = new FileReader(original.getPath().toFile())) {
      properties.load(reader);
    }

    assertThat(properties.remove("@hello"), is("world"));
    assertThat(properties.remove("creationTime"), is("987654321"));
    assertThat(properties.remove("sha1"), is("0123456789ABCDEF"));
    assertThat(properties.remove("size"), is("42"));
    assertThat(properties.remove("deleted"), is("true"));
    assertThat(properties.remove("deletedReason"), is("No reason supplied"));
    assertThat(properties.keySet(), is(empty()));

    original.setDeletedReason("Spring cleaning");
    original.store();

    try (FileReader reader = new FileReader(original.getPath().toFile())) {
      properties.load(reader);
    }

    assertThat(properties.remove("@hello"), is("world"));
    assertThat(properties.remove("creationTime"), is("987654321"));
    assertThat(properties.remove("sha1"), is("0123456789ABCDEF"));
    assertThat(properties.remove("size"), is("42"));
    assertThat(properties.remove("deleted"), is("true"));
    assertThat(properties.remove("deletedReason"), is("Spring cleaning"));
    assertThat(properties.keySet(), is(empty()));
  }

  @Test
  public void testRoundtrip() throws Exception {
    Path path = temporaryFolder.newFile().toPath();

    Map<String, String> headers = ImmutableMap.of("hello", "world");
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "0123456789ABCDEF", 42);
    FileBlobAttributes original = new FileBlobAttributes(path, headers, metrics);

    verifyRoundtrip(original);

    original.setDeleted(true);

    verifyRoundtrip(original);

    original.setDeletedReason("Spring cleaning");

    verifyRoundtrip(original);
  }

  @Test
  public void testUpdateFrom() throws Exception {
    Path originalPath = temporaryFolder.newFile().toPath();

    Map<String, String> headers = ImmutableMap.of("hello", "world");
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "0123456789ABCDEF", 42);
    FileBlobAttributes original = new FileBlobAttributes(originalPath, headers, metrics);

    Path updatedPath = temporaryFolder.newFile().toPath();

    FileBlobAttributes updated = new FileBlobAttributes(updatedPath);
    updated.updateFrom(original);
    updated.store();

    updated = new FileBlobAttributes(updatedPath);
    updated.load();

    assertThat(updated.getHeaders(), is(original.getHeaders()));
    assertThat(updated.getMetrics().getCreationTime(), is(original.getMetrics().getCreationTime()));
    assertThat(updated.getMetrics().getSha1Hash(), is(original.getMetrics().getSha1Hash()));
    assertThat(updated.getMetrics().getContentSize(), is(original.getMetrics().getContentSize()));
    assertThat(updated.isDeleted(), is(original.isDeleted()));
    assertThat(updated.getDeletedReason(), is(original.getDeletedReason()));
  }

  private static void verifyRoundtrip(final FileBlobAttributes original) throws IOException {

    original.store();

    FileBlobAttributes restored = new FileBlobAttributes(original.getPath());

    restored.load();

    assertThat(restored.getPath(), is(original.getPath()));
    assertThat(restored.getHeaders(), is(original.getHeaders()));
    assertThat(restored.getMetrics().getCreationTime(), is(original.getMetrics().getCreationTime()));
    assertThat(restored.getMetrics().getSha1Hash(), is(original.getMetrics().getSha1Hash()));
    assertThat(restored.getMetrics().getContentSize(), is(original.getMetrics().getContentSize()));
    assertThat(restored.isDeleted(), is(original.isDeleted()));
    assertThat(restored.getDeletedReason(), is(original.getDeletedReason()));
  }
}
