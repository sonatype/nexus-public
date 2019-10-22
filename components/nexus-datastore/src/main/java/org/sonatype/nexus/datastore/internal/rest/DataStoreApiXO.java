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
package org.sonatype.nexus.datastore.internal.rest;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * REST API Model for viewing Data Stores.
 *
 * @since 3.next
 */
public class DataStoreApiXO
{
  @ApiModelProperty(value = "The source of the data store.", allowableValues = "local")
  @NotEmpty
  private String source;

  @ApiModelProperty(value = "The type of the data store.", allowableValues = "jdbc")
  @NotEmpty
  private String type;

  @ApiModelProperty(value = "The name of the data store.")
  @NotEmpty
  private String name;

  @ApiModelProperty(value = "The JDBC connection URL for the data store.", required = true)
  @NotEmpty
  private String jdbcUrl;

  @ApiModelProperty(value = "The username to use for the JDBC connection.", example = "")
  private String username;

  private String schema;

  private String advanced;

  public DataStoreApiXO() {
    // do nothing
  }

  public DataStoreApiXO(final DataStoreConfiguration configuration) {
    this.name = configuration.getName();
    this.type = configuration.getType();
    this.source = configuration.getSource();
    this.jdbcUrl = configuration.getAttributes().get("jdbcUrl");
    this.username = configuration.getAttributes().get("username");
    this.schema = configuration.getAttributes().get("schema");
    this.advanced = configuration.getAttributes().get("advanced");
  }

  public String getAdvanced() {
    return advanced;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getName() {
    return name;
  }

  public String getSchema() {
    return schema;
  }

  public String getSource() {
    return source;
  }

  public String getType() {
    return type;
  }

  public String getUsername() {
    return username;
  }

  public void setAdvanced(final String advanced) {
    this.advanced = advanced;
  }

  public void setJdbcUrl(final String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setSchema(final String schema) {
    this.schema = schema;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public DataStoreConfiguration toDataStoreConfiguration() {
    DataStoreConfiguration configuration = new DataStoreConfiguration();

    configuration.setName(name);
    configuration.setSource(source);
    configuration.setType(type);

    Map<String, String> attributes = new HashMap<>();
    attributes.put("jdbcUrl", jdbcUrl);
    if (!Strings2.isBlank(username)) {
      attributes.put("username", username);
    }
    if (!Strings2.isBlank(schema)) {
      attributes.put("schema", schema);
    }
    if (!Strings2.isBlank(advanced)) {
      attributes.put("advanced", advanced);
    }
    configuration.setAttributes(attributes);

    return configuration;
  }
}
