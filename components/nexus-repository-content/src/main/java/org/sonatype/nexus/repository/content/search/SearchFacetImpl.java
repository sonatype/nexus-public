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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.search.index.SearchIndexService;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Default {@link SearchFacet} implementation.
 *
 * @since 3.25
 */
@Named
public class SearchFacetImpl
    extends FacetSupport
    implements SearchFacet
{
  private final SearchIndexService searchIndexService;

  private final Map<String, SearchDocumentProducer> searchDocumentProducersByFormat;

  private final int pageSize;

  private final boolean bulkProcessing;

  private SearchDocumentProducer searchDocumentProducer;

  private Map<String, Object> repositoryFields;

  @Inject
  public SearchFacetImpl(final SearchIndexService searchIndexService,
                         final Map<String, SearchDocumentProducer> searchDocumentProducersByFormat,
                         @Named("${nexus.elasticsearch.reindex.pageSize:-1000}") final int pageSize,
                         @Named("${nexus.elasticsearch.bulkProcessing:-true}") final boolean bulkProcessing)
  {
    this.searchIndexService = checkNotNull(searchIndexService);
    this.searchDocumentProducersByFormat = checkNotNull(searchDocumentProducersByFormat);
    this.pageSize = max(pageSize, 1);
    this.bulkProcessing = bulkProcessing;
  }

  @Override
  protected void doInit(Configuration configuration) throws Exception {
    String format = getRepository().getFormat().getValue();

    searchDocumentProducer = lookupSearchDocumentProducer(format);
    repositoryFields = ImmutableMap.of(REPOSITORY_NAME, getRepository().getName(), FORMAT, format);

    super.doInit(configuration);
  }

  @Override
  protected void doStart() throws Exception {
    searchIndexService.createIndex(getRepository());
  }

  @Override
  protected void doDelete() {
    searchIndexService.deleteIndex(getRepository());
  }

  @Guarded(by = STARTED)
  @Override
  public void index(final Component component) {
    FluentComponent source = facet(ContentFacet.class).components().with(component);
    if (bulkProcessing) {
      searchIndexService.bulkPut(getRepository(), ImmutableList.of(source), this::identifier, this::document);
    }
    else {
      searchIndexService.put(getRepository(), identifier(source), document(source));
    }
  }

  @Guarded(by = STARTED)
  @Override
  public void purge(final int... componentIds) {
    searchIndexService.bulkDelete(getRepository(), stream(componentIds).mapToObj(this::identifier).collect(toList()));
  }

  @Guarded(by = STARTED)
  @Override
  public void rebuildIndex() {
    log.info("Rebuilding index of repository {}", getRepository().getName());

    searchIndexService.rebuildIndex(getRepository()); // clears out old documents

    rebuildComponentIndex();
  }

  /**
   * Re-submit search documents for every component in the repository for indexing.
   */
  private void rebuildComponentIndex() {
    String repositoryName = getRepository().getName();
    try {
      FluentComponents components = getRepository().facet(ContentFacet.class).components();

      long total = components.count();
      if (total > 0) {
        ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
        Stopwatch sw = Stopwatch.createStarted();

        long processed = 0;

        Continuation<FluentComponent> page = components.browse(pageSize, null);
        while (!page.isEmpty()) {

          searchIndexService.bulkPut(getRepository(), page, this::identifier, this::document);
          processed += page.size();

          long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
          progressLogger.info("Indexed {} / {} {} components in {} ms",
              processed, total, repositoryName, elapsed);

          checkCancellation();

          page = components.browse(pageSize, page.nextContinuationToken());
        }

        progressLogger.flush(); // ensure the final progress message is flushed
      }
    }
    catch (Exception e) {
      log.error("Unable to rebuild search index for repository {}", repositoryName, e);
    }
  }

  /**
   * Looks for the {@link SearchDocumentProducer} to use for the given repository format.
   */
  private SearchDocumentProducer lookupSearchDocumentProducer(final String format) {
    SearchDocumentProducer producer = searchDocumentProducersByFormat.get(format);
    if (producer == null) {
      producer = searchDocumentProducersByFormat.get("default");
    }
    checkState(producer != null, "Could not find a component metadata producer for format: %s", format);
    return producer;
  }

  /**
   * Returns the identifier for the given component in the repository's index.
   */
  private String identifier(final FluentComponent component) {
    return identifier(internalComponentId(component));
  }

  /**
   * Returns the identifier for the given component in the repository's index.
   */
  private String identifier(final int internalComponentId) {
    return toExternalId(internalComponentId).getValue();
  }

  /**
   * Returns the JSON document for the given component in the repository's index.
   */
  private String document(final FluentComponent component) {
    return searchDocumentProducer.getDocument(component, repositoryFields);
  }
}
