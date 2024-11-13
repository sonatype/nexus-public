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

import java.time.OffsetDateTime;

import org.sonatype.nexus.security.authc.apikey.ApiKey;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * A database-stored object representing the association between a {@link PrincipalCollection} and a Api Key (char[]).
 */
public interface ApiKeyInternal
    extends ApiKey
{
  /**
   * Returns the domain (e.g. npm, docker, etc) which the key is applicable to.
   */
  String getDomain();

  /**
   * Set the time at which this token was created
   */
  void setCreated(OffsetDateTime created);

  /**
   * Set the domain of the token
   */
  void setDomain(String domain);

  /**
   * Sets the principals for the token, the object may not be preserved and will be constructed from the string
   * representation of primary principal and the first realm.
   */
  void setPrincipals(PrincipalCollection principals);
}
