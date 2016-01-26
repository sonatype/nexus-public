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
package org.sonatype.nexus.repository.selector;

import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.JexlSelector;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;

import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.repository.HasFacet.hasFacet;

/**
 * Invalidate caches base on a selector.
 *
 * @since 3.0
 */
@Named
@Command(name = "cache-invalidate", scope = "nexus", description = "Invalidate caches base on a selector expression")
public class SelectorCacheAction
    implements Action
{
  private enum ContentType {
    repository
  }

  private enum CacheType {
    proxy,
    nfc
  }

  @Inject
  private RepositoryManager repositoryManager;

  @Argument(index = 0, name = "contentType", description = "type of content being selected", required = true)
  ContentType contentType;

  @Argument(index = 1, name = "cacheType", description = "type of cache being invalidated", required = true)
  CacheType cacheType;

  @Argument(index = 2, name = "expression", description = "repository selector expression")
  JexlSelector selector = new JexlSelector("");

  @Override
  public Object execute() throws Exception {
    if (contentType == ContentType.repository) {
        doRepository();
    }
    return null;
  }

  private void doRepository() {
    stream(repositoryManager.browse().spliterator(), true)
      .filter(r -> selector.evaluate(ConstantVariableResolver.sourceFor(r, "repository")))
      .filter(hasCache())
      .forEach(invalidate());
  }

  private Predicate<Repository> hasCache() {
    switch (cacheType) {
      case proxy:
        return hasFacet(ProxyFacet.class);
      case nfc:
        return hasFacet(NegativeCacheFacet.class);
      default:
        return r -> false;
    }
  }

  private Consumer<Repository> invalidate() {
    switch (cacheType) {
      case proxy:
        return r -> r.facet(ProxyFacet.class).invalidateProxyCaches();
      case nfc:
        return r -> r.facet(NegativeCacheFacet.class).invalidate();
      default:
        return r -> {};
    }
  }
}
