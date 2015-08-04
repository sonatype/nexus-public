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
package org.sonatype.nexus.proxy.maven;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.metadata.AbstractMetadataHelper;
import org.sonatype.nexus.proxy.maven.metadata.DefaultMetadataHelper;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;

import org.slf4j.Logger;

/**
 * @author Juven Xu
 */
public class RecreateMavenMetadataWalkerProcessor
    extends AbstractWalkerProcessor
{
  private boolean isHostedRepo;

  private MavenRepository repository;

  private AbstractMetadataHelper mdHelper;

  private final Logger logger;

  private DeleteOperation deleteOperation;

  public RecreateMavenMetadataWalkerProcessor(Logger logger) {
    this.logger = logger;
  }

  public RecreateMavenMetadataWalkerProcessor(Logger logger, DeleteOperation operation) {
    this.logger = logger;
    this.deleteOperation = operation;
  }

  @Override
  public void beforeWalk(WalkerContext context)
      throws Exception
  {
    isHostedRepo = false;

    repository =
        context.getRepository() instanceof MavenRepository ? (MavenRepository) context.getRepository() : null;

    if (repository != null) {
      mdHelper = new DefaultMetadataHelper(logger, repository, deleteOperation);

      isHostedRepo = repository.getRepositoryKind().isFacetAvailable(HostedRepository.class);
    }

    setActive(isHostedRepo);
  }

  @Override
  public void onCollectionEnter(WalkerContext context, StorageCollectionItem coll) {
    try {
      mdHelper.onDirEnter(coll.getPath());
    }
    catch (Exception e) {
      logger.warn("Error occurred while entering collection '" + coll.getPath() + "'.", e);
    }
  }

  @Override
  public void processItem(WalkerContext context, StorageItem item) {
    if (item instanceof StorageFileItem) {
      try {
        mdHelper.processFile(item.getPath());
      }
      catch (Exception e) {
        logger.warn("Error occurred while processing item '" + item.getPath() + "'.", e);
      }
    }
  }

  @Override
  public void onCollectionExit(WalkerContext context, StorageCollectionItem coll) {
    try {
      mdHelper.onDirExit(coll.getPath());

      if (coll.list().size() == 0) {
        ResourceStoreRequest request = new ResourceStoreRequest(coll);
        if (deleteOperation != null) {
          request.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY, this.deleteOperation);
        }

        repository.deleteItem(false, request);
      }
    }
    catch (Exception e) {
      logger.warn("Error occurred while existing collection '" + coll.getPath() + "'.", e);
    }
  }
}
