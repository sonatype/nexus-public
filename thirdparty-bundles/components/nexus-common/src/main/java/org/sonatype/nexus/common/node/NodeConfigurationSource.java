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
package org.sonatype.nexus.common.node;

import java.util.List;
import java.util.Optional;

/**
 * persistence mechanism for {@link NodeConfiguration}
 *
 * @since 3.6.1
 */
public interface NodeConfigurationSource
{
  /**
   * Retrieves all node configuration records
   *
   * @return {@link List} of {@link NodeConfiguration} entities
   */
  List<NodeConfiguration> loadAll();

  /**
   * Retrieves node configuration record by node identity, if the record exists
   *
   * @param nodeId node identifier
   * @return {@link Optional} of {@link NodeConfiguration}
   */
  Optional<NodeConfiguration> getById(String nodeId);

  /**
   * Creates a node configuration record, given a {@link NodeConfiguration}
   *
   * @param configuration {@link NodeConfiguration} entity
   * @return {@link String} node identity
   */
  String create(NodeConfiguration configuration);

  /**
   * Updates the node configuration record of the entity with a matching nodeId
   *
   * @param configuration {@link NodeConfiguration} entity
   * @return whether update succeeded
   */
  boolean update(NodeConfiguration configuration);

  /**
   * Deletes the node configuration record of the entity with the matching nodeId
   *
   * @param nodeId node identifier
   * @return whether delete succeeded
   */
  boolean delete(String nodeId);

  /**
   * Updates the node configuration record of the entity with a matching nodeId to have the given friendly name
   *
   * @param nodeId node identifier
   * @param friendlyName specified by admin
   */
  void setFriendlyName(String nodeId, String friendlyName);

  /**
   * returns the current node's friendly name
   *
   * @since 3.6.1
   *
   * @return friendlyName specified by admin
   */
  String sayMyName();
}
