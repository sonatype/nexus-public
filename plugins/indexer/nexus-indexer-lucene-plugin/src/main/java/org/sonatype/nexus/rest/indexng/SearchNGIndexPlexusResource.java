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
package org.sonatype.nexus.rest.indexng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.index.Searcher;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.index.AbstractIndexerNexusPlexusResource;
import org.sonatype.nexus.rest.model.NexusNGArtifact;
import org.sonatype.nexus.rest.model.NexusNGArtifactHit;
import org.sonatype.nexus.rest.model.NexusNGArtifactLink;
import org.sonatype.nexus.rest.model.NexusNGRepositoryDetail;
import org.sonatype.nexus.rest.model.SearchNGResponse;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.lucene.queryParser.ParseException;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named(SearchNGIndexPlexusResource.ROLE_HINT)
@Singleton
@Path(SearchNGIndexPlexusResource.RESOURCE_URI)
public class SearchNGIndexPlexusResource
    extends AbstractIndexerNexusPlexusResource
{
  public static final String ROLE_HINT = "SearchNGIndexPlexusResource";

  /**
   * Capping the number of Lucene Documents to process, to avoid potention problems and DOS-like attacks. If someone
   * needs more results, download the index instead and process it in-situ.
   */
  private static final int LUCENE_HIT_LIMIT = 5000;

  /**
   * Hard upper limit of the count of search hits delivered over REST API. In short: how many rows user sees in UI
   * max. Note: this does not correspond to ArtifactInfo count! This is GA count that may be backed by a zillion
   * ArtifactInfos! Before (old resource) this was 200 (well, the max count of ROWS user would see was 200).
   */
  private static final int DEFAULT_GA_HIT_LIMIT = 200;

  /**
   * The actual limit value, that may be overridden by users using Java System Properties, and defaults to
   * DEFAULT_GA_HIT_LIMIT.
   */
  private static final int GA_HIT_LIMIT = SystemPropertiesHelper.getInteger("plexus.search.ga.hit.limit",
      DEFAULT_GA_HIT_LIMIT);

  /**
   * Time to spend in 1st processing loop before bail out. It defaults to 30sec (UI timeout is 60secs).
   */
  private static final long DEFAULT_FIRST_LOOP_EXECUTION_TIME_LIMIT = 30000;

  /**
   * The actual time limit to spend in search, that may be overridden by users using Java System Properties, and
   * defaults to DEFAULT_FIRST_LOOP_EXECUTION_TIME_LIMIT.
   */
  private static final long FIRST_LOOP_EXECUTION_TIME_LIMIT = SystemPropertiesHelper.getLong(
      "plexus.search.ga.firstLoopTime", DEFAULT_FIRST_LOOP_EXECUTION_TIME_LIMIT);

  /**
   * The default threshold of change size in relevance, from where we may "cut" the results.
   */
  private static final int DEFAULT_DOCUMENT_RELEVANCE_HIT_CHANGE_THRESHOLD = 500;

  /**
   * The threshold of change size in relevance, from where we may "cut" the results.
   */
  private static final float DOCUMENT_RELEVANCE_HIT_CHANGE_THRESHOLD = (float) SystemPropertiesHelper.getInteger(
      "plexus.search.ga.hit.relevanceDropThreshold", DEFAULT_DOCUMENT_RELEVANCE_HIT_CHANGE_THRESHOLD) / 1000f;

  /**
   * The default treshold of change from the very 1st hit. from where we may "cut" the results.
   */
  private static final int DEFAULT_DOCUMENT_TOP_RELEVANCE_HIT_CHANGE_THRESHOLD = 750;

  /**
   * The treshold of change from the very 1st hit. from where we may "cut" the results.
   */
  private static final float DOCUMENT_TOP_RELEVANCE_HIT_CHANGE_THRESHOLD = (float) SystemPropertiesHelper.getInteger(
      "plexus.search.ga.hit.topRelevanceDropThreshold", DEFAULT_DOCUMENT_TOP_RELEVANCE_HIT_CHANGE_THRESHOLD) / 1000f;

  /**
   * The default treshold, that is used to "uncollapse" the collapsed results (if less hits than threshold).
   */
  private static final int DEFAULT_COLLAPSE_OVERRIDE_TRESHOLD = 35;

  /**
   * The treshold, that is used to "uncollapse" the collapsed results (if less hits than threshold).
   */
  private static final int COLLAPSE_OVERRIDE_TRESHOLD = SystemPropertiesHelper.getInteger(
      "plexus.search.ga.collapseOverrideThreshold", DEFAULT_COLLAPSE_OVERRIDE_TRESHOLD);

  public static final String RESOURCE_URI = "/lucene/search";

  private Logger searchDiagnosticLogger = LoggerFactory.getLogger("search.ng.diagnostic");

  private final List<Searcher> searchers;

  @Inject
  public SearchNGIndexPlexusResource(final List<Searcher> searchers) {
    this.searchers = searchers;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:index]");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  /**
   * Search against all repositories using provided parameters. Note there are a few different types of searches you
   * can perform. If you provide the 'q' query parameter, a keyword search will be performed. If you provide the 'g,
   * a, v, p or c' query parameters, a maven coordinate search will be performed. If you provide the 'cn' query
   * parameter, a classname search will be performed. If you provide the 'sha1' query parameter, a checksum search
   * will be performed.
   *
   * @param q            provide this param for a keyword search (g, a, v, p, c, cn, sha1 params will be ignored).
   * @param sha1         provide this param for a checksum search (g, a, v, p, c, cn params will be ignored).
   * @param cn           provide this param for a classname search (g, a, v, p, c params will be ignored).
   * @param g            group id to perform a maven search against (can be combined with a, v, p & c params as
   *                     well).
   * @param a            artifact id to perform a maven search against (can be combined with g, v, p & c params as
   *                     well).
   * @param v            version to perform a maven search against (can be combined with g, a, p & c params as well).
   * @param p            packaging type to perform a maven search against (can be combined with g, a, v & c params as
   *                     well).
   * @param c            classifier to perform a maven search against (can be combined with g, a, v & p params as
   *                     well).
   * @param from         result index to start retrieving results from.
   * @param count        number of results to have returned to you.
   * @param repositoryId The repositoryId to which repository search should be narrowed. Omit if search should be
   *                     global.
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {
      @QueryParam("q"), @QueryParam("g"), @QueryParam("a"),
      @QueryParam("v"), @QueryParam("p"), @QueryParam("c"), @QueryParam("cn"), @QueryParam("sha1"),
      @QueryParam("from"), @QueryParam("count"), @QueryParam("repositoryId")
  }, output = SearchNGResponse.class)
  public SearchNGResponse get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    final Map<String, String> terms = new HashMap<String, String>();

    for (Parameter parameter : form) {
      terms.put(parameter.getName(), parameter.getValue());
    }

    Integer from = null;
    Boolean exact = null;
    String repositoryId = null;
    boolean expandVersion = false;
    boolean collapseResults = false;

    if (form.getFirstValue("from") != null) {
      try {
        from = Integer.valueOf(form.getFirstValue("from"));
      }
      catch (NumberFormatException e) {
        from = null;
      }
    }

    int count = LUCENE_HIT_LIMIT;
    if (form.getFirstValue("count") != null) {
      try {
        // capping the possible count
        count = Math.min(LUCENE_HIT_LIMIT, Integer.valueOf(form.getFirstValue("count")));
      }
      catch (NumberFormatException e) {
        count = LUCENE_HIT_LIMIT;
      }
    }

    if (form.getFirstValue("repositoryId") != null) {
      repositoryId = form.getFirstValue("repositoryId");
    }

    if (form.getFirstValue("exact") != null) {
      exact = Boolean.valueOf(form.getFirstValue("exact"));
    }

    if (form.getFirstValue("versionexpand") != null) {
      expandVersion = Boolean.valueOf(form.getFirstValue("versionexpand"));
    }
    if (form.getFirstValue("collapseresults") != null) {
      collapseResults = Boolean.valueOf(form.getFirstValue("collapseresults"));
    }

    boolean forceExpand = expandVersion || !collapseResults;

    try {
      try {
        IteratorSearchResponse searchResult =
            searchByTerms(terms, repositoryId, from, count, exact, searchers);

        try {
          SearchNGResponse searchResponse = packSearchNGResponse(request, terms, searchResult, forceExpand);
          searchResponse.setTotalCount(searchResult.getTotalHitsCount());
          searchResponse.setFrom(from == null ? -1 : from.intValue());
          searchResponse.setCount(count == LUCENE_HIT_LIMIT ? -1 : count);
          return searchResponse;
        }
        finally {
          searchResult.close();
        }
      }
      catch (IOException e) {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
      }
    }
    catch (NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Repository to be searched does not exists!",
          e);
    }
  }

  protected Logger getSearchDiagnosticLogger() {
    return searchDiagnosticLogger;
  }

  @VisibleForTesting
  /* UT */IteratorSearchResponse searchByTerms(final Map<String, String> terms, final String repositoryId,
                                               final Integer from, final int count, final Boolean exact,
                                               final List<Searcher> searchers)
      throws NoSuchRepositoryException, ResourceException, IOException
  {
    try {
      Searcher searcher = null;

      for (Searcher _searcher : searchers) {
        if (_searcher.canHandle(terms)) {
          searcher = _searcher;
        }
      }

      if (searcher == null) {
        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Requested search query is not supported");
      }

      SearchType searchType = searcher.getDefaultSearchType();

      if (exact != null) {
        searchType = exact ? SearchType.EXACT : SearchType.SCORED;
      }

      final IteratorSearchResponse searchResponse =
          searcher.flatIteratorSearch(terms, repositoryId, from, count, null, false, searchType, null/* filters */);

      return searchResponse;
    }
    catch (IllegalArgumentException e) {
      if (e.getCause() instanceof ParseException) {
        // NEXUS-4372: illegal query -> 400 response
        throw new PlexusResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getCause(),
            getNexusErrorResponse("search", e.getCause().getMessage()));
      }
      else {
        throw e;
      }
    }
  }

  protected SearchNGResponse packSearchNGResponse(Request request, Map<String, String> terms,
                                                  IteratorSearchResponse iterator, boolean forceExpand)
  {
    // GA -> [version] -> [repository] -> [classified, extension]
    Map<String, GAHolder> gahits = new LinkedHashMap<String, GAHolder>();

    final long startedAtMillis = System.currentTimeMillis();
    float firstDocumentScore = -1f;
    float lastDocumentScore = -1f;

    int gavcount = 0;

    // tooManyResults true when iteration was aborted due to one of limiters
    // response will contain partial info and may have incorrect latest version and classifiers/extensions.
    boolean tooManyResults = false;

    for (ArtifactInfo ai : iterator) {
      tooManyResults = true;

      // we stop if query takes too long
      if (System.currentTimeMillis() - startedAtMillis > FIRST_LOOP_EXECUTION_TIME_LIMIT) {
        getSearchDiagnosticLogger().debug(
            "Stopping delivering search results since we spent more than " + FIRST_LOOP_EXECUTION_TIME_LIMIT
                + " millis in 1st loop processing results.");

        break;
      }

      // we stop if we delivered "most important" hits (change of relevance from 1st document we got)
      if (gahits.size() > 10
          && (firstDocumentScore - ai.getLuceneScore()) > DOCUMENT_TOP_RELEVANCE_HIT_CHANGE_THRESHOLD) {
        getSearchDiagnosticLogger().debug(
            "Stopping delivering search results since we span " + DOCUMENT_TOP_RELEVANCE_HIT_CHANGE_THRESHOLD
                + " of score change (firstDocScore=" + firstDocumentScore + ", currentDocScore="
                + ai.getLuceneScore() + ").");

        break;
      }

      // we stop if we detect a "big drop" in relevance in relation to previous document's score
      if (gahits.size() > 10 && lastDocumentScore > 0) {
        if ((lastDocumentScore - ai.getLuceneScore()) > DOCUMENT_RELEVANCE_HIT_CHANGE_THRESHOLD) {
          getSearchDiagnosticLogger().debug(
              "Stopping delivering search results since we hit a relevance drop bigger than "
                  + DOCUMENT_RELEVANCE_HIT_CHANGE_THRESHOLD + " (lastDocScore=" + lastDocumentScore
                  + ", currentDocScore=" + ai.getLuceneScore() + ").");

          // the relevance change was big, so we stepped over "trash" results that are
          // probably not relevant at all, just stop here then
          break;
        }
      }

      // we stop if we hit the GA limit
      if ((gahits.size() + 1) > GA_HIT_LIMIT) {
        getSearchDiagnosticLogger().debug(
            "Stopping delivering search results since we hit a GA hit limit of " + GA_HIT_LIMIT + ".");

        // check for HIT_LIMIT: if we are stepping it over, stop here
        break;
      }

      if (firstDocumentScore < 0) {
        firstDocumentScore = ai.getLuceneScore();
      }
      lastDocumentScore = ai.getLuceneScore();

      final String gakey = ai.groupId + ":" + ai.artifactId;

      GAHolder gaholder = gahits.get(gakey);
      if (gaholder == null) {
        gaholder = new GAHolder();
        gahits.put(gakey, gaholder);

      }

      StringVersion version = new StringVersion(ai.version, ai.getArtifactVersion());

      NexusNGArtifact versionHit = gaholder.getVersionHit(version);
      if (versionHit == null) {
        versionHit = new NexusNGArtifact();
        versionHit.setGroupId(ai.groupId);
        versionHit.setArtifactId(ai.artifactId);
        versionHit.setVersion(ai.version);
        versionHit.setHighlightedFragment(getMatchHighlightHtmlSnippet(ai));

        gaholder.putVersionHit(version, versionHit);

        gavcount++;
      }

      NexusNGArtifactHit repositoryHit = getRepositoryHit(versionHit, ai.repository);
      if (repositoryHit == null) {
        repositoryHit = new NexusNGArtifactHit();
        repositoryHit.setRepositoryId(ai.repository);
        versionHit.addArtifactHit(repositoryHit);

        // we are adding the POM link "blindly", unless packaging is POM,
        // since the it will be added below the "usual" way
        if (!"pom".equals(ai.packaging)) {
          repositoryHit.addArtifactLink(createNexusNGArtifactLink(request, ai.repository, ai.groupId,
              ai.artifactId, ai.version, "pom", null));
        }
      }

      repositoryHit.addArtifactLink(createNexusNGArtifactLink(request, ai.repository, ai.groupId,
          ai.artifactId, ai.version, ai.fextension, ai.classifier));

      tooManyResults = false;
    }

    // summary:
    getSearchDiagnosticLogger().debug(
        "Query terms \"" + terms + "\" (LQL \"" + iterator.getQuery() + "\") matched total of "
            + iterator.getTotalHitsCount() + " records, " + iterator.getTotalProcessedArtifactInfoCount()
            + " records were processed out of those, resulting in " + gahits.size()
            + " unique GA records. Lucene scored documents first=" + firstDocumentScore + ", last="
            + lastDocumentScore + ". Main processing loop took " + (System.currentTimeMillis() - startedAtMillis)
            + " ms.");

    // expand if explicitly requested or if number of unique GAV matches is less than threshold
    boolean expand = forceExpand || gavcount <= COLLAPSE_OVERRIDE_TRESHOLD;

    SearchNGResponse response = new SearchNGResponse();
    response.setTooManyResults(tooManyResults || iterator.getTotalHitsCount() + 1 >= LUCENE_HIT_LIMIT);
    response.setCollapsed(!expand);

    List<NexusNGArtifact> responseData = new ArrayList<NexusNGArtifact>();

    LinkedHashSet<String> repositoryIds = new LinkedHashSet<String>();

    for (GAHolder gahit : gahits.values()) {
      if (expand) {
        // expand, i.e. include all versions
        for (NexusNGArtifact artifact : gahit.getOrderedVersionHits()) {
          responseData.add(artifact);
          setLatest(artifact, gahit.getLatestRelease(), gahit.getLatestSnapshot());
          addRepositoryIds(repositoryIds, artifact);
        }
      }
      else {
        // collapse, i.e. only include latest version in each GA
        NexusNGArtifact artifact = gahit.getLatestVersionHit();
        setLatest(artifact, gahit.getLatestRelease(), gahit.getLatestSnapshot());
        responseData.add(artifact);
        addRepositoryIds(repositoryIds, artifact);
      }
    }

    response.setData(responseData);

    List<NexusNGRepositoryDetail> repoDetails = new ArrayList<NexusNGRepositoryDetail>();

    for (String repositoryId : repositoryIds) {
      Repository repository;
      try {
        repository = getUnprotectedRepositoryRegistry().getRepository(repositoryId);
        addRepositoryDetails(repoDetails, request, repository);
      }
      catch (NoSuchRepositoryException e) {
        // XXX can't happen, can it?
      }
    }

    response.setRepoDetails(repoDetails);

    return response;
  }

  private NexusNGArtifactHit getRepositoryHit(NexusNGArtifact artifact, String repositoryId) {
    for (NexusNGArtifactHit repositoryHit : artifact.getArtifactHits()) {
      if (repositoryId.equals(repositoryHit.getRepositoryId())) {
        return repositoryHit;
      }
    }
    return null;
  }

  private void addRepositoryIds(LinkedHashSet<String> repositoryIds, NexusNGArtifact artifact) {
    for (NexusNGArtifactHit repositoryHit : artifact.getArtifactHits()) {
      repositoryIds.add(repositoryHit.getRepositoryId());
    }
  }

  private void setLatest(NexusNGArtifact artifact, NexusNGArtifact latestRelease, NexusNGArtifact latestSnapshot) {
    if (latestRelease != null) {
      artifact.setLatestRelease(latestRelease.getVersion());
      artifact.setLatestReleaseRepositoryId(latestRelease.getArtifactHits().get(0).getRepositoryId());
    }
    if (latestSnapshot != null) {
      artifact.setLatestSnapshot(latestSnapshot.getVersion());
      artifact.setLatestSnapshotRepositoryId(latestSnapshot.getArtifactHits().get(0).getRepositoryId());
    }
  }

  private void addRepositoryDetails(List<NexusNGRepositoryDetail> repoDetails, Request request, Repository repository) {
    boolean add = true;

    for (NexusNGRepositoryDetail repoDetail : repoDetails) {
      if (repoDetail.getRepositoryId().equals(repository.getId())) {
        add = false;
        break;
      }
    }

    if (add) {
      NexusNGRepositoryDetail repoDetail = new NexusNGRepositoryDetail();

      repoDetail.setRepositoryId(repository.getId());

      repoDetail.setRepositoryName(repository.getName());

      repoDetail.setRepositoryURL(createRepositoryReference(request, repository.getId()).getTargetRef().toString());

      repoDetail.setRepositoryContentClass(repository.getRepositoryContentClass().getId());

      repoDetail.setRepositoryKind(extractRepositoryKind(repository));

      MavenRepository mavenRepo = repository.adaptToFacet(MavenRepository.class);

      if (mavenRepo != null) {
        repoDetail.setRepositoryPolicy(mavenRepo.getRepositoryPolicy().name());
      }

      repoDetails.add(repoDetail);
    }
  }

  protected NexusNGArtifactLink createNexusNGArtifactLink(final Request request, final String repositoryId,
                                                          final String groupId, final String artifactId,
                                                          final String version, final String extension,
                                                          final String classifier)
  {
    NexusNGArtifactLink link = new NexusNGArtifactLink();

    link.setExtension(extension);

    link.setClassifier(classifier);

    return link;
  }

  protected String extractRepositoryKind(Repository repository) {
    RepositoryKind kind = repository.getRepositoryKind();

    if (kind.isFacetAvailable(HostedRepository.class)) {
      return "hosted";
    }
    else if (kind.isFacetAvailable(ProxyRepository.class)) {
      return "proxy";
    }
    else if (kind.isFacetAvailable(GroupRepository.class)) {
      return "group";
    }
    else if (kind.isFacetAvailable(ShadowRepository.class)) {
      return "virtual";
    }
    else {
      // huh?
      return repository.getRepositoryKind().getMainFacet().getName();
    }
  }
}