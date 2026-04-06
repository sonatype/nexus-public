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
package org.sonatype.nexus.repository.apt.datastore.internal.metadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Base class for automated APT metadata rebuild tasks.
 * Provides common implementation for both hosted and proxy repository types.
 * <p>
 * These tasks are triggered automatically by the metadata scheduler in response to repository events
 * such as asset creation, deletion, or updates. They invoke the metadata facet to rebuild repository
 * metadata (Package indexes, Release files, signatures).
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AutomatedAptMetadataRebuildTask
    extends RepositoryTaskSupport
{
  protected AutomatedAptMetadataRebuildTask() {
    super(false);
  }

  @Override
  public String getMessage() {
    return "Automated APT metadata rebuild";
  }

  @Override
  protected void execute(final Repository repository) {
    log.info("Starting automated APT metadata rebuild for repository {}", repository.getName());

    try {
      long start = System.currentTimeMillis();

      // Delegate to the appropriate metadata facet (hosted or proxy)
      rebuildMetadata(repository);

      long duration = System.currentTimeMillis() - start;
      log.info("Completed APT metadata rebuild for repository {} in {} ms", repository.getName(), duration);
    }
    catch (IOException e) {
      log.error("Failed to rebuild APT metadata for repository {}", repository.getName(), e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return List.of(ProxyType.NAME, HostedType.NAME).contains(repository.getType().getValue()) &&
        AptFormat.NAME.equals(repository.getFormat().getValue());
  }

  /**
   * Rebuilds metadata for the given repository by invoking the appropriate metadata facet.
   * <p>
   * Hosted implementation: Invokes
   * {@link org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet#rebuildMetadata()}
   * <br>
   * Proxy implementation: Invokes
   * {@link org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet#rebuildMetadata()}
   *
   * @param repository the repository to rebuild metadata for
   * @throws IOException if metadata rebuild fails
   */
  protected void rebuildMetadata(final Repository repository) throws IOException {
    repository.facet(AptMetadataFacetSupport.class).rebuildMetadata();
  }
}
