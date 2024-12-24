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
package org.sonatype.nexus.coreui;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;
import org.sonatype.nexus.validation.group.Create;

/**
 * Routing Rule transfer object for internal REST API.
 *
 * @since 3.16
 */
public class RoutingRuleXO
{
  private String id;

  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @NotBlank(groups = Create.class)
  private String name;

  private String description;

  @NotBlank(groups = Create.class)
  private RoutingMode mode;

  @NotBlank
  private List<String> matchers;

  private int assignedRepositoryCount;

  private List<String> assignedRepositoryNames;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public int getAssignedRepositoryCount() {
    return assignedRepositoryCount;
  }

  public void setAssignedRepositoryCount(int assignedRepositoryCount) {
    this.assignedRepositoryCount = assignedRepositoryCount;
  }

  public List<String> getAssignedRepositoryNames() {
    return assignedRepositoryNames;
  }

  public void setAssignedRepositoryNames(List<String> assignedRepositoryNames) {
    this.assignedRepositoryNames = assignedRepositoryNames;
  }

  @Override
  public boolean equals(final Object o) {
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
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", mode=" + mode +
        ", matchers=" + matchers +
        ", assignedRepositoryCount=" + assignedRepositoryCount +
        ", assignedRepositoryNames=" + assignedRepositoryNames +
        '}';
  }
}
