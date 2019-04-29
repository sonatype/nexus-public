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
package org.sonatype.nexus.repository.routing;

import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.common.entity.AbstractEntity;

import static org.sonatype.nexus.common.entity.EntityHelper.hasMetadata;
import static org.sonatype.nexus.common.entity.EntityHelper.id;

/**
 * A RoutingRule which can be applied to a repository to block or allow requests depending on the RoutingMode
 *
 * @since 3.16
 */
public class RoutingRule extends AbstractEntity
{
  private String description;

  private RoutingMode mode;

  private String name;

  private List<String> matchers;

  public RoutingRule() {
    // for deserialization
  }

  public RoutingRule(final String name, final String description, final RoutingMode mode, final List<String> matchers) {
    this.description = description;
    this.name = name;
    this.mode = mode;
    this.matchers = matchers;
  }

  public String description() {
    return description;
  }

  public RoutingRule description(final String description) {
    this.description = description;
    return this;
  }

  /**
   * The block mode of this RoutingRule, when ACCESS then any request which does not match one of the matchers should be
   * blocked and when BLOCK any request which matches one of the matchers should be blocked.
   */
  public RoutingMode mode() {
    return mode;
  }

  public RoutingRule mode(final RoutingMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * The name of this RoutingRule, must be unique
   */
  public String name() {
    return name;
  }

  public RoutingRule name(final String name) {
    this.name = name;
    return this;
  }

  /**
   * @return the list of regex matchers which this RoutingRule uses
   */
  public List<String> matchers() {
    return matchers;
  }

  public RoutingRule matchers(final List<String> matchers) {
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

    AbstractEntity that = (AbstractEntity) o;
    if (hasMetadata(this) && hasMetadata(that)) {
      return Objects.equals(id(this), id(that));
    }

    return super.equals(o);
  }

  @Override
  public int hashCode() {
    if (hasMetadata(this)) {
      return Objects.hash(id(this));
    }
    return super.hashCode();
  }
}
