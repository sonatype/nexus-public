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
package org.sonatype.nexus.rest;

import java.io.IOException;

import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;

import org.restlet.data.Request;

/**
 * Provides an alternative view of an artifact / file.
 *
 * @author Brian Demers
 */
public interface ArtifactViewProvider
{
  /**
   * Returns an object that represents a view for the storeRequest.
   *
   * @param store        The ResourceStore that was about to be "asked" for content.
   * @param request      The store request to retrieve the view for.
   * @param item         The item retrieved or null if not found. View provider must handle nulls.
   * @param req          The REST request.
   * @return An object representing the view.
   */
  public Object retrieveView(ResourceStore store, ResourceStoreRequest request, StorageItem item, Request req)
      throws IOException;
}
