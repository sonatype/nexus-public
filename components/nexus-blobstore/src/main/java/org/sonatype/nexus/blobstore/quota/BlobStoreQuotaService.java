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
package org.sonatype.nexus.blobstore.quota;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

/**
 * For a {@link BlobStore}, checks it usage against its quotas
 *
 * @since 3.14
 */
public interface BlobStoreQuotaService
{
  /**
   * If the config has a quota, ensure that the configuration has all the needed values
   * 
   * @param config - the configuration to be validated
   * @since 3.15
   */
  void validateSoftQuotaConfig(BlobStoreConfiguration config);

  /**
   * @param blobStore - a blob store whose quota needs to be evaluated
   * @return null if the blob store doesn't have a quota otherwise return a {@link BlobStoreQuotaResult}
   */
  BlobStoreQuotaResult checkQuota(BlobStore blobStore);
}
