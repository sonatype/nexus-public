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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight component suggestion for autocomplete/typeahead.
 * Contains only essential fields to minimize response size.
 */
public class SuggestXO
{
  private final String id;

  private final String name;

  private final String group;

  private final String version;

  private final String format;

  private final String repository;

  @JsonCreator
  public SuggestXO(
      @JsonProperty("id") final String id,
      @JsonProperty("name") final String name,
      @JsonProperty("group") final String group,
      @JsonProperty("version") final String version,
      @JsonProperty("format") final String format,
      @JsonProperty("repository") final String repository)
  {
    this.id = id;
    this.name = name;
    this.group = group;
    this.version = version;
    this.format = format;
    this.repository = repository;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
  }

  public String getVersion() {
    return version;
  }

  public String getFormat() {
    return format;
  }

  public String getRepository() {
    return repository;
  }
}
