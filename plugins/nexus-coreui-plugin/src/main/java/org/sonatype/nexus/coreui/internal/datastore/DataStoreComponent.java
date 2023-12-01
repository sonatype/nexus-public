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
package org.sonatype.nexus.coreui.internal.datastore;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.security.privilege.ApplicationPermission;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED_NAMED;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * DataStore {@link org.sonatype.nexus.extdirect.DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_Datastore")
public class DataStoreComponent
    extends DirectComponentSupport
    implements StateContributor
{
  private static final String DATASTORES_FIELD = "datastores";

  private static final String JDBCURL_FIELD = "jdbcUrl";

  private static final String DATASTORES_PERMISSION = "datastores";

  private final DataStoreManager dataStoreManager;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final boolean enabled;

  @Inject
  public DataStoreComponent(
      final DataStoreManager dataStoreManager,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      @Named(DATASTORE_ENABLED_NAMED) final boolean enabled)
  {
    this.dataStoreManager = dataStoreManager;
    this.repositoryManager = repositoryManager;
    this.repositoryPermissionChecker = repositoryPermissionChecker;
    this.enabled = enabled;
  }

  @Override
  @Nullable
  public Map<String, Object> getState() {
    return ImmutableMap.of(DATASTORES_FIELD, enabled);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<DataStoreXO> read() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission(DATASTORES_PERMISSION, READ)),
        READ,
        repositoryManager.browse()
    );
    return StreamSupport.stream(dataStoreManager.browse().spliterator(), false).map(this::asDataStoreXO)
        .collect(toList());
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<DataStoreXO> readH2() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission(DATASTORES_PERMISSION, READ)),
        READ,
        repositoryManager.browse()
    );
    return StreamSupport.stream(dataStoreManager.browse().spliterator(), false).filter(
            dataStore -> dataStore.getConfiguration().getAttributes().getOrDefault(JDBCURL_FIELD, "").startsWith("jdbc:h2:"))
        .map(this::asDataStoreXO).collect(toList());
  }

  private DataStoreXO asDataStoreXO(final DataStore<?> dataStore) {
    DataStoreXO dataStoreXO = new DataStoreXO();
    dataStoreXO.setName(dataStore.getConfiguration().getName());
    return dataStoreXO;
  }
}
