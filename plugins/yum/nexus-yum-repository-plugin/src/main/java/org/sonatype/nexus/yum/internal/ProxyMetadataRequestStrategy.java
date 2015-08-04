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
package org.sonatype.nexus.yum.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.AbstractRequestStrategy;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumProxy;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A request strategy that applies to yum enabled proxies that will process repomd.xml/primary.xml before being served.
 *
 * @since 2.7.0
 */
@Named
@Singleton
public class ProxyMetadataRequestStrategy
    extends AbstractRequestStrategy
{
  private static final Logger log = LoggerFactory.getLogger(ProxyMetadataRequestStrategy.class);

  private static final String REPOMD_XML_PATH = "/" + Yum.PATH_OF_REPOMD_XML;

  /**
   * @since 2.11
   */
  @Override
  public void onHandle(final Repository repository, final ResourceStoreRequest request, final Action action) {
    if (action.isReadAction() && request.getRequestPath().startsWith("/" + Yum.PATH_OF_REPOMD_XML)) {
      try {
        log.trace("Checking if {}:{} should be processed", repository.getId(), request.getRequestPath());
        StorageFileItem repoMDItem = (StorageFileItem) repository.retrieveItem(
            false, new ResourceStoreRequest(REPOMD_XML_PATH)
        );
        if (repoMDItem.getRepositoryItemAttributes().get(YumProxy.PROCESSED) == null) {
          try {
            repoMDItem.getRepositoryItemUid().getLock().lock(Action.update);
            if (repoMDItem.getRepositoryItemAttributes().get(YumProxy.PROCESSED) == null) {
              MetadataProcessor.processProxiedMetadata((ProxyRepository) repository);
              repoMDItem.getRepositoryItemAttributes().put(
                  YumProxy.PROCESSED, String.valueOf(System.currentTimeMillis())
              );
              repository.getAttributesHandler().storeAttributes(repoMDItem);
            }
          }
          finally {
            repoMDItem.getRepositoryItemUid().getLock().unlock();
          }
        }
      }
      catch (ItemNotFoundException e) {
        // ignore as we do not have a repomd.xml
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
  }

}
