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
package org.sonatype.security.realms.publickey;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Shiro {@link org.apache.shiro.realm.Realm} for authenticating {@link java.security.PublicKey}s. Implement a
 * {@link PublicKeyRepository} in which you consult your own accounts backend, or use the
 * {@link com.sonatype.sshjgit.core.shiro.publickey.SimplePublicKeyRepository} for testing purposes. <BR/>
 * Originally implemented for use with SSHD, this realm compares two public keys for equality. It does NOT handle any
 * part of the TLS handshake. For more info on that <a
 * href="http://en.wikipedia.org/wiki/Transport_Layer_Security#Client-authenticated_TLS_handshake">see this wiki
 * page.<a/>
 *
 * @author hugo@josefson.org
 * @author Brian Demers
 * @see com.sonatype.sshjgit.core.shiro.publickey.PublicKeyRepository
 * @see org.apache.shiro.realm.Realm
 */
public class PublicKeyAuthenticatingRealm
    extends AuthorizingRealm
{
  protected static final Class<PublicKeyAuthenticationToken> AUTHENTICATION_TOKEN_CLASS =
      PublicKeyAuthenticationToken.class;

  protected PublicKeyRepository publicKeyRepository;

  /**
   * Default constructor needed for injection.
   */
  public PublicKeyAuthenticatingRealm() {
    setAuthenticationTokenClass(AUTHENTICATION_TOKEN_CLASS);
    setCredentialsMatcher(new PublicKeyCredentialsMatcher());
  }

  /**
   * Constructs this realm, accepting a {@code PublicKeyRepository} from which all keys will be fetched, and an
   * {@code Authorizer} to which all authorization will be delegated.
   *
   * @param publicKeyRepository public keys will be looked up from this.
   */
  public PublicKeyAuthenticatingRealm(PublicKeyRepository publicKeyRepository) {
    this();
    this.publicKeyRepository = publicKeyRepository;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException
  {
    final Object principal = token.getPrincipal();

    if (!publicKeyRepository.hasAccount(principal)) {
      return null;
    }

    return new SimpleAuthenticationInfo(principal, publicKeyRepository.getPublicKeys(principal), getName());
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    // No Authorization, just Authentication
    return null;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    // only support PublicKeyAuthenticationToken
    return PublicKeyAuthenticationToken.class.isInstance(token);
  }

  public void setPublicKeyRepository(PublicKeyRepository publicKeyRepository) {
    this.publicKeyRepository = publicKeyRepository;
  }

}
