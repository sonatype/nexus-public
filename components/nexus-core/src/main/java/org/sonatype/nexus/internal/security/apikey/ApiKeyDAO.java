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

import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * {@link ApiKeyData} access.
 *
 * @since 3.21
 */
public interface ApiKeyDAO
    extends DataAccess
{
  Iterable<PrincipalCollection> browsePrincipals();

  Optional<ApiKeyToken> findApiKey(@Param("domain") String domain, @Param("primaryPrincipal") String primaryPrincipal);

  Optional<PrincipalCollection> findPrincipals(@Param("domain") String domain, @Param("token") ApiKeyToken token);

  void save(ApiKeyData apiKeyData);

  boolean deleteDomainKey(@Param("domain") String domain, @Param("primaryPrincipal") String primaryPrincipal);

  boolean deleteKeys(@Param("primaryPrincipal") String primaryPrincipal);

  boolean deleteAllKeys();
}
