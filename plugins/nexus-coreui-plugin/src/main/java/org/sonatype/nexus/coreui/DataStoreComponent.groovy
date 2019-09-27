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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.datastore.DataStoreConfigurationSource
import org.sonatype.nexus.datastore.DataStoreDescriptor
import org.sonatype.nexus.datastore.api.DataStore
import org.sonatype.nexus.datastore.api.DataStoreConfiguration
import org.sonatype.nexus.datastore.api.DataStoreManager
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker
import org.sonatype.nexus.security.privilege.ApplicationPermission
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static java.util.Collections.singletonList
import static org.sonatype.nexus.security.BreadActions.READ

/**
 * DataStore {@link org.sonatype.nexus.extdirect.DirectComponent}.
 *
 * @since 3.19
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Datastore')
class DataStoreComponent
    extends DirectComponentSupport
    implements StateContributor
{
  @Inject
  DataStoreManager dataStoreManager

  @Inject
  Map<String, DataStoreDescriptor> dataStoreDescriptors

  @Inject
  Map<String, DataStoreConfigurationSource> dataStoreConfigurationSources

  @Inject
  RepositoryManager repositoryManager

  @Inject
  RepositoryPermissionChecker repositoryPermissionChecker

  @Inject
  @Named('${nexus.datastore.enabled:-false}')
  boolean enabled

  @Override
  @Nullable
  Map<String, Object> getState() {
    return ['datastores': enabled]
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<DataStoreXO> read() {
    repositoryPermissionChecker.ensureUserHasAnyPermissionOrAdminAccess(
        singletonList(new ApplicationPermission('datastores', READ)),
        READ,
        repositoryManager.browse()
    )
    def dataStores = dataStoreManager.browse()

    dataStores.collect { asDataStoreXO(it) }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:datastores:read')
  List<DataStoreTypeXO> readTypes() {
    dataStoreDescriptors.collect { key, descriptor ->
      new DataStoreTypeXO(
          id: key,
          name: descriptor.name,
          formFields: descriptor.formFields.collect { FormFieldXO.create(it) },
          isEnabled: descriptor.isEnabled()
      )
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:datastores:read')
  List<DataStoreSourceXO> readSources() {
    dataStoreConfigurationSources.collect { key, source ->
      new DataStoreSourceXO(
          id: key,
          name: source.name,
          isModifiable: source.isModifiable(),
          isEnabled: source.isEnabled()
      )
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:datastores:create')
  @Validate(groups = [Create, Default])
  DataStoreXO create(final @NotNull @Valid DataStoreXO dataStore) {
    return asDataStoreXO(dataStoreManager.create(asConfiguration(dataStore)))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:datastores:update')
  @Validate(groups = [Update, Default])
  DataStoreXO update(final @NotNull @Valid DataStoreXO dataStore) {
    return asDataStoreXO(dataStoreManager.update(asConfiguration(dataStore)))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:datastores:delete')
  @Validate
  void remove(final @NotEmpty String name) {
    dataStoreManager.delete(name)
  }

  static DataStoreConfiguration asConfiguration(final DataStoreXO dataStoreXO) {
    new DataStoreConfiguration(
        name: dataStoreXO.name,
        type: dataStoreXO.type,
        source: dataStoreXO.source,
        attributes: dataStoreXO.attributes
    )
  }

  DataStoreXO asDataStoreXO(final DataStore dataStore) {
    String storeName = dataStore.configuration.name
    boolean isContentStore = dataStoreManager.isContentStore(storeName)
    def dataStoreXO = new DataStoreXO(
        name: storeName,
        type: dataStore.configuration.type,
        source: dataStore.configuration.source,
        attributes: dataStore.configuration.attributes,
        inUse: !isContentStore || repositoryManager.isDataStoreUsed(storeName),
        isContentStore: isContentStore
    )
    return dataStoreXO
  }
}
