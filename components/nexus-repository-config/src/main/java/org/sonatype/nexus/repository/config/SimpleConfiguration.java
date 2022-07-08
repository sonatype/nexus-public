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

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple repository configuration object primarily intended for testing. At runtime a database configuration object
 * is typically used.
 */
public class SimpleConfiguration
    implements Configuration
{
  private EntityId repositoryId;

  private String repositoryName;

  private String recipeName;

  private boolean online;

  private EntityId routingRuleId = null;

  private Map<String, Map<String, Object>> attributes;

  public SimpleConfiguration() {}

  public SimpleConfiguration(
      final EntityId repositoryId,
      final String repositoryName,
      final String recipeName,
      final boolean online,
      final EntityId routingRuleId,
      final Map<String, Map<String, Object>> attributes)
  {
    this.repositoryId = repositoryId;
    this.repositoryName = repositoryName;
    this.recipeName = recipeName;
    this.online = online;
    this.routingRuleId = routingRuleId;
    this.attributes = attributes;
  }

  @Override
  public EntityId getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final EntityId repositoryId) {
    this.repositoryId = repositoryId;
  }

  @Override
  public String getRepositoryName() {
    return repositoryName;
  }

  @Override
  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public String getRecipeName() {
    return recipeName;
  }

  @Override
  public void setRecipeName(final String recipeName) {
    this.recipeName = recipeName;
  }

  @Override
  public boolean isOnline() {
    return online;
  }

  @Override
  public void setOnline(final boolean online) {
    this.online = online;
  }

  @Override
  public EntityId getRoutingRuleId() {
    return routingRuleId;
  }

  @Override
  public void setRoutingRuleId(final EntityId routingRuleId) {
    this.routingRuleId = routingRuleId;
  }

  @Nullable
  @Override
  public Map<String, Map<String, Object>> getAttributes() {
    return attributes;
  }

  @Override
  public void setAttributes(@Nullable final Map<String, Map<String, Object>> attributes) {
    this.attributes = attributes;
  }

  @Override
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
  public Configuration copy() {
    return this;
  }
}
