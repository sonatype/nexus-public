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
package org.sonatype.nexus.testsuite.testsupport.fixtures

import javax.annotation.Nonnull

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration

import groovy.transform.CompileStatic

/**
 * Factory for Go {@link Repository} {@link Configuration}
 */
@CompileStatic
trait GolangRepoRecipes
    extends ConfigurationRecipes
{
  @Nonnull
  Repository createGolangProxy(final String name,
                               final String remoteUrl)
  {
    createRepository(createProxy(name, 'go-proxy', remoteUrl))
  }

  @Nonnull
  Repository createGolangHosted(final String name)
  {
    createRepository(createHosted(name, 'go-hosted'))
  }

  @Nonnull
  Repository createGolangGroup(final String name, String ...members) {
    createRepository(createGroup(name, 'go-group', members))
  }

  abstract Repository createRepository(final Configuration configuration)
}
