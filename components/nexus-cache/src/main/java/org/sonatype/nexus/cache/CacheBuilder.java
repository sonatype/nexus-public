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
package org.sonatype.nexus.cache;

import java.util.function.BiConsumer;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.expiry.ExpiryPolicy;

/**
 * Abstracts cache configurations to make using different underlying implementations easier
 * 
 * @since 3.14
 */
public interface CacheBuilder<K, V>
{
  String getName();

  Class<K> getKeyType();

  Class<V> getValueType();

  CacheBuilder<K, V> name(String name);

  CacheBuilder<K, V> cacheSize(int size);

  CacheBuilder<K, V> expiryFactory(Factory<? extends ExpiryPolicy> expiryFactory);

  CacheBuilder<K, V> storeByValue(boolean storeByValue);

  CacheBuilder<K, V> managementEnabled(boolean enabled);

  CacheBuilder<K, V> statisticsEnabled(boolean enabled);

  CacheBuilder<K, V> keyType(Class<K> keyType);

  CacheBuilder<K, V> valueType(Class<V> valueType);

  CacheBuilder<K, V> persister(BiConsumer<K, V> persister);

  Cache<K, V> build(CacheManager manager);
}
