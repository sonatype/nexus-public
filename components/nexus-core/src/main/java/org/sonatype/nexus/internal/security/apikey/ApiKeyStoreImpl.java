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
package org.sonatype.nexus.internal.security.apikey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * OrientDB impl of {@link ApiKeyStore}.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class ApiKeyStoreImpl
    extends StateGuardLifecycleSupport
    implements ApiKeyStore, EventAware
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final ApiKeyEntityAdapter entityAdapter;

  private final UserPrincipalsHelper principalsHelper;

  private final Map<String, ApiKeyFactory> apiKeyFactories;

  private final DefaultApiKeyFactory defaultApiKeyFactory;

  @Inject
  public ApiKeyStoreImpl(@Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> databaseInstance,
                         final ApiKeyEntityAdapter entityAdapter,
                         final UserPrincipalsHelper principalsHelper,
                         final Map<String, ApiKeyFactory> apiKeyFactories,
                         final DefaultApiKeyFactory defaultApiKeyFactory)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.principalsHelper = checkNotNull(principalsHelper);
    this.apiKeyFactories = checkNotNull(apiKeyFactories);
    this.defaultApiKeyFactory = checkNotNull(defaultApiKeyFactory);
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public char[] createApiKey(final String domain, final PrincipalCollection principals) {
    checkNotNull(domain);
    checkNotNull(principals);
    final char[] apiKeyCharArray = makeApiKey(domain, principals);
    persistApiKey(domain, principals, apiKeyCharArray);
    return apiKeyCharArray;
  }

  @Override
  @Guarded(by = STARTED)
  public void persistApiKey(final String domain, final PrincipalCollection principals, final char[] apiKey) {
    checkNotNull(domain);
    checkNotNull(principals);
    checkNotNull(apiKey);
    final ApiKey entity = new ApiKey();
    entity.setDomain(domain);
    entity.setApiKey(apiKey);
    entity.setPrincipals(principals);
    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.addEntity(db, entity);
    }
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public char[] getApiKey(final String domain, final PrincipalCollection principals) {
    try (ODatabaseDocumentTx db = openDb()) {
      for (ApiKey entity : findByPrimaryPrincipal(db, principals)) {
        if (entity.getDomain().equals(domain)) {
          return entity.getApiKey();
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public PrincipalCollection getPrincipals(final String domain, final char[] apiKey) {
    try (ODatabaseDocumentTx db = openDb()) {
      final ApiKey entity = entityAdapter.findByApiKey(db, domain, checkNotNull(apiKey));
      return entity == null ? null : entity.getPrincipals();
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKey(final String domain, final PrincipalCollection principals) {
    try (ODatabaseDocumentTx db = openDb()) {
      for (ApiKey entity : findByPrimaryPrincipal(db, principals)) {
        if (entity.getDomain().equals(domain)) {
          entityAdapter.deleteEntity(db, entity);
        }
      }
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKeys(final PrincipalCollection principals) {
    try (ODatabaseDocumentTx db = openDb()) {
      for (ApiKey entity : findByPrimaryPrincipal(db, principals)) {
        entityAdapter.deleteEntity(db, entity);
      }
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKeys() {
    try (ODatabaseDocumentTx db = openDb()) {
      entityAdapter.deleteAll.execute(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void purgeApiKeys() {
    try (ODatabaseDocumentTx db = openDb()) {
      List<ApiKey> delete = new ArrayList<>();
      for (ApiKey entity : entityAdapter.browse.execute(db)) {
        // avoid leaking current DB when calling out
        ODatabaseRecordThreadLocal.INSTANCE.set(null);
        try {
          principalsHelper.getUserStatus(entity.getPrincipals());
        }
        catch (UserNotFoundException e) {
          log.debug("Stale user found", e);
          delete.add(entity);
        }
        finally {
          // restore DB in case getUserStatus changed it
          ODatabaseRecordThreadLocal.INSTANCE.set(db);
        }
      }
      for (ApiKey entity : delete) {
        entityAdapter.deleteEntity(db, entity);
      }
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final UserPrincipalsExpired event) {
    final String userId = event.getUserId();
    if (userId != null) {
      deleteApiKeys(new SimplePrincipalCollection(userId, event.getSource()));
    }
    else {
      purgeApiKeys();
    }
  }

  private ODatabaseDocumentTx openDb() {
    return databaseInstance.get().acquire();
  }

  private Iterable<ApiKey> findByPrimaryPrincipal(final ODatabaseDocumentTx db,
                                                  final PrincipalCollection principals)
  {
    final String primaryPrincipal = checkNotNull(principals).getPrimaryPrincipal().toString();
    return entityAdapter.browseByPrimaryPrincipal.execute(db, primaryPrincipal);
  }

  private char[] makeApiKey(final String domain, final PrincipalCollection principals) {
    ApiKeyFactory factory = apiKeyFactories.get(domain);
    if (factory != null) {
      return checkNotNull(factory.makeApiKey(principals));
    }
    return defaultApiKeyFactory.makeApiKey(principals);
  }
}
