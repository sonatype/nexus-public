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
package org.sonatype.nexus.repository.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.DetachingList;
import org.sonatype.nexus.common.collect.DetachingMap;
import org.sonatype.nexus.common.collect.DetachingSet;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Entity;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Repository configuration.
 *
 * @since 3.0
 */
public class Configuration
    extends Entity
    implements Cloneable
{
  private String repositoryName;

  private String recipeName;

  private boolean online;

  private Map<String, Map<String, Object>> attributes;

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = checkNotNull(repositoryName);
  }

  public String getRecipeName() {
    return recipeName;
  }

  public void setRecipeName(final String recipeName) {
    this.recipeName = checkNotNull(recipeName);
  }

  /**
   * @return true, if repository should serve inbound requests
   */
  public boolean isOnline() {
    return online;
  }

  /**
   * @param online true, if repository should serve inbound requests
   */
  public void setOnline(final boolean online) {
    this.online = online;
  }

  @Nullable
  public Map<String, Map<String, Object>> getAttributes() {
    return attributes;
  }

  public void setAttributes(@Nullable final Map<String, Map<String, Object>> attributes) {
    this.attributes = attributes;
  }

  public NestedAttributesMap attributes(final String key) {
    checkNotNull(key);

    if (attributes == null) {
      attributes = Maps.newHashMap();
    }

    Map<String, Object> map = attributes.get(key);
    if (map == null) {
      map = Maps.newHashMap();
      attributes.put(key, map);
    }

    return new NestedAttributesMap(key, map);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryName='" + repositoryName + '\'' +
        ", recipeName='" + recipeName + '\'' +
        ", attributes=" + attributes +
        '}';
  }

  /**
   * Returns a deeply cloned copy. Note that Entity.entityMetadata is not deep-copied.
   */
  public Configuration copy() {
    try {
      Configuration c = (Configuration) clone();
      c.attributes = copy(attributes);
      return c;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a lazy copy; for collections the copy is only taken as the content is touched.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <V> V copy(final V value) {
    Object copy = value;
    if (value instanceof Map) {
      copy = new DetachingMap((Map) value, this::copy);
    }
    else if (value instanceof List) {
      copy = new DetachingList((List) value, this::copy);
    }
    else if (value instanceof Set) {
      copy = new DetachingSet((Set) value, this::copy);
    }
    return (V) copy;
  }

}
