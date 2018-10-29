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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.SearchResultComponent;
import org.sonatype.nexus.repository.search.SearchResultComponentGeneratorSupport;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;

import org.elasticsearch.search.SearchHit;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.ATTRIBUTES;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;

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
  public SearchResultComponent from(final SearchHit hit, final Set<String> componentIdSet) {
    SearchResultComponent component = null;
    final Map<String, Object> source = hit.getSource();
    String repositoryName = getPrivilegedRepositoryName(source);
    String group = (String) source.get(GROUP);
    String name = (String) source.get(NAME);
    String format = (String) source.get(FORMAT);
    Optional<String> baseVersion = Optional.ofNullable(source.get(ATTRIBUTES))
        .map(attributes -> ((Map) attributes).get("maven2"))
        .map(maven2 -> ((Map) maven2).get("baseVersion"))
        .map(Object::toString);
    String baseVersionString = baseVersion.orElse("");
    String baseVersionId = baseVersionString.isEmpty() ? "" :
        repositoryName + ":" + group + ":" + name + ":" + baseVersionString;

    if (baseVersionId.isEmpty() || (!componentIdSet.contains(baseVersionId))) {
      boolean isSnapshot = isSnapshotId(baseVersionId);
      component = new SearchResultComponent();

      component.setId(isSnapshot ? baseVersionId : hit.getId());
      component.setRepositoryName(repositoryName);
      component.setGroup(group);
      component.setName(name);
      component.setFormat(format);

      if (baseVersionString.isEmpty()) {
        component.setVersion((String) source.get(VERSION));
      }
      else {
        component.setVersion(baseVersionString);
      }
    }

    return component;
  }

  public static boolean isSnapshotId(String id) {
    return nonNull(id) && id.endsWith("-SNAPSHOT");
  }
}
