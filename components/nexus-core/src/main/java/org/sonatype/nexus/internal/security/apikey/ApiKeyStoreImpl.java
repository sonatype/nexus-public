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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyFactory;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * MyBatis {@link ApiKeyStore} implementation.
 *
 * @since 3.21
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named("mybatis")
@Singleton
public class ApiKeyStoreImpl
    extends ConfigStoreSupport<ApiKeyDAO>
    implements ApiKeyStore, EventAware
{
  private final UserPrincipalsHelper principalsHelper;

  private final Map<String, ApiKeyFactory> apiKeyFactories;

  private final DefaultApiKeyFactory defaultApiKeyFactory;

  @Inject
  public ApiKeyStoreImpl(final DataSessionSupplier sessionSupplier,
                         final UserPrincipalsHelper principalsHelper,
                         final Map<String, ApiKeyFactory> apiKeyFactories,
                         final DefaultApiKeyFactory defaultApiKeyFactory)
  {
    super(sessionSupplier);

    this.principalsHelper = checkNotNull(principalsHelper);
    this.apiKeyFactories = checkNotNull(apiKeyFactories);
    this.defaultApiKeyFactory = checkNotNull(defaultApiKeyFactory);
  }

  @Override
  public char[] createApiKey(final String domain, final PrincipalCollection principals) {
    final char[] apiKey = makeApiKey(domain, principals);
    persistApiKey(domain, principals, apiKey);
    return apiKey;
  }

  private char[] makeApiKey(final String domain, final PrincipalCollection principals) {
    ApiKeyFactory factory = apiKeyFactories.get(domain);
    if (factory != null) {
      return checkNotNull(factory.makeApiKey(principals));
    }
    return defaultApiKeyFactory.makeApiKey(principals);
  }

  @Transactional
  @Override
  public void persistApiKey(final String domain, final PrincipalCollection principals, final char[] apiKey) {
    ApiKeyData apiKeyData = new ApiKeyData();
    apiKeyData.setDomain(domain);
    apiKeyData.setPrincipals(principals);
    apiKeyData.setApiKey(apiKey);
    dao().save(apiKeyData);
  }

  @Transactional
  @Override
  public char[] getApiKey(final String domain, final PrincipalCollection principals) {
    return dao().findApiKey(domain, principals.getPrimaryPrincipal().toString())
        .map(ApiKeyToken::getChars)
        .orElse(null);
  }

  @Transactional
  @Override
  public PrincipalCollection getPrincipals(final String domain, final char[] apiKey) {
    return dao().findPrincipals(domain, new ApiKeyToken(apiKey)).orElse(null);
  }

  @Transactional
  @Override
  public void deleteApiKey(final String domain, final PrincipalCollection principals) {
    dao().deleteDomainKey(domain, principals.getPrimaryPrincipal().toString());
  }

  @Transactional
  @Override
  public void deleteApiKeys(final PrincipalCollection principals) {
    dao().deleteKeys(principals.getPrimaryPrincipal().toString());
  }

  @Transactional
  @Override
  public void deleteApiKeys() {
    dao().deleteAllKeys();
  }

  @Override
  public void purgeApiKeys() {
    checkCancellation();
    List<PrincipalCollection> candidates = newArrayList(doBrowsePrincipals());
    for (Iterator<PrincipalCollection> itr = candidates.iterator(); itr.hasNext();) {
      checkCancellation();
      if (userExists(itr.next())) {
        itr.remove(); // don't purge keys belonging to existing users
      }
    }
    for (PrincipalCollection principals : candidates) {
      checkCancellation();
      deleteApiKeys(principals);
    }
  }

  @Transactional
  protected Iterable<PrincipalCollection> doBrowsePrincipals() {
    return dao().browsePrincipals();
  }

  protected boolean userExists(final PrincipalCollection principals) {
    try {
      principalsHelper.getUserStatus(principals);
      return true;
    }
    catch (UserNotFoundException e) {
      log.debug("Stale user found", e);
      return false;
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
}
