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
package org.sonatype.nexus.rest.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.NexusArtifact;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorResultSet;
import org.apache.maven.index.MatchHighlight;
import org.restlet.data.Request;

public abstract class AbstractIndexerNexusPlexusResource
    extends AbstractNexusPlexusResource
{
  @Override
  public void configureXStream(XStream xstream) {
    super.configureXStream(xstream);
    MIndexerXStreamConfiguratorLightweight.configureXStream(xstream);
  }

  /**
   * Convert a collection of ArtifactInfo's to NexusArtifacts
   */
  protected Collection<NexusArtifact> ai2NaColl(Request request, Collection<ArtifactInfo> aic) {
    if (aic == null) {
      return null;
    }

    List<NexusArtifact> result = new ArrayList<NexusArtifact>();

    for (ArtifactInfo ai : aic) {
      NexusArtifact na = ai2Na(request, ai);

      if (na != null) {
        result.add(na);
      }
    }
    return result;
  }

  protected Collection<NexusArtifact> ai2NaColl(Request request, IteratorResultSet aic) {
    List<NexusArtifact> result = new ArrayList<NexusArtifact>();

    if (aic != null) {
      for (ArtifactInfo ai : aic) {
        NexusArtifact na = ai2Na(request, ai);

        if (na != null) {
          result.add(na);
        }
      }
    }

    return result;
  }

  protected String getMatchHighlightHtmlSnippet(ArtifactInfo ai) {
    if (ai.getMatchHighlights().size() > 0) {
      // <blockquote>Artifact classes
      // <ul>
      // <li>aaaa</li>
      // <li>bbbbb</li>
      // </ul>
      // </blockquote>

      StringBuilder sb = new StringBuilder();

      for (MatchHighlight mh : ai.getMatchHighlights()) {
        sb.append("<blockquote>").append(mh.getField().getDescription()).append("<UL>");

        // TODO: fix this!
        for (String high : mh.getHighlightedMatch()) {
          sb.append("<LI>").append(high).append("</LI>");
        }

        sb.append("</UL></blockquote>");
      }

      return sb.toString();
    }
    else {
      return null;
    }
  }

  /**
   * Convert from ArtifactInfo to a NexusArtifact
   */
  protected NexusArtifact ai2Na(Request request, ArtifactInfo ai) {
    if (ai == null) {
      return null;
    }

    NexusArtifact a = new NexusArtifact();

    a.setGroupId(ai.groupId);

    a.setArtifactId(ai.artifactId);

    a.setVersion(ai.version);

    a.setClassifier(ai.classifier);

    a.setPackaging(ai.packaging);

    a.setExtension(ai.fextension);

    a.setRepoId(ai.repository);

    a.setContextId(ai.context);

    a.setHighlightedFragment(getMatchHighlightHtmlSnippet(ai));

    if (ai.repository != null) {
      a.setPomLink(createPomLink(request, ai));

      a.setArtifactLink(createArtifactLink(request, ai));

      try {
        Repository repository = getUnprotectedRepositoryRegistry().getRepository(ai.repository);

        if (MavenRepository.class.isAssignableFrom(repository.getClass())) {
          MavenRepository mavenRepository = (MavenRepository) repository;

          Gav gav =
              new Gav(ai.groupId, ai.artifactId, ai.version, ai.classifier,
                  mavenRepository.getArtifactPackagingMapper().getExtensionForPackaging(ai.packaging),
                  null, null, null, false, null, false, null);

          ResourceStoreRequest req =
              new ResourceStoreRequest(mavenRepository.getGavCalculator().gavToPath(gav));

          a.setResourceURI(createRepositoryReference(request, ai.repository, req.getRequestPath()).toString());
        }
      }
      catch (NoSuchRepositoryException e) {
        getLogger().warn("No such repository: '" + ai.repository + "'.", e);

        return null;
      }
    }

    return a;
  }

  protected String createPomLink(Request request, ArtifactInfo ai) {
    if (StringUtils.isNotEmpty(ai.classifier)) {
      return "";
    }

    String suffix =
        "?r=" + ai.repository + "&g=" + ai.groupId + "&a=" + ai.artifactId + "&v=" + ai.version + "&e=pom";

    return createRedirectBaseRef(request).toString() + suffix;
  }

  protected String createArtifactLink(Request request, ArtifactInfo ai) {
    if (StringUtils.isEmpty(ai.packaging) || "pom".equals(ai.packaging)) {
      return "";
    }

    String suffix =
        "?r=" + ai.repository + "&g=" + ai.groupId + "&a=" + ai.artifactId + "&v=" + ai.version + "&e="
            + ai.fextension;

    if (StringUtils.isNotBlank(ai.classifier)) {
      suffix = suffix + "&c=" + ai.classifier;
    }

    return createRedirectBaseRef(request).toString() + suffix;
  }
}
