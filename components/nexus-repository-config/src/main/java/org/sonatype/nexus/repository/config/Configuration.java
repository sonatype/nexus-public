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
package org.sonatype.nexus.repository.config;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Repository configuration.
 *
 * @since 3.0
 */
public interface Configuration
    extends Cloneable
{
  List<String> SENSITIVE_FIELD_NAMES = newArrayList("applicationPassword", "password",
      "systemPassword", "secret", "yumSigning", "aptSigning");

  static void addSensitiveFieldName(String sensitiveFieldName) {
    SENSITIVE_FIELD_NAMES.add(sensitiveFieldName);
  }

  /**
   * @since 3.24
   */
  EntityId getRepositoryId();

  String getRepositoryName();

  void setRepositoryName(String repositoryName);

  String getRecipeName();

  void setRecipeName(String recipeName);

  /**
   * @return true, if repository should serve inbound requests
   */
  boolean isOnline();

  /**
   * @param online true, if repository should serve inbound requests
   */
  void setOnline(boolean online);

  EntityId getRoutingRuleId();

  void setRoutingRuleId(EntityId routingRuleId);

  @Nullable
  Map<String, Map<String, Object>> getAttributes();

  void setAttributes(@Nullable Map<String, Map<String, Object>> attributes);

  NestedAttributesMap attributes(String key);

  /**
   * Returns a deeply cloned copy. Note that Entity.entityMetadata is not deep-copied.
   */
  Configuration copy();
}
