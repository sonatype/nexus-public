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
package org.sonatype.nexus.proxy.attributes.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.attributes.Attributes;

import com.google.common.base.Preconditions;

/**
 * Default implementation of Attributes.
 * 
 * @author cstamas
 * @since 2.0
 */
public class DefaultAttributes
    implements Attributes
{
  private final HashMap<String, String> defaults;

  private final HashMap<String, String> values;

  public DefaultAttributes() {
    super();
    this.defaults = new HashMap<String, String>();
    this.values = new HashMap<String, String>();
  }

  public DefaultAttributes(final Map<String, String> m) {
    this();
    overlayMap(m);
  }

  // ==

  @Override
  public boolean containsKey(String key) {
    return values.containsKey(key) || defaults.containsKey(key);
  }

  @Override
  public String get(String key) {
    if (values.containsKey(key)) {
      return values.get(key);
    }
    return defaults.get(key);
  }

  @Override
  public String put(String key, String value) {
    return values.put(Preconditions.checkNotNull(key), Preconditions.checkNotNull(value));
  }

  @Override
  public String remove(String key) {
    // key needs to be removed from both maps
    final String valuesPrev = values.remove(key);
    final String defaultsPrev = defaults.remove(key);
    // but what we "saw" before depends where was the removed value
    if (valuesPrev == null) {
      return defaultsPrev;
    }
    else {
      return valuesPrev;
    }
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> map) {
    values.putAll(map);
  }

  // ==
  protected int getInteger(final String key, final int defaultValue) {
    if (containsKey(key)) {
      return Integer.valueOf(get(key));
    }
    else {
      return defaultValue;
    }
  }

  protected void setInteger(final String key, final int value) {
    put(key, Integer.toString(value));
  }

  protected long getLong(final String key, final long defaultValue) {
    if (containsKey(key)) {
      return Long.valueOf(get(key));
    }
    else {
      return defaultValue;
    }
  }

  protected void setLong(final String key, final long value) {
    put(key, Long.toString(value));
  }

  protected boolean getBoolean(final String key, final boolean defaultValue) {
    if (containsKey(key)) {
      return Boolean.valueOf(get(key));
    }
    else {
      return defaultValue;
    }
  }

  protected void setBoolean(final String key, final boolean value) {
    put(key, Boolean.toString(value));
  }

  protected String getString(final String key, final String defaultValue) {
    if (containsKey(key)) {
      return get(key);
    }
    else {
      return defaultValue;
    }
  }

  protected void setString(final String key, final String value) {
    put(key, Preconditions.checkNotNull(value));
  }

  protected String getKeyForAttribute(final String attributeName) {
    return new StringBuilder(32).append("storageItem-").append(Preconditions.checkNotNull(attributeName)).toString();
  }

  // ==

  protected void overlayMap(Map<String, String> map) {
    defaults.putAll(map);
    defaults.remove(getKeyForAttribute("length")); // since 2.7.0 no length!
  }

  @Override
  public void overlayAttributes(final Attributes repositoryItemAttributes) {
    overlayMap(repositoryItemAttributes.asMap());
  }

  @Override
  public int getGeneration() {
    return getInteger(getKeyForAttribute("generation"), 0);
  }

  @Override
  public void setGeneration(final int value) {
    setInteger(getKeyForAttribute("generation"), value);
  }

  @Override
  public void incrementGeneration() {
    setInteger(getKeyForAttribute("generation"), getGeneration() + 1);
  }

  @Override
  public String getPath() {
    return getString(getKeyForAttribute("path"), null);
  }

  @Override
  public void setPath(final String value) {
    setString(getKeyForAttribute("path"), value);
  }

  @Override
  public boolean isReadable() {
    return getBoolean(getKeyForAttribute("readable"), true);
  }

  @Override
  public void setReadable(final boolean value) {
    setBoolean(getKeyForAttribute("readable"), value);
  }

  @Override
  public boolean isWritable() {
    return getBoolean(getKeyForAttribute("writable"), true);
  }

  @Override
  public void setWritable(final boolean value) {
    setBoolean(getKeyForAttribute("writable"), value);
  }

  @Override
  public String getRepositoryId() {
    return getString(getKeyForAttribute("repositoryId"), null);
  }

  @Override
  public void setRepositoryId(final String value) {
    setString(getKeyForAttribute("repositoryId"), value);
  }

  @Override
  public long getCreated() {
    return getLong(getKeyForAttribute("created"), 0);
  }

  @Override
  public void setCreated(final long value) {
    setLong(getKeyForAttribute("created"), value);
  }

  @Override
  public long getModified() {
    return getLong(getKeyForAttribute("modified"), 0);
  }

  @Override
  public void setModified(final long value) {
    setLong(getKeyForAttribute("modified"), value);
  }

  @Override
  public long getStoredLocally() {
    return getLong(getKeyForAttribute("storedLocally"), 0);
  }

  @Override
  public void setStoredLocally(final long value) {
    setLong(getKeyForAttribute("storedLocally"), value);
  }

  @Override
  public long getCheckedRemotely() {
    return getLong(getKeyForAttribute("checkedRemotely"), 0);
  }

  @Override
  public void setCheckedRemotely(final long value) {
    setLong(getKeyForAttribute("checkedRemotely"), value);
  }

  @Override
  public long getLastRequested() {
    return getLong(getKeyForAttribute("lastRequested"), 0);
  }

  @Override
  public void setLastRequested(final long value) {
    setLong(getKeyForAttribute("lastRequested"), value);
  }

  @Override
  public boolean isExpired() {
    return getBoolean(getKeyForAttribute("expired"), false);
  }

  @Override
  public void setExpired(final boolean value) {
    setBoolean(getKeyForAttribute("expired"), value);
  }

  @Override
  public String getRemoteUrl() {
    return getString(getKeyForAttribute("remoteUrl"), null);
  }

  @Override
  public void setRemoteUrl(final String value) {
    setString(getKeyForAttribute("remoteUrl"), value);
  }

  @Override
  public Map<String, String> asMap() {
    final HashMap<String, String> result = new HashMap<String, String>();
    result.putAll(defaults);
    result.putAll(values);
    return Collections.unmodifiableMap(result);
  }
}
