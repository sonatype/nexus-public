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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.npm.internal.NpmAuditTarballFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.vulnerability.exceptions.TarballLoadingException;

import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Data store {@link NpmAuditTarballFacet}
 *
 * @since 3.29
 */
@Named
public class DataStoreNpmAuditTarballFacet
    extends NpmAuditTarballFacet
{
  @Inject
  public DataStoreNpmAuditTarballFacet(
      @Named("${nexus.npm.audit.maxConcurrentRequests:-10}") final int maxConcurrentRequests)
  {
    super(maxConcurrentRequests);
  }

  @Override
  protected Optional<String> getComponentHashsumForProxyRepo(final Repository repository, final Context context)
      throws TarballLoadingException
  {
    try {
      return getComponentHashsum(repository, context);
    }
    catch (IOException e) {
      throw new TarballLoadingException(e.getMessage());
    }
  }

  @Override
  protected Optional<String> getHashsum(final AttributesMap attributes) {
    return ofNullable(attributes.get(Asset.class))
        .map(Asset::blob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(AssetBlob::checksums)
        .map(checksums -> checksums.get(SHA1.name()));
  }
}
