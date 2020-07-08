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
package org.sonatype.nexus.repository.search.index;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Name search indexes by hashing the repository name; avoids characters not allowed by Elasticsearch.
 *
 * @since 3.25.0
 */
@Named
@Singleton
public class HashedNamingPolicy
    implements IndexNamingPolicy
{
  private final LoadingCache<String, String> cachedNames =
      CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(HashedNamingPolicy::hashedName));

  @Override
  public String indexName(final Repository repository) {
    return cachedNames.getUnchecked(repository.getName());
  }

  private static String hashedName(final String repositoryName) {
    return SHA1.function().hashUnencodedChars(repositoryName).toString();
  }
}
