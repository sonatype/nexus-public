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
package org.sonatype.nexus.repository.maven.tasks;

import java.io.IOException;

import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.maven.MavenIndexFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import com.google.common.base.Throwables;

/**
 * Maven 2 un-publish MI indexes task.
 *
 * @since 3.0
 */
@Named
public class UnpublishMavenIndexTask
    extends RepositoryTaskSupport
{
  @Override
  protected void execute(final Repository repository) {
    MavenIndexFacet mavenIndexFacet = repository.facet(MavenIndexFacet.class);
    try {
      mavenIndexFacet.unpublishIndex();
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.getFormat().getValue().equals(Maven2Format.NAME);
  }

  @Override
  public String getMessage() {
    return "Remove Maven indexes of " + getRepositoryField();
  }
}
