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
package org.sonatype.nexus.repository.selector.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.elasticsearch.PluginLocator;
import org.sonatype.nexus.repository.search.SearchSubjectHelper;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;

import org.elasticsearch.plugins.Plugin;

/**
 * {@link PluginLocator} for {@link ContentAuthPlugin}. Also responsible for setting some required objects into static
 * fields on {@link ContentAuthPlugin} as the instantiation of the latter occurs outside of our purview within ES.
 *
 * @since 3.1
 */
@Named
@Singleton
public class ContentAuthPluginLocator
    implements PluginLocator
{
  @Inject
  public ContentAuthPluginLocator(final ContentPermissionChecker contentPermissionChecker,
                                  final VariableResolverAdapterManager variableResolverAdapterManager,
                                  final SearchSubjectHelper searchSubjectHelper)
  {
    ContentAuthPlugin.setDependencies(contentPermissionChecker, variableResolverAdapterManager,
        searchSubjectHelper);
  }

  @Override
  public Class<? extends Plugin> pluginClass() {
    return ContentAuthPlugin.class;
  }
}
