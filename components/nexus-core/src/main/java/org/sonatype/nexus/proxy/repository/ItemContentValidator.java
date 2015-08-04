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
package org.sonatype.nexus.proxy.repository;

import java.util.List;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;

/**
 * Item content validator component.
 *
 * @author cstamas
 */
public interface ItemContentValidator
{
  /**
   * Performs a validation in the context of the given Proxy repository, request and baseUrl against passed in item.
   * Returns {@code true} if item found to be valid, and {@code false} if item found invalid.
   *
   * @param proxy   repository that was used to get this item
   * @param request request that was used to get this item
   * @param baseUrl baseUrl that was used to get this item
   * @param item    item to validate
   * @param events  list of events that might be appended to, if given validator wants to emit event. At the end of
   *                validation (all validators that were participating are "asked" for opinion), the events contained
   *                in
   *                this list will be "fired off" as events.
   * @return {@code true} if item found to be valid, and {@code false} if item found invalid.
   * @throws LocalStorageException in case of some fatal unrecoverable error (IO or other).
   */
  boolean isRemoteItemContentValid(ProxyRepository proxy, ResourceStoreRequest request, String baseUrl,
                                   AbstractStorageItem item, List<RepositoryItemValidationEvent> events)
      throws LocalStorageException;
}
