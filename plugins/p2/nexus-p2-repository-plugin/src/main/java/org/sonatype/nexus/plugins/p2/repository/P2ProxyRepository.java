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
package org.sonatype.nexus.plugins.p2.repository;

import java.util.Map;

import org.sonatype.nexus.plugins.p2.repository.mappings.ArtifactMapping;
import org.sonatype.nexus.plugins.p2.repository.metadata.P2MetadataSource;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

public interface P2ProxyRepository
    extends ProxyRepository, P2Repository
{
  P2MetadataSource<P2ProxyRepository> getMetadataSource();

  void initArtifactMappingsAndMirrors();

  Map<String, ArtifactMapping> getArtifactMappings()
      throws IllegalOperationException, StorageException;

  int getArtifactMaxAge();

  void setArtifactMaxAge(final int maxAge);

  int getMetadataMaxAge();

  void setMetadataMaxAge(final int metadataMaxAge);

  ChecksumPolicy getChecksumPolicy();

  void setChecksumPolicy(final ChecksumPolicy checksumPolicy);

  boolean isMetadataOld(StorageItem metadataItem);
}
