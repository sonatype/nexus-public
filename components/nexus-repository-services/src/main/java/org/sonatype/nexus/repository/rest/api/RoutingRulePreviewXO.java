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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotBlank;

/**
 * Routing Rule Preview grid row transfer object.
 */
public class RoutingRulePreviewXO
{
  @NotBlank
  private String repository;

  private String type;

  private String format;

  private String rule;

  private Boolean allowed;

  private Boolean expanded;

  private Boolean expandable;

  private List<RoutingRulePreviewXO> children;

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }

  public Boolean getAllowed() {
    return allowed;
  }

  public void setAllowed(Boolean allowed) {
    this.allowed = allowed;
  }

  public Boolean getExpanded() {
    return expanded;
  }

  public void setExpanded(Boolean expanded) {
    this.expanded = expanded;
  }

  public Boolean getExpandable() {
    return expandable;
  }

  public void setExpandable(Boolean expandable) {
    this.expandable = expandable;
  }

  public List<RoutingRulePreviewXO> getChildren() {
    return children;
  }

  public void setChildren(List<RoutingRulePreviewXO> children) {
    this.children = children;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoutingRulePreviewXO that = (RoutingRulePreviewXO) o;
    return Objects.equals(repository, that.repository) && Objects.equals(type, that.type) &&
        Objects.equals(format, that.format) && Objects.equals(rule, that.rule) &&
        Objects.equals(allowed, that.allowed) && Objects.equals(expanded, that.expanded) &&
        Objects.equals(expandable, that.expandable) && Objects.equals(children, that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repository, type, format, rule, allowed, expanded, expandable, children);
  }

  public static RoutingRulePreviewXOBuilder builder() {
    return new RoutingRulePreviewXOBuilder();
  }

  public static class RoutingRulePreviewXOBuilder
  {
    private String repository;

    private String type;

    private String format;

    private String rule;

    private Boolean allowed;

    private Boolean expanded;

    private Boolean expandable;

    private List<RoutingRulePreviewXO> children;

    public RoutingRulePreviewXOBuilder repository(String repository) {
      this.repository = repository;
      return this;
    }

    public RoutingRulePreviewXOBuilder type(String type) {
      this.type = type;
      return this;
    }

    public RoutingRulePreviewXOBuilder format(String format) {
      this.format = format;
      return this;
    }

    public RoutingRulePreviewXOBuilder rule(String rule) {
      this.rule = rule;
      return this;
    }

    public RoutingRulePreviewXOBuilder allowed(Boolean allowed) {
      this.allowed = allowed;
      return this;
    }

    public RoutingRulePreviewXOBuilder expanded(Boolean expanded) {
      this.expanded = expanded;
      return this;
    }

    public RoutingRulePreviewXOBuilder expandable(Boolean expandable) {
      this.expandable = expandable;
      return this;
    }

    public RoutingRulePreviewXOBuilder children(List<RoutingRulePreviewXO> children) {
      this.children = children;
      return this;
    }

    public RoutingRulePreviewXO build() {
      RoutingRulePreviewXO routingRulePreviewXO = new RoutingRulePreviewXO();
      routingRulePreviewXO.setRepository(repository);
      routingRulePreviewXO.setType(type);
      routingRulePreviewXO.setFormat(format);
      routingRulePreviewXO.setRule(rule);
      routingRulePreviewXO.setAllowed(allowed);
      routingRulePreviewXO.setExpanded(expanded);
      routingRulePreviewXO.setExpandable(expandable);
      routingRulePreviewXO.setChildren(children);
      return routingRulePreviewXO;
    }
  }
}
