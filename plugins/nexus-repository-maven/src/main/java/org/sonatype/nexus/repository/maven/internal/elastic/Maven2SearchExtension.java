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
package org.sonatype.nexus.repository.maven.internal.elastic;

import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.elasticsearch.ElasticSearchExtension;

import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;

/**
 * An {@link ElasticSearchExtension} which annotations {@link ComponentSearchResult} with the maven baseVersion.
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2SearchExtension
    implements ElasticSearchExtension
{
  @Override
  public void updateComponent(final ComponentSearchResult component, final SearchHit hit) {
    if (Maven2Format.NAME.equals(component.getFormat())) {
      getBaseVersion(hit)
          .ifPresent(baseVersion -> component.addAnnotation(P_BASE_VERSION, baseVersion));
    }
  }

  private static Optional<String> getBaseVersion(final SearchHit hit) {
    Map<String, Object> source = checkNotNull(hit.getSource());

    return Optional.ofNullable(source.get("attributes"))
        .filter(attrs -> attrs instanceof Map)
        .map(Map.class::cast)
        .map(attrs -> attrs.get(Maven2Format.NAME))
        .filter(attrs -> attrs instanceof Map)
        .map(Map.class::cast)
        .map(attrs -> attrs.get(P_BASE_VERSION))
        .filter(baseVersion -> baseVersion instanceof String)
        .map(String.class::cast);
  }
}
