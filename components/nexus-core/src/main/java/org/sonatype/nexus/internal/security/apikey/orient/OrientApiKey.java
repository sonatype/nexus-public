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
import java.util.Arrays;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.security.authc.apikey.ApiKey;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * An Orient-stored object representing the association between a {@link PrincipalCollection} and a Api Key (char[]).
 *
 * @since 3.0
 */
public class OrientApiKey
    extends AbstractEntity
    implements ApiKey
{
  private String domain;

  private PrincipalCollection principals;

  private char[] apiKey;

  private OffsetDateTime created;

  OrientApiKey() {
    // package-private constructor
  }

  public void setDomain(final String domain) {
    this.domain = domain;
  }

  public void setPrincipals(final PrincipalCollection principals) {
    this.principals = principals;
  }

  public void setApiKey(final char[] apiKey) {
    this.apiKey = Arrays.copyOf(apiKey, apiKey.length);
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public PrincipalCollection getPrincipals() {
    return principals;
  }

  @Override
  public char[] getApiKey() {
    return Arrays.copyOf(apiKey, apiKey.length);
  }

  @Override
  public OffsetDateTime getCreated() {
    return created;
  }

  public void setCreated(final OffsetDateTime created) {
    this.created = created;
  }
}
