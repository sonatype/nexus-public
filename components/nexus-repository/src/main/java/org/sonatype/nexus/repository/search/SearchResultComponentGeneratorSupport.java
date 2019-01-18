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
package org.sonatype.nexus.repository.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import org.elasticsearch.search.lookup.SourceLookup;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;

/**
 * @since 3.15
 */
public abstract class SearchResultComponentGeneratorSupport
  extends ComponentSupport
  implements SearchResultComponentGenerator
{
  final VariableResolverAdapterManager variableResolverAdapterManager;

  final RepositoryManager repositoryManager;

  final ContentPermissionChecker contentPermissionChecker;

  public SearchResultComponentGeneratorSupport(final VariableResolverAdapterManager variableResolverAdapterManager,
                                               final RepositoryManager repositoryManager,
                                               final ContentPermissionChecker contentPermissionChecker)
  {
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
  }

  protected String getPrivilegedRepositoryName(Map<String,Object> source) {
    String repositoryName = (String) source.get(REPOSITORY_NAME);
    String repositoryFormat = (String) source.get(FORMAT);

    List<Map<String, Object>> assets = (List<Map<String, Object>>) source.getOrDefault("assets", Collections.emptyList());

    SourceLookup lookup = new SourceLookup();
    lookup.setSource(source);

    if (assets != null && !assets.isEmpty()) {
      VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(repositoryFormat);
      VariableSource variableSource = variableResolverAdapter.fromSourceLookup(lookup, assets.get(0));
      List<String> repositoryNames = repositoryManager.findContainingGroups(repositoryName);
      repositoryNames.add(0, repositoryName);
      for (String name : repositoryNames) {
        if (contentPermissionChecker.isPermitted(name, repositoryFormat, BreadActions.BROWSE, variableSource)) {
          return name;
        }
      }
    }

    return null;
  }
}
