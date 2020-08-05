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
package org.sonatype.nexus.repository.content.search;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Optional.empty;

/**
 * @since 3.26
 */
@Named
@Singleton
public class DefaultComponentFinder
    extends ComponentSupport
    implements ComponentFinder
{
  @Override
  public Stream<FluentComponent> findComponentsByModel(
      final Repository repository,
      final String searchComponentId,
      final String namespace,
      final String name,
      final String version)
  {
    Optional<FluentComponent> component = empty();

    if (repository != null) {
      FluentComponents components = contentFacet(repository).components();

      if (searchComponentId != null) {
        component = components.find(EntityHelper.id(searchComponentId));
      }

      if (!component.isPresent()) {
        component = components
            .name(name)
            .namespace(nullToEmpty(namespace))
            .version(nullToEmpty(version))
            .find();
      }
    }

    return component.map(Stream::of).orElse(Stream.empty());
  }

  protected ContentFacet contentFacet(final Repository repository) {
    return repository.facet(ContentFacet.class);
  }
}
