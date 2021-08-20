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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileRawObjectAccessTest
    extends TestSupport
{
  private FileRawObjectAccess underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void initBlobStore() {
    BlobStoreConfiguration configuration = new MockBlobStoreConfiguration();

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileMap = new HashMap<>();
    fileMap.put("path", temporaryFolder.getRoot().toPath());
    attributes.put("file", fileMap);

    configuration.setAttributes(attributes);

    underTest = new FileRawObjectAccess(temporaryFolder.getRoot().toPath());
  }

  @Test
  public void listRawObjects() throws Exception {
    Path dir = Paths.get("path", "to");

    temporaryFolder.newFolder(dir.toString());
    temporaryFolder.newFile(dir.resolve("object1.txt").toString());

    List<String> objects = underTest.listRawObjects(dir).collect(toList());
    assertEquals(1, objects.size());
    assertEquals("object1.txt", objects.get(0));
  }

  @Test
  public void listRawObjects_empty() throws Exception {
    Path dir = Paths.get("path", "to");

    temporaryFolder.newFolder(dir.toString());

    List<String> objects = underTest.listRawObjects(dir).collect(toList());
    assertTrue(objects.isEmpty());
  }

  @Test
  public void listRawObjects_root() throws Exception {
    temporaryFolder.newFile("object1.txt");

    List<String> objects = underTest.listRawObjects(null).collect(toList());
    assertEquals(1, objects.size());
    assertEquals("object1.txt", objects.get(0));
  }

  @Test
  public void getRawObject() throws Exception {
    Path dir = Paths.get("path", "to");

    temporaryFolder.newFolder(dir.toString());
    File file1 = temporaryFolder.newFile(dir.resolve("object1.txt").toString());

    FileUtils.writeStringToFile(file1, "hello!", StandardCharsets.UTF_8.name());

    InputStream in = underTest.getRawObject(dir.resolve("object1.txt"));
    assertNotNull(in);
    assertEquals("hello!", IOUtils.toString(in, StandardCharsets.UTF_8.name()));
  }

  @Test
  public void getRawObject_notFound() {
    InputStream in = underTest.getRawObject(Paths.get("path", "to", "object1.txt"));
    assertNull(in);
  }

  @Test
  public void putRawObject() throws Exception {
    Path dir = Paths.get("path", "to");
    File dirFile = temporaryFolder.newFolder(dir.toString());

    underTest.putRawObject(dir.resolve("object1.txt"), new ByteArrayInputStream("hello!".getBytes()));

    byte[] object1 = Files.readAllBytes(dirFile.toPath().resolve("object1.txt"));
    assertEquals("hello!", new String(object1, StandardCharsets.UTF_8));
  }

  @Test
  public void deleteRawObjectsInPath() throws Exception {
    Path path = Paths.get("path", "to");

    File parent = temporaryFolder.newFolder(path.toString());
    File file1 = temporaryFolder.newFile(path.resolve("object1.txt").toString());
    File file2 = temporaryFolder.newFile(path.resolve("object2.txt").toString());

    underTest.deleteRawObjectsInPath(path);
    assertFalse(file1.exists());
    assertFalse(file2.exists());
    assertFalse(parent.exists());
  }

  @Test
  public void deleteRawObjectsInPathNestedContent() throws Exception {
    Path path = Paths.get("path", "to");

    File parent = temporaryFolder.newFolder(path.toString());
    File file1 = temporaryFolder.newFile(path.resolve("object1.txt").toString());
    File file2 = temporaryFolder.newFile(path.resolve("object2.txt").toString());
    temporaryFolder.newFolder(path.resolve("nested").toString());

    underTest.deleteRawObjectsInPath(path);
    assertFalse(file1.exists());
    assertFalse(file2.exists());
    // can't delete parent because of nested folder
    assertTrue(parent.exists());
  }
}
