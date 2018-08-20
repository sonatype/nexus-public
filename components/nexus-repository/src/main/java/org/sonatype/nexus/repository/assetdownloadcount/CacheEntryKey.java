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
package org.sonatype.nexus.repository.assetdownloadcount;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple key consisting of a reponame and assetname
 * CacheEntryKey is serializable so that it can be used in distributed caches
 *
 * @since 3.4
 */
public class CacheEntryKey implements Serializable
{

  private static final long serialVersionUID = 1L;

  private final String repositoryName;

  private final String assetName;

  public CacheEntryKey(final String repositoryName, final String assetName) {
    this.repositoryName = checkNotNull(repositoryName);
    this.assetName = checkNotNull(assetName);
  }

  public String getAssetName() {
    return assetName;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryName, assetName);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof CacheEntryKey)) {
      return false;
    }
    CacheEntryKey other = (CacheEntryKey) obj;
    if (other == this) {
      return true;
    }
    return Objects.equals(repositoryName, other.repositoryName) && Objects.equals(assetName, other.assetName);
  }

  @Override
  public String toString() {
    return "CacheEntryKey for Repository:" + repositoryName + " Asset:" + assetName;
  }
}
