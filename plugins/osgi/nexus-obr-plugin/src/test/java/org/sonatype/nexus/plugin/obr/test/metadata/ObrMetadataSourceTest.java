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
package org.sonatype.nexus.plugin.obr.test.metadata;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.junit.Test;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

public class ObrMetadataSourceTest
    extends AbstractObrMetadataTest
{

  @Test
  public void testBundleItem1()
      throws Exception
  {
    final ContentLocator content = new AbstractContentLocator("application/java-archive", false, ContentLocator.UNKNOWN_LENGTH)
    {
      public InputStream getContent()
          throws IOException
      {
        return getResourceAsStream("/obr/jars/osgi.core.jar");
      }
    };

    final StorageFileItem item = createStorageFileItem("/valid/bundle/1", content);

    final Resource resource = obrMetadataSource.buildResource(item);

    assertNotNull(resource);
  }

  @Test
  public void testBundleItem2()
      throws Exception
  {
    final ContentLocator content = new AbstractContentLocator("application/java-archive", false, ContentLocator.UNKNOWN_LENGTH)
    {
      public InputStream getContent()
          throws IOException
      {
        return getResourceAsStream("/obr/jars/org.eclipse.core.runtime_3.4.0.v20080512.jar");
      }
    };

    final StorageFileItem item = createStorageFileItem("/valid/bundle/2", content);

    final Resource resource = obrMetadataSource.buildResource(item);
    for (final Requirement req : resource.getRequirements()) {
      if (req.getFilter().contains("org.eclipse.osgi")) {
        assertFalse("NXCM-1365: org.eclipse.osgi dependency should not be optional", req.isOptional());
      }
    }

    assertNotNull(resource);
  }

  @Test
  public void testNonBundleItem()
      throws Exception
  {
    final ContentLocator content = new AbstractContentLocator("application/java-archive", false, ContentLocator.UNKNOWN_LENGTH)
    {
      public InputStream getContent()
          throws IOException
      {
        return getResourceAsStream("/obr/jars/maven-model-2.0.jar");
      }
    };

    final StorageFileItem item = createStorageFileItem("/non/bundle", content);

    assertNull(obrMetadataSource.buildResource(item));
  }

  @Test
  public void testBrokenStream()
      throws Exception
  {
    final ContentLocator content = new AbstractContentLocator("application/java-archive", false, ContentLocator.UNKNOWN_LENGTH)
    {
      public InputStream getContent()
          throws IOException
      {
        throw new IOException("EOF");
      }
    };

    final StorageFileItem item = createStorageFileItem("/broken/stream", content);

    assertNull(obrMetadataSource.buildResource(item));
  }

  @Test
  public void testNullStream()
      throws Exception
  {
    final ContentLocator content = new AbstractContentLocator("application/java-archive", false, ContentLocator.UNKNOWN_LENGTH)
    {
      public InputStream getContent()
          throws IOException
      {
        return null;
      }
    };

    final StorageFileItem item = createStorageFileItem("/null/stream", content);

    assertNull(obrMetadataSource.buildResource(item));
  }

  private StorageFileItem createStorageFileItem(final String path, final ContentLocator content) {
    return new DefaultStorageFileItem(testRepository, new ResourceStoreRequest(path), true, true, content);
  }
}
