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
package org.sonatype.nexus.repository.maven.internal.proxy;

import java.io.IOException;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.MavenIndexFacetSupport;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;

import static org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher.prefetchIndexFiles;
import static org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher.publishHostedIndex;

/**
 * Proxy implementation of {@link MavenIndexFacet}.
 *
 * @since 3.0
 */
@Named
public class MavenProxyIndexFacet
    extends MavenIndexFacetSupport
{
  @VisibleForTesting
  static final String CONFIG_KEY = "maven-indexer";

  @VisibleForTesting
  static class Config
  {
    @NotNull(groups = ProxyType.ValidationGroup.class)
    public Boolean cacheFallback = Boolean.FALSE;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "cacheFallback=" + cacheFallback +
          '}';
    }
  }

  private Config config;

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class,
        Default.class, getRepository().getType().getValidationGroup()
    );
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    log.debug("Config: {}", config);
  }

  @Override
  public void publishIndex() throws IOException {
    log.debug("Fetching maven index properties from remote");
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      if (!prefetchIndexFiles(getRepository())) {
        if (Boolean.TRUE.equals(config.cacheFallback)) {
          log.debug("No remote index found... generating partial index from caches");
          publishHostedIndex(getRepository());
        }
        else {
          log.debug("No remote index found... nothing to publish");
        }
      }
    }
    finally {
      UnitOfWork.end();
    }
  }
}
