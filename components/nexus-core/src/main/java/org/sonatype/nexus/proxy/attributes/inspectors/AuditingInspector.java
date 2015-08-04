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
package org.sonatype.nexus.proxy.attributes.inspectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.attributes.AbstractStorageItemInspector;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * The Class AuditingInspector simply records the auth stuff from Item Context to attributes..
 *
 * @author cstamas
 */
@Singleton
@Named
public class AuditingInspector
    extends AbstractStorageItemInspector
{
  @Override
  public boolean isHandled(final StorageItem item) {
    if (item instanceof StorageFileItem) {
      final StorageFileItem fitem = (StorageFileItem) item;
      addIfExistsButDontContains(fitem, AccessManager.REQUEST_USER);
      addIfExistsButDontContains(fitem, AccessManager.REQUEST_REMOTE_ADDRESS);
      addIfExistsButDontContains(fitem, AccessManager.REQUEST_CONFIDENTIAL);
    }
    // don't do File copy for us, we done our job already
    return false;
  }

  @Override
  public void processStorageItem(final StorageItem item)
      throws Exception
  {
    // noop
  }

  /**
   * Save it only 1st time. Meaning, a newly proxied/cached item will have not set these attributes, but when it
   * comes from cache, it will. By storing it only once, at first time, we have the record of who did it initially
   * requested.
   */
  private void addIfExistsButDontContains(final StorageFileItem item, final String contextKey) {
    if (item.getItemContext().containsKey(contextKey) && !item.getRepositoryItemAttributes().containsKey(contextKey)) {
      Object val = item.getItemContext().get(contextKey);

      if (val != null) {
        item.getRepositoryItemAttributes().put(contextKey, val.toString());
      }
    }
  }

}
