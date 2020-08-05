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
package org.sonatype.nexus.content.maven.internal.index;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link MavenIndexFacet} support.
 *
 * @since 3.26
 */
public abstract class MavenContentIndexFacetSupport
    extends FacetSupport
    implements MavenIndexFacet
{

  final MavenIndexPublisher mavenIndexPublisher;

  protected MavenContentIndexFacetSupport(final MavenIndexPublisher mavenIndexPublisher) {
    this.mavenIndexPublisher = checkNotNull(mavenIndexPublisher);
  }

  @Nullable
  @Override
  public DateTime lastPublished() throws IOException {
    return mavenIndexPublisher.lastPublished(getRepository());
  }

  @Override
  public void unpublishIndex() throws IOException {
    mavenIndexPublisher.unpublishIndexFiles(getRepository());
  }
}
