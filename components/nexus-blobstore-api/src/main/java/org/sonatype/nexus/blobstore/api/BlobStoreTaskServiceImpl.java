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
package org.sonatype.nexus.blobstore.api;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreConsumer;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;

@Named
public class BlobStoreTaskServiceImpl
    extends ComponentSupport
    implements BlobStoreTaskService
{

  final List<BlobStoreConsumer> blobStoreConsumers;

  @Inject
  public BlobStoreTaskServiceImpl(final List<BlobStoreConsumer> blobStoreConsumers) {
    this.blobStoreConsumers = blobStoreConsumers;
  }

  @Override
  public boolean isAnyTaskInUseForBlobStore(final String blobStoreName) {
    return blobStoreConsumers.stream().anyMatch(blobStoreConsumer -> blobStoreConsumer.isBlobStoreInUse(blobStoreName));
  }

  @Override
  public int countTasksInUseForBlobStore(final String blobStoreName) {
    return blobStoreConsumers.stream()
        .map(blobStoreConsumer -> blobStoreConsumer.blobStoreUsageCount(blobStoreName))
        .mapToInt(Integer::intValue)
        .sum();
  }
}
