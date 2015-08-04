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
package org.sonatype.nexus.templates.repository.maven;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

public abstract class AbstractMavenRepositoryTemplate
    extends AbstractRepositoryTemplate
{
  private RepositoryPolicy repositoryPolicy;

  public AbstractMavenRepositoryTemplate(AbstractRepositoryTemplateProvider provider, String id, String description,
                                         ContentClass contentClass, Class<?> mainFacet,
                                         RepositoryPolicy repositoryPolicy)
  {
    super(provider, id, description, contentClass, mainFacet);

    setRepositoryPolicy(repositoryPolicy);
  }

  @Override
  public boolean targetFits(Object clazz) {
    return super.targetFits(clazz) || clazz.equals(getRepositoryPolicy());
  }

  public RepositoryPolicy getRepositoryPolicy() {
    return repositoryPolicy;
  }

  public void setRepositoryPolicy(RepositoryPolicy repositoryPolicy) {
    this.repositoryPolicy = repositoryPolicy;
  }

  @Override
  public MavenRepository create()
      throws ConfigurationException, IOException
  {
    MavenRepository mavenRepository = (MavenRepository) super.create();

    // huh? see initConfig classes
    if (getRepositoryPolicy() != null) {
      mavenRepository.setRepositoryPolicy(getRepositoryPolicy());
    }

    return mavenRepository;
  }
}
