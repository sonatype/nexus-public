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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.browse.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.browse.internal.resources.doc.SearchResourceDoc;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonatype.nexus.repository.browse.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.browse.internal.resources.SearchResource.RESOURCE_URI;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResource
    extends ComponentSupport
    implements Resource, SearchResourceDoc
{
  public static final String RESOURCE_URI = BETA_API_PREFIX + "/search";

  private static final Map<String, String> SEARCH_PARAMS = ImmutableMap.<String, String>builder()
      // common
      .put("repository", REPOSITORY_NAME)
      .put("format", "format")
      .put("group", "group.raw")
      .put("name", "name.raw")
      .put("version", "version")
      .put("md5", "assets.attributes.checksum.md5")
      .put("sha1", "assets.attributes.checksum.sha1")
      .put("sha256", "assets.attributes.checksum.sha256")
      .put("sha512", "assets.attributes.checksum.sha512")
      // Maven
      .put("maven.groupId", "attributes.maven2.groupId")
      .put("maven.artifactId", "attributes.maven2.artifactId")
      .put("maven.baseVersion", "attributes.maven2.baseVersion")
      .put("maven.extension", "assets.attributes.maven2.extension")
      .put("maven.classifier", "assets.attributes.maven2.classifier")
      // Nuget
      .put("nuget.id", "attributes.nuget.id")
      .put("nuget.tags", "assets.attributes.nuget.tags")
      // NPM
      .put("npm.scope", "group")
      // Docker
      .put("docker.imageName", "attributes.docker.imageName")
      .put("docker.imageTag", "attributes.docker.imageTag")
      .put("docker.layerId", "attributes.docker.layerAncestry")
      .put("docker.contentDigest", "attributes.docker.content_digest")
      // PyPi
      .put("pypi.classifiers", "assets.attributes.pypi.classifiers")
      .put("pypi.description", "assets.attributes.pypi.description")
      .put("pypi.keywords", "assets.attributes.pypi.keywords")
      .put("pypi.summary", "assets.attributes.pypi.summary")
      // RubyGems
      .put("rubygems.description", "assets.attributes.rubygems.description")
      .put("rubygems.platform", "assets.attributes.rubygems.platform")
      .put("rubygems.summary", "assets.attributes.rubygems.summary")
      .build();

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final BrowseService browseService;

  private final SearchService searchService;

  private static final int PAGE_SIZE = 50;

  private final Type groupType;

  private final TokenEncoder tokenEncoder;

  @Inject
  public SearchResource(final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                        final BrowseService browseService,
                        final SearchService searchService,
                        @Named(GroupType.NAME) final Type groupType,
                        final TokenEncoder tokenEncoder)
  {
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.browseService = checkNotNull(browseService);
    this.searchService = checkNotNull(searchService);
    this.groupType = checkNotNull(groupType);
    this.tokenEncoder = checkNotNull(tokenEncoder);
  }

  @GET
  public Page<ComponentXO> search(
      @QueryParam("continuationToken") final String continuationToken,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = buildQuery(uriInfo);
    log.debug("Query: {}", query);

    int from = tokenEncoder.decode(continuationToken, query);
    SearchResponse response = searchService.search(query, emptyList(), from, PAGE_SIZE);

    List<ComponentXO> componentXOs = Arrays.stream(response.getHits().hits())
        .map(this::toComponent)
        .collect(toList());

    return new Page<>(componentXOs, componentXOs.size() == PAGE_SIZE ?
        tokenEncoder.encode(from, PAGE_SIZE, query) : null);
  }

  private ComponentXO toComponent(final SearchHit hit) {
    Map<String, Object> source = checkNotNull(hit.getSource());
    Repository repository = repositoryManagerRESTAdapter.getRepository((String) source.get(REPOSITORY_NAME));
    ComponentXO componentXO = new ComponentXO();

    componentXO
        .setAssets(browseService.browseComponentAssets(repository, hit.getId())
            .getResults()
            .stream()
            .map(asset -> fromAsset(asset, repository))
            .collect(toList()));

    componentXO.setGroup((String) source.get(GROUP));
    componentXO.setName((String) source.get(NAME));
    componentXO.setVersion((String) source.get(VERSION));
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), hit.getId()).getValue());
    componentXO.setRepository(repository.getName());
    componentXO.setFormat(repository.getFormat().getValue());

    return componentXO;
  }

  /**
   * Builds a {@link QueryBuilder} based on configured search parameters.
   *
   * @param uriInfo {@link UriInfo} to extract query parameters from
   */
  @VisibleForTesting
  QueryBuilder buildQuery(final UriInfo uriInfo) {
    BoolQueryBuilder query = boolQuery();

    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

    if (queryParams.containsKey("q")) {
      query.must(queryStringQuery(queryParams.getFirst("q")));
    }

    queryParams.forEach((key, value) -> {
      if (!SEARCH_PARAMS.containsKey(key) || value.isEmpty()) {
        // no search param of that name, or no value sent
        return;
      }
      if ("repository".equals(key)) {
        Repository repository = repositoryManagerRESTAdapter.getRepository(value.get(0));
        if (groupType.equals(repository.getType())) {
          repository.facet(GroupFacet.class).leafMembers().forEach(r ->
              query.should(termQuery(SEARCH_PARAMS.get(key), r.getName())));
          query.minimumNumberShouldMatch(1);
          return;
        }
      }
      query.filter(termQuery(SEARCH_PARAMS.get(key), value.get(0)));
    });

    return query;
  }

}
