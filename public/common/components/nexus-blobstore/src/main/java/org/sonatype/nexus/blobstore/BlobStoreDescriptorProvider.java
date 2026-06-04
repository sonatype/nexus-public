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
package org.sonatype.nexus.blobstore;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.springframework.stereotype.Component;

@Component
public class BlobStoreDescriptorProvider
{
  private final Map<String, BlobStoreDescriptor> blobStoreDescriptors;

  private final DatabaseCheck databaseCheck;

  @Autowired
  public BlobStoreDescriptorProvider(
      final DatabaseCheck databaseCheck,
      final List<BlobStoreDescriptor> blobStoreDescriptorsList)
  {
    this.databaseCheck = databaseCheck;
    this.blobStoreDescriptors = QualifierUtil.buildQualifierBeanMap(blobStoreDescriptorsList);
  }

  public Map<String, BlobStoreDescriptor> get() {
    return blobStoreDescriptors.entrySet()
        .stream()
        .filter(item -> databaseCheck.isAllowedByVersion(item.getValue().getClass()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
