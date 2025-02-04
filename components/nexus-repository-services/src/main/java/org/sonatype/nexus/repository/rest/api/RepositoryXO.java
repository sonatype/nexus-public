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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.ProxyType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModelProperty;

/**
 * Repository transfer object for REST APIs.
 */
public class RepositoryXO
{
  private String name;

  private String format;

  private String type;

  private String url;

  @ApiModelProperty(hidden = true)
  @JsonInclude(Include.NON_NULL)
  private Long size;

  private Map<String, Object> attributes;

  public static RepositoryXO fromRepository(final Repository repository) {
    return fromRepository(repository, null);
  }

  public static RepositoryXO fromRepository(final Repository repository, final Long size) {
    return builder().name(repository.getName())
        .format(repository.getFormat().getValue())
        .type(repository.getType().getValue())
        .url(repository.getUrl())
        .attributes(attributes(repository))
        .size(size)
        .build();
  }

  private static Map<String, Object> attributes(Repository repository) {
    if (repository.getType() instanceof ProxyType) {
      Map<String, Object> proxyAttributes = new HashMap<>();
      proxyAttributes.put("remoteUrl",
          repository.getConfiguration().attributes("proxy").get("remoteUrl", String.class));

      Map<String, Object> attributes = new HashMap<>();
      attributes.put("proxy", proxyAttributes);
      return attributes;
    }
    return Collections.emptyMap();
  }

  public static RepositoryXOBuilder builder() {
    return new RepositoryXOBuilder();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RepositoryXO that = (RepositoryXO) o;
    return Objects.equals(name, that.name) && Objects.equals(format, that.format) &&
        Objects.equals(type, that.type) && Objects.equals(url, that.url) &&
        Objects.equals(size, that.size) && Objects.equals(attributes, that.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, format, type, url, size, attributes);
  }

  public static class RepositoryXOBuilder
  {
    private String name;

    private String format;

    private String type;

    private String url;

    private Long size;

    private Map<String, Object> attributes;

    public RepositoryXOBuilder name(String name) {
      this.name = name;
      return this;
    }

    public RepositoryXOBuilder format(String format) {
      this.format = format;
      return this;
    }

    public RepositoryXOBuilder type(String type) {
      this.type = type;
      return this;
    }

    public RepositoryXOBuilder url(String url) {
      this.url = url;
      return this;
    }

    public RepositoryXOBuilder size(Long size) {
      this.size = size;
      return this;
    }

    public RepositoryXOBuilder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public RepositoryXO build() {
      RepositoryXO repositoryXO = new RepositoryXO();
      repositoryXO.setName(name);
      repositoryXO.setFormat(format);
      repositoryXO.setType(type);
      repositoryXO.setUrl(url);
      repositoryXO.setSize(size);
      repositoryXO.setAttributes(attributes);
      return repositoryXO;
    }
  }
}
