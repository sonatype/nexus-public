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

import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;

import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_HASH_EXPIRED;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_MD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_SHA1;

public class ExpireCacheWalker
    extends AbstractFileWalkerProcessor
{
  private final Repository repository;

  private int alteredItemCount;

  private boolean isMavenProxy;

  public ExpireCacheWalker(Repository repository) {
    this.repository = repository;
    this.alteredItemCount = 0;
    this.isMavenProxy = repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class);
  }

  public Repository getRepository() {
    return repository;
  }

  @Override
  protected void processFileItem(WalkerContext context, StorageFileItem item)
      throws Exception
  {
    if (!item.isExpired()) {
      // expiring found files
      item.setExpired(true);

      if (isMavenProxy) {
        // expire remote hashes when expiring maven proxy cache
        Attributes attributes = item.getRepositoryItemAttributes();
        if (attributes.containsKey(ATTR_REMOTE_SHA1) || attributes.containsKey(ATTR_REMOTE_MD5)) {
          attributes.put(ATTR_REMOTE_HASH_EXPIRED, "true");
        }
      }

      // store it
      getRepository().getAttributesHandler().storeAttributes(item);

      alteredItemCount++;
    }
  }

  public boolean isCacheAltered() {
    return alteredItemCount > 0;
  }

  public int getAlteredItemCount() {
    return alteredItemCount;
  }
}
