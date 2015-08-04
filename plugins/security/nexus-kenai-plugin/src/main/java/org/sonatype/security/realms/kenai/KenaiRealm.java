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
package org.sonatype.security.realms.kenai;

import java.io.IOException;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.security.realms.kenai.config.KenaiRealmConfiguration;

import com.google.common.collect.Lists;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Realm that connects to a java.net kenai API.
 *
 * @author Brian Demers
 */
@Singleton
@Typed(Realm.class)
@Named(KenaiRealm.ROLE)
@Description("Kenai Realm")
public class KenaiRealm
    extends AuthorizingRealm
{
  public static final String ROLE = "kenai";

  private static final Logger logger = LoggerFactory.getLogger(KenaiRealm.class);

  private final KenaiRealmConfiguration kenaiRealmConfiguration;

  private final Hc4Provider hc4Provider;

  @Inject
  public KenaiRealm(final KenaiRealmConfiguration kenaiRealmConfiguration,
                    final Hc4Provider hc4Provider)
  {
    this.kenaiRealmConfiguration = checkNotNull(kenaiRealmConfiguration);
    this.hc4Provider = checkNotNull(hc4Provider);
    setName(ROLE);

    // TODO: write another test before enabling this
    // this.setAuthenticationCachingEnabled( true );
  }

  // ------------ AUTHENTICATION ------------

  @Override
  public boolean supports(final AuthenticationToken token) {
    return (token instanceof UsernamePasswordToken);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
      throws AuthenticationException
  {
    final UsernamePasswordToken upToken = (UsernamePasswordToken) token;

    // if the user can authenticate we are good to go
    if (authenticateViaUrl(upToken)) {
      return buildAuthenticationInfo(upToken);
    }
    else {
      throw new AccountException("User \"" + upToken.getUsername()
          + "\" cannot be authenticated via Kenai Realm.");
    }
  }

  private AuthenticationInfo buildAuthenticationInfo(final UsernamePasswordToken token) {
    return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
  }

  private boolean authenticateViaUrl(final UsernamePasswordToken usernamePasswordToken) {
    final HttpClient client = hc4Provider.createHttpClient();

    try {
      final String url = kenaiRealmConfiguration.getConfiguration().getBaseUrl() + "api/login/authenticate.json";
      final List<NameValuePair> nameValuePairs = Lists.newArrayListWithCapacity(2);
      nameValuePairs.add(new BasicNameValuePair("username", usernamePasswordToken.getUsername()));
      nameValuePairs.add(new BasicNameValuePair("password", new String(usernamePasswordToken.getPassword())));
      final HttpPost post = new HttpPost(url);
      post.setEntity(new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8));
      final HttpResponse response = client.execute(post);

      try {
        logger.debug("Kenai Realm user \"{}\" validated against URL={} as {}", usernamePasswordToken.getUsername(), url,
            response.getStatusLine());
        final boolean success =
            response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299;
        return success;
      }
      finally {
        HttpClientUtils.closeQuietly(response);
      }
    }
    catch (IOException e) {
      logger.info("Kenai Realm was unable to perform authentication", e);
      return false;
    }
  }

  // ------------ AUTHORIZATION ------------

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    // only if authenticated with this realm too
    if (!principals.getRealmNames().contains(getName())) {
      return null;
    }
    // add the default role
    final SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
    authorizationInfo.addRole(kenaiRealmConfiguration.getConfiguration().getDefaultRole());
    return authorizationInfo;
  }
}
