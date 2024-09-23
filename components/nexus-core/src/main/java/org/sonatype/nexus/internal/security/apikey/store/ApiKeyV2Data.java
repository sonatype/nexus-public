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
package org.sonatype.nexus.internal.security.apikey.store;

import java.time.OffsetDateTime;

import javax.annotation.Nullable;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;

import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link ApiKeyInternal} data for use with {@link ApiKeyStoreV2Impl}
 */
public class ApiKeyV2Data
    implements ApiKeyInternal
{
  private PrincipalCollection principals;

  private String username;

  private String domain;

  private String accessKey;

  private Secret secret;

  private OffsetDateTime created;

  ApiKeyV2Data() { }

  ApiKeyV2Data(
      final String domain,
      final PrincipalCollection principals,
      final String accessKey,
      final Secret secret,
      @Nullable final OffsetDateTime created)
  {
    this.domain = checkNotNull(domain);
    this.principals = checkNotNull(principals);
    this.username = principals.getPrimaryPrincipal().toString();
    this.accessKey = checkNotNull(accessKey);
    this.secret = checkNotNull(secret);
    this.created = created;
  }

  public String getAccessKey() {
    return accessKey;
  }

  @Override
  public char[] getApiKey() {
    int keyLength = accessKey.length();
    char[] secretPart = secret.decrypt();
    char[] token = new char[keyLength + secretPart.length];

    System.arraycopy(accessKey.toCharArray(), 0, token, 0, keyLength);
    System.arraycopy(secretPart, 0, token, keyLength, secretPart.length);

    return token;
  }

  @Override
  public OffsetDateTime getCreated() {
    return created;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public PrincipalCollection getPrincipals() {
    return principals;
  }

  public Secret getSecret() {
    return secret;
  }

  public String getUsername() {
    return username;
  }

  public void setAccessKey(final String accessKey) {
    this.accessKey = checkNotNull(accessKey);
  }

  @Override
  public void setCreated(final OffsetDateTime created) {
    this.created = created;
  }

  @Override
  public void setDomain(final String domain) {
    this.domain = domain;
  }

  @Override
  public void setPrincipals(final PrincipalCollection principals) {
    this.principals = checkNotNull(principals);
    this.username = principals.getPrimaryPrincipal().toString();
  }

  public void setSecret(final Secret secret) {
    this.secret = secret;
  }
}
