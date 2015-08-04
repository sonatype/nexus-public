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
package org.sonatype.nexus.proxy.item;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.common.collect.Lists;

public class DefaultStorageCompositeFileItem
    extends DefaultStorageFileItem
    implements StorageCompositeFileItem
{
  private final transient List<StorageItem> sources;

  public DefaultStorageCompositeFileItem(Repository repository, ResourceStoreRequest request, boolean canRead,
                                         boolean canWrite, ContentLocator contentLocator, List<StorageItem> sources)
  {
    super(repository, request, canRead, canWrite, contentLocator);
    final List<StorageItem> src = Lists.newArrayList();
    if (sources != null) {
      src.addAll(sources);
    }
    this.sources = Collections.unmodifiableList(src);
  }

  @Override
  public List<StorageItem> getSources() {
    return sources;
  }
}
