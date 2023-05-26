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
package org.sonatype.nexus.repository.maven.internal.search;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.query.SearchResultComponentGeneratorSupport;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;

import static java.util.Objects.nonNull;

/**
 * @since 3.14
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2SearchResultComponentGenerator
    extends SearchResultComponentGeneratorSupport
{
  @Inject
  public Maven2SearchResultComponentGenerator(final VariableResolverAdapterManager variableResolverAdapterManager,
                                              final RepositoryManager repositoryManager,
                                              final ContentPermissionChecker contentPermissionChecker) {
    super(variableResolverAdapterManager, repositoryManager, contentPermissionChecker);
  }

  @Override
  public ComponentSearchResult from(final ComponentSearchResult hit, final Set<String> componentIdSet) {
    hit.setRepositoryName(getPrivilegedRepositoryName(hit));

    Optional<String> baseVersion = Optional.ofNullable(hit.getAnnotation("baseVersion"))
        .map(Object::toString)
        .filter(Objects::nonNull)
        .filter(((Predicate<String>) String::isEmpty).negate());

    if (baseVersion.isPresent()) {
      return createComponentForBaseVersion(hit, componentIdSet, baseVersion.get());
    }
    return hit;
  }

  private ComponentSearchResult createComponentForBaseVersion(
      final ComponentSearchResult hit,
      final Set<String> componentIdSet,
      final String baseVersion)
  {
    ComponentSearchResult component = null;
    String baseVersionId = hit.getRepositoryName() + ":" + hit.getGroup() + ":" + hit.getName() + ":" + baseVersion;

    if (!componentIdSet.contains(baseVersionId)) {
      boolean isSnapshot = isSnapshotId(baseVersionId);
      component = new ComponentSearchResult();

      component.setId(isSnapshot ? baseVersionId : hit.getId());
      component.setRepositoryName(hit.getRepositoryName());
      component.setGroup(hit.getGroup());
      component.setName(hit.getName());
      component.setFormat(hit.getFormat());
      component.setAssets(hit.getAssets());
      component.setLastModified(hit.getLastModified());

      component.setVersion(baseVersion);
    }

    return component;
  }

  public static boolean isSnapshotId(final String id) {
    return nonNull(id) && id.endsWith("-SNAPSHOT");
  }
}
