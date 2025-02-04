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
import javax.validation.constraints.Pattern;

import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import io.swagger.annotations.ApiModelProperty;

public class RoutingRuleXO
{
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  private String name;

  @ApiModelProperty(allowEmptyValue = true)
  private String description;

  @ApiModelProperty(
      value = "Determines what should be done with requests when their path matches any of the matchers",
      allowableValues = "BLOCK,ALLOW")
  private RoutingMode mode;

  @ApiModelProperty(
      value = "Regular expressions used to identify request paths that are allowed or blocked (depending on mode)")
  private List<String> matchers;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RoutingMode getMode() {
    return mode;
  }

  public void setMode(RoutingMode mode) {
    this.mode = mode;
  }

  public List<String> getMatchers() {
    return matchers;
  }

  public void setMatchers(List<String> matchers) {
    this.matchers = matchers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoutingRuleXO that = (RoutingRuleXO) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "RoutingRuleXO{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", mode=" + mode +
        ", matchers=" + matchers +
        '}';
  }

  public static RoutingRuleXO fromRoutingRule(RoutingRule routingRule) {
    return new RoutingRuleXOBuilder()
        .name(routingRule.name())
        .description(routingRule.description())
        .mode(routingRule.mode())
        .matchers(routingRule.matchers())
        .build();
  }

  public static RoutingRuleXOBuilder builder() {
    return new RoutingRuleXOBuilder();
  }

  public static class RoutingRuleXOBuilder
  {
    private String name;

    private String description;

    private RoutingMode mode;

    private List<String> matchers;

    public RoutingRuleXOBuilder name(String name) {
      this.name = name;
      return this;
    }

    public RoutingRuleXOBuilder description(String description) {
      this.description = description;
      return this;
    }

    public RoutingRuleXOBuilder mode(RoutingMode mode) {
      this.mode = mode;
      return this;
    }

    public RoutingRuleXOBuilder matchers(List<String> matchers) {
      this.matchers = matchers;
      return this;
    }

    public RoutingRuleXO build() {
      RoutingRuleXO routingRuleXO = new RoutingRuleXO();
      routingRuleXO.setName(name);
      routingRuleXO.setDescription(description);
      routingRuleXO.setMode(mode);
      routingRuleXO.setMatchers(matchers);
      return routingRuleXO;
    }
  }
}
