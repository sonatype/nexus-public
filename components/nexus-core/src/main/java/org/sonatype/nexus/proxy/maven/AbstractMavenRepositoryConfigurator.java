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
package org.sonatype.nexus.proxy.maven;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepositoryConfigurator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.validator.FileTypeItemContentValidator;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractMavenRepositoryConfigurator
    extends AbstractProxyRepositoryConfigurator
{
  private ChecksumContentValidator checksumValidator;

  private FileTypeItemContentValidator fileTypeItemContentValidator;

  @Inject
  public void populateAbstractMavenRepositoryConfigurator(final ChecksumContentValidator checksumValidator,
                                                          final FileTypeItemContentValidator fileTypeItemContentValidator)
  {
    this.checksumValidator = checkNotNull(checksumValidator);
    this.fileTypeItemContentValidator = checkNotNull(fileTypeItemContentValidator);
  }

  @Override
  public void doApplyConfiguration(Repository repository, ApplicationConfiguration configuration,
                                   CRepositoryCoreConfiguration coreConfiguration)
      throws ConfigurationException
  {
    super.doApplyConfiguration(repository, configuration, coreConfiguration);

    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      ProxyRepository proxy = repository.adaptToFacet(ProxyRepository.class);

      proxy.getItemContentValidators().put("checksum", checksumValidator);
      proxy.getItemContentValidators().put("filetypevalidator", fileTypeItemContentValidator);
    }
  }
}
