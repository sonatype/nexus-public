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

import java.security.PublicKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;

/**
 * Matches authentication tokens which are {@link java.security.PublicKey}.
 *
 * @author hugo@josefson.org
 */
class PublicKeyCredentialsMatcher
    implements CredentialsMatcher
{

  public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
    PublicKeyWithEquals tokenKey = getTokenKey(token);
    Collection<PublicKeyWithEquals> infoKeys = getInfoKeys(info);
    for (PublicKeyWithEquals infoKey : infoKeys) {
      if (infoKey.equals(tokenKey)) {
        return true;
      }
    }
    return false;
  }

  protected PublicKeyWithEquals getTokenKey(AuthenticationToken token) {
    final PublicKeyAuthenticationToken publicKeyAuthentictionToken = (PublicKeyAuthenticationToken) token;
    return new PublicKeyWithEquals(publicKeyAuthentictionToken.getCredentials());
  }

  protected Collection<PublicKeyWithEquals> getInfoKeys(AuthenticationInfo info) {
    // TODO: check types so they are sure to be PublicKey
    final Set<PublicKeyWithEquals> result = new HashSet<PublicKeyWithEquals>();
    final Object credentials = info.getCredentials();
    if (Collection.class.isAssignableFrom(credentials.getClass())) {
      Collection<PublicKey> credentialsCollection = (Collection<PublicKey>) credentials;
      for (PublicKey publicKey : credentialsCollection) {
        result.add(new PublicKeyWithEquals(publicKey));
      }
    }
    else {
      result.add(new PublicKeyWithEquals((PublicKey) credentials));
    }
    return result;
  }
}
