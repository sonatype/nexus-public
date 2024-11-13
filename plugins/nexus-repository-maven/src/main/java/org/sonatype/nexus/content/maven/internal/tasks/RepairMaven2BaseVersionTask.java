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
package org.sonatype.nexus.content.maven.internal.tasks;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.scheduling.Cancelable;

@Named
public class RepairMaven2BaseVersionTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  @Override
  protected void execute(final Repository repository) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    Iterable<FluentComponent> componentsWithMissedBaseVersion = mavenContentFacet.getComponentsWithMissedBaseVersion();
    for (FluentComponent fluentComponent : componentsWithMissedBaseVersion) {
      Maven2ComponentData componentData = new Maven2ComponentData();
      componentData.setNamespace(fluentComponent.namespace());
      componentData.setName(fluentComponent.name());
      componentData.setVersion(fluentComponent.version());
      componentData.setRepositoryId(mavenContentFacet.contentRepositoryId());
      NestedAttributesMap maven2 = fluentComponent.attributes("maven2");
      componentData.setBaseVersion(maven2.get("baseVersion", String.class));
      mavenContentFacet.updateBaseVersion(componentData);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.getFormat().getValue().equals(Maven2Format.NAME);
  }

  @Override
  public String getMessage() {
    return "Fixed Maven Base Versions of " + getRepositoryField();
  }
}
