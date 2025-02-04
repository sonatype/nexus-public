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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

/**
 * Component transfer object for REST APIs.
 */
public class DefaultComponentXO
    implements ComponentXO
{
  private String id;

  private String group;

  private String name;

  private String version;

  private String repository;

  private String format;

  private List<AssetXO> assets;

  public DefaultComponentXO() {
  }

  public DefaultComponentXO(
      String id,
      String group,
      String name,
      String version,
      String repository,
      String format,
      List<AssetXO> assets)
  {
    this.id = id;
    this.group = group;
    this.name = name;
    this.version = version;
    this.repository = repository;
    this.format = format;
    this.assets = assets;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getGroup() {
    return group;
  }

  @Override
  public void setGroup(String group) {
    this.group = group;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String getRepository() {
    return repository;
  }

  @Override
  public void setRepository(String repository) {
    this.repository = repository;
  }

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public List<AssetXO> getAssets() {
    return assets;
  }

  @Override
  public void setAssets(List<AssetXO> assets) {
    this.assets = assets;
  }

  @Override
  @JsonAnyGetter
  public Map<String, Object> getExtraJsonAttributes() {
    return Collections.emptyMap();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultComponentXO that = (DefaultComponentXO) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "DefaultComponentXO{" +
        "id='" + id + '\'' +
        ", group='" + group + '\'' +
        ", name='" + name + '\'' +
        ", version='" + version + '\'' +
        ", repository='" + repository + '\'' +
        ", format='" + format + '\'' +
        ", assets=" + assets +
        '}';
  }

  public static DefaultComponentXOBuilder builder() {
    return new DefaultComponentXOBuilder();
  }

  public static class DefaultComponentXOBuilder
  {
    private String id;

    private String group;

    private String name;

    private String version;

    private String repository;

    private String format;

    private List<AssetXO> assets;

    public DefaultComponentXOBuilder id(String id) {
      this.id = id;
      return this;
    }

    public DefaultComponentXOBuilder group(String group) {
      this.group = group;
      return this;
    }

    public DefaultComponentXOBuilder name(String name) {
      this.name = name;
      return this;
    }

    public DefaultComponentXOBuilder version(String version) {
      this.version = version;
      return this;
    }

    public DefaultComponentXOBuilder repository(String repository) {
      this.repository = repository;
      return this;
    }

    public DefaultComponentXOBuilder format(String format) {
      this.format = format;
      return this;
    }

    public DefaultComponentXOBuilder assets(List<AssetXO> assets) {
      this.assets = assets;
      return this;
    }

    public DefaultComponentXO build() {
      return new DefaultComponentXO(id, group, name, version, repository, format, assets);
    }
  }
}
