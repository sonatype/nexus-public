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
package org.sonatype.nexus.obr.metadata;

import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.osgi.service.obr.Resource;

/**
 * Component that provides methods to read, generate, and write OBR metadata.
 */
public interface ObrMetadataSource
{
  /**
   * Gets an OBR reader for the given site.
   *
   * @param site the OBR site
   * @return the resource reader
   */
  ObrResourceReader getReader(ObrSite site)
      throws StorageException;

  /**
   * Builds an OBR resource for the given repository item, null if the item is not an OSGi bundle.
   *
   * @param item the bundle item
   * @return a new resource, null if not a bundle
   */
  Resource buildResource(StorageFileItem item);

  /**
   * Gets an OBR writer for the given repository item.
   *
   * @param uid the target UID
   * @return the resource writer
   */
  ObrResourceWriter getWriter(RepositoryItemUid uid)
      throws StorageException;
}
