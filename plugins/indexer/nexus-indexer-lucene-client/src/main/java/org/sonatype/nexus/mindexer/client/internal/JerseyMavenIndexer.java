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
package org.sonatype.nexus.mindexer.client.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.mindexer.client.SearchRequest;
import org.sonatype.nexus.mindexer.client.SearchResponse;
import org.sonatype.nexus.mindexer.client.SearchResponseArtifact;
import org.sonatype.nexus.mindexer.client.SearchResponseRepository;
import org.sonatype.nexus.rest.index.MIndexerXStreamConfiguratorLightweight;
import org.sonatype.nexus.rest.model.NexusNGArtifact;
import org.sonatype.nexus.rest.model.NexusNGArtifactHit;
import org.sonatype.nexus.rest.model.NexusNGArtifactLink;
import org.sonatype.nexus.rest.model.NexusNGRepositoryDetail;
import org.sonatype.nexus.rest.model.SearchNGResponse;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Provides Maven Indexer REST Client.
 *
 * @author cstamas
 */
public class JerseyMavenIndexer
    extends JerseyMavenIndexerSupport
{

  public JerseyMavenIndexer(final JerseyNexusClient client) {
    super(client);
    MIndexerXStreamConfiguratorLightweight.configureXStream(client.getXStream());
  }

  @Override
  public SearchResponse search(final SearchRequest request) {
    final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    final Map<String, String> terms = request.getQuery().toMap();
    for (Map.Entry<String, String> te : terms.entrySet()) {
      queryParams.add(te.getKey(), te.getValue());
    }
    if (request.getQuery().getExact() != null) {
      queryParams.add("exact", request.getQuery().getExact().toString());
    }
    if (request.getFrom() != null) {
      queryParams.add("from", request.getFrom().toString());
    }
    if (request.getCount() != null) {
      queryParams.add("count", request.getCount().toString());
    }
    if (request.getRepositoryId() != null) {
      queryParams.add("repositoryId", request.getRepositoryId());
    }
    if (request.getVersionexpand() != null) {
      queryParams.add("versionexpand", request.getVersionexpand().toString());
    }
    if (request.getCollapseresults() != null) {
      queryParams.add("collapseresults", request.getCollapseresults().toString());
    }

    try {
      return toResponse(
          request,
          getNexusClient()
              .serviceResource("lucene/search", queryParams)
              .get(SearchNGResponse.class)
      );
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  /**
   * The method simple "converts" (or flattens?) the Nexus REST resource response, to make it more usable. The
   * response of resource is structured sadly to mostly fulfill UI needs, and not for programmatic processing.
   */
  protected SearchResponse toResponse(final SearchRequest searchRequest, final SearchNGResponse response) {
    final HashMap<String, SearchResponseRepository> repositories = new HashMap<String, SearchResponseRepository>();
    for (NexusNGRepositoryDetail repoDetail : response.getRepoDetails()) {
      repositories.put(repoDetail.getRepositoryId(), new SearchResponseRepository(repoDetail.getRepositoryId(),
          repoDetail.getRepositoryName(), repoDetail.getRepositoryContentClass(), repoDetail.getRepositoryURL()));
    }
    final ArrayList<SearchResponseArtifact> hits = new ArrayList<SearchResponseArtifact>();
    for (NexusNGArtifact responseHit : response.getData()) {
      for (NexusNGArtifactHit responseArtifactHit : responseHit.getArtifactHits()) {
        for (NexusNGArtifactLink responseArtifactLink : responseArtifactHit.getArtifactLinks()) {
          // "pom" records are generated for stupid client, they are NOT
          // coming from index but rather are injected on the fly
          if (responseArtifactHit.getArtifactLinks().size() > 1
              && !"pom".equals(responseArtifactLink.getExtension())) {
            SearchResponseArtifact artifact =
                new SearchResponseArtifact(responseHit.getGroupId(), responseHit.getArtifactId(),
                    responseHit.getVersion(), responseArtifactLink.getClassifier(),
                    responseArtifactLink.getExtension(),
                    repositories.get(responseArtifactHit.getRepositoryId()),
                    responseHit.getLatestSnapshot(), responseHit.getLatestSnapshotRepositoryId(),
                    responseHit.getLatestRelease(), responseHit.getLatestReleaseRepositoryId(),
                    responseHit.getHighlightedFragment());
            hits.add(artifact);
          }
        }
      }
    }
    final Integer from = response.getFrom() != -1 ? response.getFrom() : null;
    final Integer count = response.getCount() != -1 ? response.getCount() : null;
    return new SearchResponse(searchRequest, response.getTotalCount(), from, count, response.isTooManyResults(),
        response.isCollapsed(), hits);
  }
}
