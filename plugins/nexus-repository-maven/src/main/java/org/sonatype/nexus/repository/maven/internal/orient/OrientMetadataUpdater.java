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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.IOException;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataUpdater;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * Maven 2 repository metadata updater with Orient DB transaction wrappers.
 *
 * @since 3.0
 */
public class OrientMetadataUpdater
    extends AbstractMetadataUpdater
{
  public OrientMetadataUpdater(final boolean update, final Repository repository) {
    super(update, repository);
  }

  @Override
  protected void update(final MavenPath mavenPath, final Maven2Metadata metadata) {
    try {
      TransactionalStoreBlob.operation.throwing(IOException.class).run(() -> {
        super.update(mavenPath, metadata);
      });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void replace(final MavenPath mavenPath, final Maven2Metadata metadata) {
    try {
      TransactionalStoreBlob.operation.throwing(IOException.class).run(() -> {
        super.replace(mavenPath, metadata);
      });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void write(final MavenPath mavenPath, final Metadata metadata) throws IOException {
    OrientMetadataUtils.write(repository, mavenPath, metadata);
  }

  @Override
  protected Optional<Metadata> read(final MavenPath mavenPath) throws IOException {
    return Optional.ofNullable(OrientMetadataUtils.read(repository, mavenPath));
  }

  @Override
  protected void delete(final MavenPath mavenPath) {
    OrientMetadataUtils.delete(repository, mavenPath);
  }
}
