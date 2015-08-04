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
package org.sonatype.nexus.obr.proxy;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepositoryConfigurator;
import org.sonatype.nexus.proxy.repository.Repository;

@Named
@Singleton
public class ObrRepositoryConfigurator
    extends AbstractProxyRepositoryConfigurator
{
  @Override
  public void doApplyConfiguration(final Repository repository, final ApplicationConfiguration configuration,
                                   final CRepositoryCoreConfiguration coreConfig)
      throws ConfigurationException
  {
    repository.setIndexable(false);

    super.doApplyConfiguration(repository, configuration, coreConfig);

    final CRemoteStorage remoteStorage = coreConfig.getConfiguration(true).getRemoteStorage();

    if (remoteStorage != null) {
      // // FIXME: on the fly upgrade, if needed
      // // it will trigger if detects that nexus.xml contains remoteUrl _with_ OBR XML file
      // String[] siteAndPath = ObrUtils.splitObrSiteAndPath( remoteStorage.getUrl(), false );
      //
      // if ( siteAndPath[1] != null )
      // {
      // // upgrade needed!
      // ( (ObrProxyRepository) repository ).setObrPath( siteAndPath[1] );
      //
      // // write back the stripped URL
      // remoteStorage.setUrl( siteAndPath[0] );
      // }

      // FIXME: this should happen in this super's class: AbstractProxyRepositoryConfigurator
      try {
        ((ObrProxyRepository) repository).setRemoteUrl(remoteStorage.getUrl());
      }
      catch (final StorageException e) {
        throw new ConfigurationException("Cannot configure OBR Proxy Repository! " + remoteStorage.getUrl(), e);
      }
    }
  }
}
