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

import org.sonatype.nexus.repository.search.SearchSubjectHelper;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.script.ScriptModule;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Elasticsearch plugin that exposes a content auth function for working with content selectors in search. Also holds
 * on to necessary instances as static variables that should be set by the matching {@link ContentAuthPluginLocator},
 * as this class is instantiated by ES, not by us.
 *
 * @since 3.1
 */
public class ContentAuthPlugin
    extends Plugin
{
  private static ContentPermissionChecker contentPermissionChecker;
  private static VariableResolverAdapterManager variableResolverAdapterManager;
  private static SearchSubjectHelper searchSubjectHelper;

  public ContentAuthPlugin() {
    checkNotNull(contentPermissionChecker);
    checkNotNull(variableResolverAdapterManager);
    checkNotNull(searchSubjectHelper);
  }

  @Override
  public String name() {
    return "content-auth-plugin";
  }

  @Override
  public String description() {
    return "ES plugin for working with content selectors";
  }

  public void onModule(final ScriptModule module) {
    module.registerScript(ContentAuthPluginScript.NAME, ContentAuthPluginScriptFactory.class);
  }

  public static void setDependencies(final ContentPermissionChecker contentPermissionChecker,
                                     final VariableResolverAdapterManager variableResolverAdapterManager,
                                     final SearchSubjectHelper searchSubjectHelper)
  {
    ContentAuthPlugin.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    ContentAuthPlugin.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    ContentAuthPlugin.searchSubjectHelper = checkNotNull(searchSubjectHelper);
  }

  public static ContentPermissionChecker getContentPermissionChecker() {
    return contentPermissionChecker;
  }

  public static VariableResolverAdapterManager getVariableResolverAdapterManager() {
    return variableResolverAdapterManager;
  }

  public static SearchSubjectHelper getSearchSubjectHelper() {
    return searchSubjectHelper;
  }
}
