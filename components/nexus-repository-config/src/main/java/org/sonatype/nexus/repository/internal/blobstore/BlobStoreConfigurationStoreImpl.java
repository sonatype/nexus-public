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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.blobstore.group.BlobStoreGroupConfigurationHelper.memberNames;

/**
 * MyBatis {@link BlobStoreConfigurationStore} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class BlobStoreConfigurationStoreImpl
    extends ConfigStoreSupport<BlobStoreConfigurationDAO>
    implements BlobStoreConfigurationStore
{
  @Inject
  public BlobStoreConfigurationStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public BlobStoreConfiguration newConfiguration() {
    return new BlobStoreConfigurationData();
  }

  @Transactional
  @Override
  public List<BlobStoreConfiguration> list() {
    return ImmutableList.copyOf(dao().browse());
  }

  @Transactional
  @Override
  public void create(final BlobStoreConfiguration configuration) {
    dao().create((BlobStoreConfigurationData) configuration);
  }

  @Transactional
  @Override
  public BlobStoreConfiguration read(final String name) {
    return dao().readByName(name).orElse(null);
  }

  @Transactional
  @Override
  public void update(final BlobStoreConfiguration configuration) {
    dao().update((BlobStoreConfigurationData) configuration);
  }

  @Transactional
  @Override
  public void delete(final BlobStoreConfiguration configuration) {
    dao().deleteByName(configuration.getName());
  }

  @Transactional
  @Override
  public Optional<BlobStoreConfiguration> findParent(final String name) {
    return dao().findCandidateParents(name).stream()
        .filter(config -> memberNames(config).contains(name))
        .findFirst();
  }
}
