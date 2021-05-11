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

import javax.cache.configuration.Factory;
import javax.cache.expiry.ExpiryPolicy;

import org.sonatype.goodies.common.ComponentSupport;

/**
 * Abstracts cache builder to contain all of the getters/setters
 *
 * @since 3.14
 */
public abstract class AbstractCacheBuilder<K, V>
    extends ComponentSupport
    implements CacheBuilder<K, V>
{
  protected String name;

  protected Factory<? extends ExpiryPolicy> expiryFactory;

  protected int cacheSize = 10000;

  protected boolean storeByValue = false;

  protected boolean managementEnabled = true;

  protected boolean statisticsEnabled = true;

  protected Class<K> keyType;

  protected Class<V> valueType;

  protected BiConsumer<K, V> persister;

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Class<K> getKeyType() {
    return this.keyType;
  }

  @Override
  public Class<V> getValueType() {
    return this.valueType;
  }

  @Override
  public CacheBuilder<K, V> name(final String name) {
    this.name = name;
    return this;
  }

  @Override
  public CacheBuilder<K, V> cacheSize(final int cacheSize) {
    this.cacheSize = cacheSize;
    return this;
  }

  @Override
  public CacheBuilder<K, V> expiryFactory(final Factory<? extends ExpiryPolicy> expiryFactory) {
    this.expiryFactory = expiryFactory;
    return this;
  }

  @Override
  public CacheBuilder<K, V> storeByValue(final boolean storeByValue) {
    this.storeByValue = storeByValue;
    return this;
  }

  @Override
  public CacheBuilder<K, V> managementEnabled(final boolean enabled) {
    this.managementEnabled = enabled;
    return this;
  }

  @Override
  public CacheBuilder<K, V> statisticsEnabled(final boolean enabled) {
    this.statisticsEnabled = enabled;
    return this;
  }

  @Override
  public CacheBuilder<K, V> keyType(final Class<K> keyType) {
    this.keyType = keyType;
    return this;
  }

  @Override
  public CacheBuilder<K, V> valueType(final Class<V> valueType) {
    this.valueType = valueType;
    return this;
  }

  @Override
  public CacheBuilder<K, V> persister(final BiConsumer<K, V> persister) {
    this.persister = persister;
    return this;
  }
}
