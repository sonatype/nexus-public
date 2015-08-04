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
package org.sonatype.nexus.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing content detection aginst some most typical files.
 */
public class Nexus5772MimeTest
    extends TestSupport
{

  @After
  public void cleanUp()
      throws Exception
  {
    logger.info("Cleaning up!");
  }

  protected void assertComplete(final MimeSupport mimeSupport, final ContentLocator contentLocator,
                                String expectedMimeType)
      throws IOException
  {
    final List<String> mimeTypes = mimeSupport.detectMimeTypesListFromContent(contentLocator);
    assertThat("Expected MIME type not returned for content:", mimeTypes, hasItem(expectedMimeType));
  }

  @Test
  public void assertComplete()
      throws IOException
  {
    final MimeSupport mimeSupport = new DefaultMimeSupport();
    assertComplete(mimeSupport, new FileContentLocator(util.resolveFile("src/test/resources/mime/file.gif"),
        "application/octet-stream"), "image/gif");
    assertComplete(mimeSupport, new FileContentLocator(util.resolveFile("src/test/resources/mime/file.zip"),
        "application/octet-stream"), "application/zip");
    assertComplete(mimeSupport, new FileContentLocator(util.resolveFile("src/test/resources/mime/empty.zip"),
        "application/octet-stream"), "application/zip");
    assertComplete(mimeSupport, new FileContentLocator(util.resolveFile("src/test/resources/mime/file.jar"),
        "application/octet-stream"), "application/zip");
  }

  @Test
  public void assertCompleteWithFiles()
      throws IOException
  {
    final MimeSupport mimeSupport = new DefaultMimeSupport();
    {
      final StorageFileItem fileItem = mock(StorageFileItem.class);
      final File file = util.resolveFile("src/test/resources/mime/file.gif");
      try (final InputStream is = new FileInputStream(file)) {
        when(fileItem.getInputStream()).thenReturn(is);
        when(fileItem.getName()).thenReturn(file.getName());
        assertThat(mimeSupport.detectMimeTypesFromContent(fileItem), equalTo("image/gif"));
      }
    }
    {
      final StorageFileItem fileItem = mock(StorageFileItem.class);
      final File file = util.resolveFile("src/test/resources/mime/file.zip");
      try (final InputStream is = new FileInputStream(file)) {
        when(fileItem.getInputStream()).thenReturn(is);
        when(fileItem.getName()).thenReturn(file.getName());
        assertThat(mimeSupport.detectMimeTypesFromContent(fileItem), equalTo("application/zip"));
      }
    }
    {
      final StorageFileItem fileItem = mock(StorageFileItem.class);
      final File file = util.resolveFile("src/test/resources/mime/empty.zip");
      try (final InputStream is = new FileInputStream(file)) {
        when(fileItem.getInputStream()).thenReturn(is);
        when(fileItem.getName()).thenReturn(file.getName());
        assertThat(mimeSupport.detectMimeTypesFromContent(fileItem), equalTo("application/zip"));
      }
    }
    {
      final StorageFileItem fileItem = mock(StorageFileItem.class);
      final File file = util.resolveFile("src/test/resources/mime/file.jar");
      try (final InputStream is = new FileInputStream(file)) {
        when(fileItem.getInputStream()).thenReturn(is);
        when(fileItem.getName()).thenReturn(file.getName());
        // NOTE: by content, this is application/zip (JAR file is a Zip file with some extra spice)
        // But here, as we provide file, MimeSupport uses content AND filename to guess
        assertThat(mimeSupport.detectMimeTypesFromContent(fileItem), equalTo("application/java-archive"));
      }
    }
  }

}
