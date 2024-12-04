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

import java.util.Objects;

import org.sonatype.nexus.repository.upload.UploadFieldDefinition;

/**
 * Upload field definition transfer object for rest api
 */
public class UploadFieldDefinitionXO
{
  private String name;

  private String type;

  private String description;

  private boolean optional;

  private String group;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public static UploadFieldDefinitionXO from(final UploadFieldDefinition uploadFieldDefinition) {
    return builder()
        .name(uploadFieldDefinition.getName())
        .type(uploadFieldDefinition.getType().name())
        .description(uploadFieldDefinition.getHelpText())
        .optional(uploadFieldDefinition.isOptional())
        .group(uploadFieldDefinition.getGroup())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadFieldDefinitionXO that = (UploadFieldDefinitionXO) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "UploadFieldDefinitionXO{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", description='" + description + '\'' +
        ", optional=" + optional +
        ", group='" + group + '\'' +
        '}';
  }

  public static UploadFieldDefinitionXOBuilder builder() {
    return new UploadFieldDefinitionXOBuilder();
  }

  public static class UploadFieldDefinitionXOBuilder
  {
    private String name;

    private String type;

    private String description;

    private boolean optional;

    private String group;

    public UploadFieldDefinitionXOBuilder name(String name) {
      this.name = name;
      return this;
    }

    public UploadFieldDefinitionXOBuilder type(String type) {
      this.type = type;
      return this;
    }

    public UploadFieldDefinitionXOBuilder description(String description) {
      this.description = description;
      return this;
    }

    public UploadFieldDefinitionXOBuilder optional(boolean optional) {
      this.optional = optional;
      return this;
    }

    public UploadFieldDefinitionXOBuilder group(String group) {
      this.group = group;
      return this;
    }

    public UploadFieldDefinitionXO build() {
      UploadFieldDefinitionXO uploadFieldDefinitionXO = new UploadFieldDefinitionXO();
      uploadFieldDefinitionXO.setName(name);
      uploadFieldDefinitionXO.setType(type);
      uploadFieldDefinitionXO.setDescription(description);
      uploadFieldDefinitionXO.setOptional(optional);
      uploadFieldDefinitionXO.setGroup(group);
      return uploadFieldDefinitionXO;
    }
  }
}
