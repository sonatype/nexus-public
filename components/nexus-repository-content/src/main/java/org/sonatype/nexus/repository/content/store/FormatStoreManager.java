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
package org.sonatype.nexus.repository.content.store;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;

/**
 * Manages content stores for a particular format. To get the {@link FormatStoreManager} for format "example", inject
 * <p>
 * {@code @Named("example") FormatStoreManager}
 * <p>
 * Content stores can exist in different datastores (unlike config stores that only exist in the config datastore)
 * so you need to pass in the datastore name. The manager knows which DAO and store classes to use for the format.
 *
 * @since 3.24
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FormatStoreManager
{
  private static final Class<?>[] KNOWN_DAOS = {
      ContentRepositoryDAO.class,
      ComponentDAO.class,
      AssetDAO.class,
      AssetBlobDAO.class };

  private final Cache<String, ContentStoreSupport<?>> cachedStores = CacheBuilder.newBuilder().weakValues().build();

  private final Provider<FormatStoreFactory> factoryProvider;

  private final Map<Class<?>, Class<?>> formatDaoMap;

  FormatStoreManager(final Provider<FormatStoreFactory> factoryProvider, final Class<?>[] daoClasses) {
    this.factoryProvider = checkNotNull(factoryProvider);
    this.formatDaoMap = indexDAOs(checkNotNull(daoClasses));
  }

  /**
   * Gets the {@link ContentRepositoryStore} for this format in the named datastore.
   */
  public <T extends ContentRepositoryStore<?>> T contentRepositoryStore(final String contentStoreName) {
    return (T) getFormatStore(contentStoreName, ContentRepositoryDAO.class);
  }

  /**
   * Gets the {@link ComponentStore} for this format in the named datastore.
   */
  public <T extends ComponentStore<?>> T componentStore(final String contentStoreName) {
    return (T) getFormatStore(contentStoreName, ComponentDAO.class);
  }

  /**
   * Gets the {@link AssetStore} for this format in the named datastore.
   */
  public <T extends AssetStore<?>> T assetStore(final String contentStoreName) {
    return (T) getFormatStore(contentStoreName, AssetDAO.class);
  }

  /**
   * Gets the {@link AssetBlobStore} for this format in the named datastore.
   */
  public <T extends AssetBlobStore<?>> T assetBlobStore(final String contentStoreName) {
    return (T) getFormatStore(contentStoreName, AssetBlobDAO.class);
  }

  /**
   * Gets the format-specific store for the named datastore and type of DAO (component/asset/etc...)
   * If the store doesn't exist it is created and cached for other repositories in the same datastore.
   */
  private ContentStoreSupport<?> getFormatStore(final String contentStoreName, final Class<?> daoClass) {
    try {
      return cachedStores.get(contentStoreName + '/' + daoClass.getName(),
          () -> createFormatStore(contentStoreName, daoClass));
    }
    catch (ExecutionException e) {
      throw new UncheckedExecutionException(e.getCause());
    }
  }

  /**
   * Creates a format-specific store for the named datastore and type of DAO (component/asset/etc...)
   */
  private ContentStoreSupport<?> createFormatStore(final String contentStoreName, final Class<?> daoClass) {
    FormatStoreFactory factory = factoryProvider.get();
    Class<?> formatDaoClass = formatDaoMap.get(daoClass);

    if (ContentRepositoryDAO.class.equals(daoClass)) {
      return factory.contentRepositoryStore(contentStoreName, formatDaoClass);
    }
    else if (ComponentDAO.class.equals(daoClass)) {
      return factory.componentStore(contentStoreName, formatDaoClass);
    }
    else if (AssetDAO.class.equals(daoClass)) {
      return factory.assetStore(contentStoreName, formatDaoClass);
    }
    else if (AssetBlobDAO.class.equals(daoClass)) {
      return factory.assetBlobStore(contentStoreName, formatDaoClass);
    }
    else {
      throw new IllegalArgumentException("Unexpected DAO class: " + daoClass);
    }
  }

  /**
   * Indexes the format-specific DAOs by whichever known DAO interface they extend.
   */
  private static Map<Class<?>, Class<?>> indexDAOs(final Class<?>[] daoClasses) {
    ImmutableMap.Builder<Class<?>, Class<?>> index = ImmutableMap.builder();
    for (Class<?> knownDao : KNOWN_DAOS) {
      index.put(knownDao, stream(daoClasses)
          .filter(knownDao::isAssignableFrom)
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Format does not define " + knownDao)));
    }
    return index.build();
  }
}
