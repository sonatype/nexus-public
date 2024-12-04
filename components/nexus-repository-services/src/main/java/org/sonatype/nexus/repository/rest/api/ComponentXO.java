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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @since 3.8
 */
@JsonPropertyOrder({"id", "repository", "format", "group", "name", "version", "assets"})
public interface ComponentXO
{
  String getId();

  void setId(String id);

  String getGroup();

  void setGroup(String group);

  String getName();

  void setName(String name);

  String getVersion();

  void setVersion(String version);

  String getRepository();

  void setRepository(String repository);

  String getFormat();

  void setFormat(String format);

  List<AssetXO> getAssets();

  void setAssets(List<AssetXO> assets);

  /**
   * Attributes to add the JSON payload
   *
   * @return
   */
  @JsonAnyGetter
  Map<String, Object> getExtraJsonAttributes();
}
