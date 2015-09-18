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
package org.sonatype.nexus.repository.maven.internal;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.maven.internal.maven2.metadata.MetadataRebuilder;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link MavenHostedFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class MavenHostedFacetImpl
    extends FacetSupport
    implements MavenHostedFacet
{
  private final MetadataRebuilder metadataRebuilder;

  @Inject
  public MavenHostedFacetImpl(final MetadataRebuilder metadataRebuilder)
  {
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
  }

  @Override
  public void rebuildMetadata(@Nullable final String groupId,
                              @Nullable final String artifactId,
                              @Nullable final String baseVersion)
  {
    final boolean update = !Strings.isNullOrEmpty(groupId)
        || !Strings.isNullOrEmpty(artifactId)
        || !Strings.isNullOrEmpty(baseVersion);
    log.info("Rebuilding Maven2 repository metadata: repository={}, update={}, g={}, a={}, bV={}",
        getRepository().getName(), update, groupId, artifactId, baseVersion);
    metadataRebuilder.rebuild(getRepository(), update, groupId, artifactId, baseVersion);
  }
}
