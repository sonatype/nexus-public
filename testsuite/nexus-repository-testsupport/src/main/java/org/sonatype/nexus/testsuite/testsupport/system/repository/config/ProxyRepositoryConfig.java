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
package org.sonatype.nexus.testsuite.testsupport.system.repository.config;

public interface ProxyRepositoryConfig<THIS>
    extends RepositoryConfig<THIS>
{
  THIS withBlocked(final Boolean blocked);

  Boolean isBlocked();

  THIS withAutoBlocked(final Boolean autoBlocked);

  Boolean isAutoBlocked();

  THIS withUsername(final String username);

  String getUsername();

  THIS withPassword(final String password);

  String getPassword();

  THIS withRemoteUrl(final String remoteUrl);

  String getRemoteUrl();

  THIS withPullReplication();

  Boolean isPreemptivePullEnabled();

  THIS withAssetPathRegex(String assetPathRegex);

  String getAssetPathRegex();

  THIS withContentMaxAge(final Integer contentMaxAge);

  Integer getContentMaxAge();

  THIS withMetadataMaxAge(final Integer metadataMaxAge);

  Integer getMetadataMaxAge();

  THIS withNegativeCacheEnabled(final Boolean negativeCacheEnabled);

  Boolean isNegativeCacheEnabled();

  THIS withNegativeCacheTimeToLive(final Integer negativeCacheTimeToLive);

  Integer getNegativeCacheTimeToLive();
}
