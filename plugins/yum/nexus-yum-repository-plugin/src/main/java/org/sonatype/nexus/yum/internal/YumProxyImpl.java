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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.yum.YumProxy;
import org.sonatype.nexus.yum.YumRepository;

import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 2.7
 */
@Named
public class YumProxyImpl
    implements YumProxy
{

  private final ProxyRepository repository;

  private final File baseDir;

  private final YumRepository yumRepository;

  @Inject
  public YumProxyImpl(final ProxyMetadataRequestStrategy proxyMetadataRequestStrategy,
                      final BlockSqliteDatabasesRequestStrategy blockSqliteDatabasesRequestStrategy,
                      final @Assisted ProxyRepository repository)
      throws MalformedURLException, URISyntaxException

  {
    this.repository = checkNotNull(repository);
    this.baseDir = RepositoryUtils.getBaseDir(repository);
    this.yumRepository = new YumRepositoryImpl(baseDir, repository.getId(), null);

    repository.registerRequestStrategy(
        BlockSqliteDatabasesRequestStrategy.class.getName(), checkNotNull(blockSqliteDatabasesRequestStrategy)
    );
    repository.registerRequestStrategy(
        ProxyMetadataRequestStrategy.class.getName(), checkNotNull(proxyMetadataRequestStrategy)
    );
  }

  @Override
  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public Repository getNexusRepository() {
    return repository;
  }

  @Override
  public YumRepository getYumRepository() {
    return yumRepository;
  }

}
