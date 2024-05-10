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
package org.sonatype.nexus.internal.security.apikey.orient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.security.apikey.DefaultApiKeyFactory;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * OrientDB impl of {@link ApiKeyStore}.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientApiKeyStore
    extends StateGuardLifecycleSupport
    implements ApiKeyStore, EventAware
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientApiKeyEntityAdapter entityAdapter;

  private final UserPrincipalsHelper principalsHelper;

  private final Map<String, ApiKeyFactory> apiKeyFactories;

  private final DefaultApiKeyFactory defaultApiKeyFactory;

  @Inject
  public OrientApiKeyStore(
      @Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> databaseInstance,
      final OrientApiKeyEntityAdapter entityAdapter,
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

  public ApiKey newApiKey(
      final String domain,
      final PrincipalCollection principals,
      char[] apiKey,
      OffsetDateTime created)
  {
    return new OrientApiKey(domain, principals, apiKey, created);
  }

  @Override
  @Guarded(by = STARTED)
  public char[] createApiKey(final String domain, final PrincipalCollection principals) {
    checkNotNull(domain);
    checkNotNull(principals);
    try {
      final char[] apiKeyCharArray = makeApiKey(domain, principals);
      persistApiKey(domain, principals, apiKeyCharArray);
      return apiKeyCharArray;
    }
    catch (ORecordDuplicatedException e) { // NOSONAR
      // There is a chance here that if multiple threads enter this method for the same principal that create can be
      // called multiple times resulting in a ORecordDuplicatedException. In that case we know the record must already
      // exist and can call getApiKey again. This avoids locking and gives us eventual-consistency.
      return getApiKey(domain, principals).map(ApiKey::getApiKey).orElse(null);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void persistApiKey(
      final String domain,
      final PrincipalCollection principals,
      final char[] apiKey,
      final OffsetDateTime date)
  {
    checkNotNull(domain);
    checkNotNull(principals);
    checkNotNull(apiKey);
    final OrientApiKey entity = entityAdapter.newEntity();
    entity.setDomain(domain);
    entity.setApiKey(apiKey);
    entity.setPrincipals(principals);
    entity.setCreated(date);
    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, entity));
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<ApiKey> getApiKey(final String domain, final PrincipalCollection principals) {
    return inTx(databaseInstance).call(db -> {
      for (OrientApiKey entity : findByPrimaryPrincipal(db, principals)) {
        if (entity.getDomain().equals(domain) && principalsEqual(entity.getPrincipals(), principals)) {
          return Optional.of(entity);
        }
      }
      return Optional.empty();
    });
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<ApiKey> getApiKeyByToken(final String domain, final char[] apiKey) {
    return inTx(databaseInstance)
        .call(db -> Optional.ofNullable(entityAdapter.findByApiKey(db, domain, checkNotNull(apiKey))));
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKey(final String domain, final PrincipalCollection principals) {
    inTxRetry(databaseInstance).run(db -> {
      for (OrientApiKey entity : findByPrimaryPrincipal(db, principals)) {
        if (entity.getDomain().equals(domain) && principalsEqual(entity.getPrincipals(), principals)) {
          entityAdapter.deleteEntity(db, entity);
        }
      }
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKeys(final PrincipalCollection principals) {
    inTxRetry(databaseInstance).run(db -> {
      for (OrientApiKey entity : findByPrimaryPrincipal(db, principals)) {
        if (principalsEqual(entity.getPrincipals(), principals)) {
          entityAdapter.deleteEntity(db, entity);
        }
      }
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteApiKeys() {
    inTxRetry(databaseInstance).run(entityAdapter::deleteAll);
  }

  @Override
  @Guarded(by = STARTED)
  public void purgeApiKeys() {
    checkCancellation();
    inTxRetry(databaseInstance).run(db -> {
      List<OrientApiKey> delete = new ArrayList<>();
      for (OrientApiKey entity : entityAdapter.browse(db)) {
        checkCancellation();
        UnitOfWork work = UnitOfWork.pause();
        try {
          principalsHelper.getUserStatus(entity.getPrincipals());
        }
        catch (UserNotFoundException e) {
          log.debug("Stale user found", e);
          delete.add(entity);
        }
        finally {
          UnitOfWork.resume(work);
          db.activateOnCurrentThread();
        }
      }
      for (OrientApiKey entity : delete) {
        checkCancellation();
        entityAdapter.deleteEntity(db, entity);
      }
    });
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

  private Iterable<OrientApiKey> findByPrimaryPrincipal(
      final ODatabaseDocumentTx db,
      final PrincipalCollection principals)
  {
    final String primaryPrincipal = checkNotNull(principals).getPrimaryPrincipal().toString();
    return entityAdapter.browseByPrimaryPrincipal(db, primaryPrincipal);
  }

  private char[] makeApiKey(final String domain, final PrincipalCollection principals) {
    ApiKeyFactory factory = apiKeyFactories.get(domain);
    if (factory != null) {
      return checkNotNull(factory.makeApiKey(principals));
    }
    return defaultApiKeyFactory.makeApiKey(principals);
  }

  @Override
  public Collection<ApiKey> browse(final String domain) {
    Iterable<OrientApiKey> keys = inTx(databaseInstance)
        .retryOn(ONeedRetryException.class, ORecordNotFoundException.class)
        .call(db -> entityAdapter.browseByDomain(db, domain));

    return convert(keys);
  }

  @Override
  public Collection<ApiKey> browseByCreatedDate(final String domain, final OffsetDateTime created) {
    Iterable<OrientApiKey> keys = inTx(databaseInstance)
        .retryOn(ONeedRetryException.class, ORecordNotFoundException.class)
        .call(db -> entityAdapter.browseByDomainAndCreated(db, domain, created));

    return convert(keys);
  }

  @Override
  public int count(final String domain) {
    return inTx(databaseInstance)
        .retryOn(ONeedRetryException.class)
        .call(db -> entityAdapter.countByDomainI(db, domain));
  }

  @Override
  public void deleteApiKeys(final String domain) {
    inTxRetry(databaseInstance).run(db -> {
      for (OrientApiKey entity : entityAdapter.browseByDomain(db, domain)) {
        checkCancellation();
        entityAdapter.deleteEntity(db, entity);
      }
    });
  }

  @Override
  public void deleteApiKeys(final OffsetDateTime expiration) {
    inTxRetry(databaseInstance).run(db -> {
      for (OrientApiKey entity : entityAdapter.browseByExpiration(db, expiration)) {
        checkCancellation();
        entityAdapter.deleteEntity(db, entity);
      }
    });
  }

  @Override
  public void updateApiKey(final ApiKey from, final ApiKey to) {
    OrientApiKey fromApiKey = (OrientApiKey) from;
    fromApiKey.setApiKey(to.getApiKey());
    fromApiKey.setPrincipals(to.getPrincipals());
    fromApiKey.setCreated(to.getCreated());
    inTx(databaseInstance).run(db -> entityAdapter.editEntity(db, fromApiKey));
  }

  @Override
  public Collection<ApiKey> browsePaginated(final String domain, final int page, final int pageSize) {
    Iterable<OrientApiKey> keys = inTx(databaseInstance)
        .retryOn(ONeedRetryException.class, ORecordNotFoundException.class)
        .call(db -> entityAdapter.browseByDomainPaginated(db, domain, (page - 1) * pageSize, pageSize));

    return convert(keys);
  }

  private static Collection<ApiKey> convert(final Iterable<OrientApiKey> keys) {
    return StreamSupport.stream(keys.spliterator(), false)
        .map(ApiKey.class::cast)
        .collect(Collectors.toList());
  }

  private boolean principalsEqual(final PrincipalCollection a, final PrincipalCollection b) {
    return Objects.equals(a.getPrimaryPrincipal(), b.getPrimaryPrincipal()) &&
        Objects.equals(a.getRealmNames(), b.getRealmNames());
  }
}