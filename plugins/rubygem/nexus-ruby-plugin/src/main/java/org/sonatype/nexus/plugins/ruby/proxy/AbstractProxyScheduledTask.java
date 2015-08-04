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
package org.sonatype.nexus.plugins.ruby.proxy;

import java.util.List;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

public abstract class AbstractProxyScheduledTask
    extends AbstractNexusRepositoriesTask<Object>
{
  protected abstract void doRun(ProxyRubyRepository rubyRepository) throws Exception;

  @Override
  public Object doRun() throws Exception {
    if (getRepositoryId() != null) {
      Repository repository = getRepositoryRegistry().getRepository(getRepositoryId());

      // is this a proxied rubygems repository at all?
      if (repository.getRepositoryKind().isFacetAvailable(ProxyRubyRepository.class)) {
        ProxyRubyRepository rubyRepository = repository.adaptToFacet(ProxyRubyRepository.class);
        doRun(rubyRepository);
      }
      else {
        getLogger().info(
                "Repository {} is not a Rubygems Proxy repository. Will not execute anything, but the task seems wrongly configured!",
                repository);
      }
    }
    else {
      List<ProxyRubyRepository> reposes = getRepositoryRegistry().getRepositoriesWithFacet(ProxyRubyRepository.class);

      for (ProxyRubyRepository repo : reposes) {
        doRun(repo);
      }
    }

    return null;
  }
}