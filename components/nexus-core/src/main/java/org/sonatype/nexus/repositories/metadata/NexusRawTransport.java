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
package org.sonatype.nexus.repositories.metadata;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.repository.metadata.RawTransport;
import org.sonatype.nexus.util.io.StreamSupport;

public class NexusRawTransport
    implements RawTransport
{
  private final Repository repository;

  private final boolean localOnly;

  private final boolean remoteOnly;

  private StorageFileItem lastReadFile;

  private StorageFileItem lastWriteFile;

  public NexusRawTransport(Repository repository, boolean localOnly, boolean remoteOnly) {
    this.repository = repository;

    this.localOnly = localOnly;

    this.remoteOnly = remoteOnly;
  }

  public byte[] readRawData(String path)
      throws Exception
  {
    try {
      ResourceStoreRequest request = new ResourceStoreRequest(path, localOnly, remoteOnly);

      StorageItem item = repository.retrieveItem(false, request);

      if (item instanceof StorageFileItem) {
        StorageFileItem file = (StorageFileItem) item;

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (final InputStream is = file.getInputStream()) {
          StreamSupport.copy(is, os, StreamSupport.BUFFER_SIZE);
        }
        lastReadFile = file;
        return os.toByteArray();
      }
      else {
        return null;
      }

    }
    catch (ItemNotFoundException e) {
      // not found should return null
      return null;
    }
  }

  public void writeRawData(String path, byte[] data)
      throws Exception
  {
    DefaultStorageFileItem file = new DefaultStorageFileItem(
        repository,
        new ResourceStoreRequest(path),
        true,
        true,
        new ByteArrayContentLocator(data, "text/xml"));

    repository.storeItem(false, file);

    lastWriteFile = file;
  }

  // ==

  public StorageFileItem getLastReadFile() {
    return lastReadFile;
  }

  public StorageFileItem getLastWriteFile() {
    return lastWriteFile;
  }
}
