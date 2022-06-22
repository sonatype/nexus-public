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
package org.sonatype.nexus.repository.content.kv;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.store.InternalIds;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.ATTACHED;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * Support class encapsulating the key-value store actions.
 */
public abstract class KeyValueFacetSupport
    extends FacetSupport
{
  private final String format;

  private FormatStoreManager formatStoreManager;

  private KeyValueStore<KeyValueDAO> dataStore;

  protected KeyValueFacetSupport(final String formatName) {
    this.format = checkNotNull(formatName);
  }

  @Inject
  public void inject(final Map<String, FormatStoreManager> formatStoreManagersByFormat)
  {
    this.formatStoreManager = checkNotNull(formatStoreManagersByFormat.get(format));
  }

  @Override
  protected void doStart() throws Exception {
    ContentFacetSupport contentFacet = (ContentFacetSupport) getRepository().facet(ContentFacet.class);

    String storeName = contentFacet.stores().contentStoreName;

    dataStore = formatStoreManager.formatStore(storeName, KeyValueDAO.class);
  }

  /**
   * Get the value for the key in the specified category.
   *
   * @param category the category for the property
   * @param key      the key for the desired value
   *
   * @return An optional containing the value, or empty if the value is unset.
   */
  @Guarded(by = {ATTACHED, STARTED})
  public Optional<String> get(final String category, final String key) {
    return dataStore.get(repositoryId(), category, key);
  }

  /**
   * Set the value for the key in the specified category.
   *
   * @param category  the category of this property
   * @param key       the key to store the value under
   * @param value     the value to store
   */
  @Guarded(by = {ATTACHED, STARTED})
  public void set(final String category, final String key, final String value) {
    dataStore.set(repositoryId(), category, key, value);
  }

  /**
   * Remove the stored value associated with the key in the specified category if it exists.
   *
   * @param category the category of the key-value pair
   * @param key      the key identifying the value
   */
  @Guarded(by = {ATTACHED, STARTED})
  public void remove(final String category, final String key) {
    dataStore.remove(repositoryId(), category, key);
  }

  /**
   * Remove all data in the specified category for the attached repository.
   *
   * @param category the category to remove associated content from
   */
  @Guarded(by = {ATTACHED, STARTED})
  public void removeAll(final String category) {
    dataStore.removeAll(repositoryId(), category);
  }

  /**
   * Browse all the values stored with a category.
   *
   * @param category          the category to browse content for
   * @param limit             the page size
   * @param continuationToken the continuation token for the page of results, or null for the first page
   *
   * @return the page of results.
   */
  @Guarded(by = {ATTACHED, STARTED})
  public Continuation<KeyValue> browseValues(
      final String category,
      final int limit,
      @Nullable final String continuationToken)
  {
    return dataStore.browse(repositoryId(), category, limit, continuationToken);
  }

  /**
   * Count all the values stored with a category
   *
   * @param category the category to count the content of
   * @return
   */
  @Guarded(by = {ATTACHED, STARTED})
  public int countValues(final String category)
  {
    return dataStore.count(repositoryId(), category);
  }

  private int repositoryId() {
    return InternalIds.contentRepositoryId(getRepository())
        .orElseThrow(IllegalStateException::new);
  }
}
