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

import java.util.Map;
import java.util.Objects;
import jakarta.validation.constraints.NotBlank;

/**
 * Component exchange object.
 *
 * @since 3.0
 */
public class ComponentXO
{
  @NotBlank
  private String id;

  @NotBlank
  private String repositoryName;

  @NotBlank
  private String group;

  @NotBlank
  private String name;

  @NotBlank
  private String version;

  @NotBlank
  private String format;

  @NotBlank
  private String lastBlobUpdated;

  private String groupingKey;

  private Map<String, Object> attributes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
    setGroupingKey();
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
    setGroupingKey();
  }

  public String getLastBlobUpdated() {
    return lastBlobUpdated;
  }

  public void setLastBlobUpdated(String lastBlobUpdated) {
    this.lastBlobUpdated = lastBlobUpdated;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  private void setGroupingKey() {
    if (this.format != null && this.id != null) {
      this.groupingKey = this.format + "-" + this.id;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentXO that = (ComponentXO) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ComponentXO{" +
        "id='" + id + '\'' +
        ", repositoryName='" + repositoryName + '\'' +
        ", group='" + group + '\'' +
        ", name='" + name + '\'' +
        ", version='" + version + '\'' +
        ", format='" + format + '\'' +
        ", lastBlobUpdated='" + lastBlobUpdated + '\'' +
        '}';
  }
}
