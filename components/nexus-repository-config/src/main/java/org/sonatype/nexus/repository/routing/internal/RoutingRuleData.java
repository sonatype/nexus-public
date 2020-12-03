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
package org.sonatype.nexus.repository.routing.internal;

import java.util.List;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.common.entity.HasName;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link RoutingRule} data.
 *
 * @since 3.21
 */
public class RoutingRuleData
    implements HasEntityId, HasName, RoutingRule
{
  private EntityId id;

  private String description;

  private RoutingMode mode;

  private String name;

  private List<String> matchers;

  @Override
  public EntityId getId() {
    return id;
  }

  @Override
  public void setId(final EntityId id) {
    this.id = id;
  }

  @Override
  public EntityId id() {
    return getId();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  @JsonProperty("description")
  public String description() {
    return description;
  }

  @Override
  public RoutingRuleData description(final String description) {
    this.description = description;
    return this;
  }

  /**
   * The block mode of this RoutingRule, when ACCESS then any request which does not match one of the matchers should be
   * blocked and when BLOCK any request which matches one of the matchers should be blocked.
   */
  @Override
  @JsonProperty("mode")
  public RoutingMode mode() {
    return mode;
  }

  @Override
  public RoutingRuleData mode(final RoutingMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * The name of this RoutingRule, must be unique
   */
  @Override
  public String name() {
    return name;
  }

  @Override
  public RoutingRuleData name(final String name) {
    this.name = name;
    return this;
  }

  /**
   * @return the list of regex matchers which this RoutingRule uses
   */
  @Override
  @JsonProperty("matchers")
  public List<String> matchers() {
    return matchers;
  }

  @Override
  public RoutingRuleData matchers(final List<String> matchers) {
    this.matchers = matchers;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (id != null) {
      return id.equals(((RoutingRuleData) o).id);
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    if (id != null) {
      return id.hashCode();
    }
    return super.hashCode();
  }
}
