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
package org.sonatype.nexus.templates.repository;

import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.registry.ContentClass;

public class ManuallyConfiguredRepositoryTemplate
    extends AbstractRepositoryTemplate
{
  private final CRepositoryCoreConfiguration repoConfig;

  public ManuallyConfiguredRepositoryTemplate(AbstractRepositoryTemplateProvider provider, String id,
                                              String description, ContentClass contentClass, Class<?> mainFacet,
                                              CRepositoryCoreConfiguration repoConfig)
  {
    super(provider, id, description, contentClass, mainFacet);

    this.repoConfig = repoConfig;
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    return repoConfig;
  }
}
