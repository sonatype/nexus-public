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
package org.sonatype.nexus.rest.artifact;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.model.Maven2ArtifactInfoResource;
import org.sonatype.nexus.rest.model.Maven2ArtifactInfoResourceRespose;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Request;

/**
 * Returns Maven2 artifact information.
 *
 * @author Brian Demers
 * @author cstamas
 */
@Named("maven2")
@Singleton
public class Maven2ArtifactViewProvider
    extends AbstractArtifactViewProvider
{
  public Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req)
      throws IOException
  {
    final Repository itemRepository = itemUid.getRepository();

    final String itemPath = itemUid.getPath();

    // is this a MavenRepository at all? If not, this view is not applicable
    if (!itemRepository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
      // this items comes from a non-maven repository, this view is not applicable
      return null;
    }
    else {
      // we need maven repository for this operation, but we actually don't care is this
      // maven2 or mave1 repository! Let's handle this in generic way.
      MavenRepository mavenRepository = itemRepository.adaptToFacet(MavenRepository.class);

      // use maven repository's corresponding GavCalculator instead of "wired in" one!
      Gav gav = mavenRepository.getGavCalculator().pathToGav(itemPath);

      if (gav == null || gav.isSignature() || gav.isHash()) {
        // if we cannot calculate the gav, it is not a maven artifact (or hash/sig), return null;
        return null;
      }

      // if we are here, we have GAV, so just pack it and send it back
      Maven2ArtifactInfoResourceRespose response = new Maven2ArtifactInfoResourceRespose();
      Maven2ArtifactInfoResource data = new Maven2ArtifactInfoResource();
      response.setData(data);

      data.setGroupId(gav.getGroupId());
      data.setArtifactId(gav.getArtifactId());
      data.setBaseVersion(gav.getBaseVersion());
      data.setVersion(gav.getVersion());
      data.setExtension(gav.getExtension());
      data.setClassifier(gav.getClassifier());

      data.setDependencyXmlChunk(generateDependencyXml(gav));

      return response;
    }
  }

  private String generateDependencyXml(Gav gav) {
    StringBuilder buffer = new StringBuilder();

    buffer.append("<dependency>\n");
    buffer.append("  <groupId>").append(gav.getGroupId()).append("</groupId>\n");
    buffer.append("  <artifactId>").append(gav.getArtifactId()).append("</artifactId>\n");
    buffer.append("  <version>").append(gav.getBaseVersion()).append("</version>\n");

    if (StringUtils.isNotEmpty(gav.getClassifier())) {
      buffer.append("  <classifier>").append(gav.getClassifier()).append("</classifier>\n");
    }

    if (StringUtils.isNotEmpty(gav.getExtension()) && !StringUtils.equalsIgnoreCase("jar", gav.getExtension())) {
      buffer.append("  <type>").append(gav.getExtension()).append("</type>\n");
    }

    buffer.append("</dependency>");

    return buffer.toString();
  }

}
