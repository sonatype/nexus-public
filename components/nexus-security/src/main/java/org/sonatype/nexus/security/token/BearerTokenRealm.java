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
package org.sonatype.nexus.security.token;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.NexusApiKeyAuthenticationToken;
import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.security.authc.apikey.ApiKeyService;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AuthenticatingRealm} that maps bearer tokens to valid {@link Subject}s. This class provides format agnostic
 * behaviour with the intention that it will be subclassed for formats that require token auth.
 *
 * @since 3.6
 */
public abstract class BearerTokenRealm
    extends AuthenticatingRealm
{
  public static final String IS_TOKEN_AUTH_KEY = BearerTokenRealm.class.getName() + ".IS_TOKEN";

  @VisibleForTesting
  static final String ANONYMOUS_USER = "anonymous";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ApiKeyService keyStore;

  private final UserPrincipalsHelper principalsHelper;

  private final String format;

  private Provider<HttpServletRequest> requestProvider;

  protected BearerTokenRealm(final ApiKeyService keyStore,
                          final UserPrincipalsHelper principalsHelper,
                          final String format) {
    this.keyStore = checkNotNull(keyStore);
    this.principalsHelper = checkNotNull(principalsHelper);
    this.format = checkNotNull(format);
    setName(format);
    setAuthenticationCachingEnabled(true);
  }

  @Inject
  protected void setRequestProvider(final Provider<HttpServletRequest> requestProvider) {
    this.requestProvider = checkNotNull(requestProvider);
  }

  @Override
  public boolean supports(final AuthenticationToken token) {
    return token instanceof NexusApiKeyAuthenticationToken && format.equals(token.getPrincipal());
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
  {
    checkNotNull(token);
    return getPrincipals(token).map(principals -> {
      try {
        if (anonymousAndSupported(principals) || principalsHelper.getUserStatus(principals).isActive()) {
          return new SimpleAuthenticationInfo(principals, token.getCredentials());
        }
      }
      catch (final UserNotFoundException e) {
        log.debug("Realm did not find user", e);
        keyStore.deleteApiKeys(principals);
      }
      return null;
    }).orElse(null);
  }

  @Override
  @Nullable
  protected Object getAuthenticationCacheKey(@Nullable final AuthenticationToken token) {
    if (token != null) {
      return getPrincipals(token)
          .map(PrincipalCollection::getPrimaryPrincipal)
          .orElse(null);
    }
    return null;
  }

  @Override
  protected void assertCredentialsMatch(final AuthenticationToken token, final AuthenticationInfo info)
      throws AuthenticationException
  {
    super.assertCredentialsMatch(token, info);
    //after successful assertion it means we authenticated successfully, so now we can set attributes
    requestProvider.get().setAttribute(IS_TOKEN_AUTH_KEY, Boolean.TRUE);
    getPrincipals(token)
        .map(PrincipalCollection::getPrimaryPrincipal)
        .ifPresent(principal -> ((NexusApiKeyAuthenticationToken) token).setPrincipal(principal));
  }

  protected boolean isAnonymousSupported() {
    return false;
  }

  private Optional<PrincipalCollection> getPrincipals(final AuthenticationToken token) {
    return keyStore.getApiKeyByToken(format, (char[]) token.getCredentials())
        .map(ApiKey::getPrincipals);
  }

  private boolean anonymousAndSupported(final PrincipalCollection principals) {
    return ANONYMOUS_USER.equals(principals.getPrimaryPrincipal()) && isAnonymousSupported();
  }
}
