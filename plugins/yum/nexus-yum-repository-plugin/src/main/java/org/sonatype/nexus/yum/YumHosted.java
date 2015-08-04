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
package org.sonatype.nexus.yum;

import java.util.Map;

import org.sonatype.scheduling.ScheduledTask;

/**
 * Provides access to Yum functionality around a Nexus hosted repository.
 *
 * @since 2.7
 */
public interface YumHosted
    extends Yum
{

  static final long DEFAULT_DELETE_PROCESSING_DELAY = 10;

  /**
   * Configures if deletes from a Nexus repository should result in Yum metadata regeneration.
   *
   * @param processDeletes true if metadata should be regenerated
   * @return itself
   */
  YumHosted setProcessDeletes(boolean processDeletes);

  /**
   * Configures the delay between a delete and Yum metadata regeneration.
   *
   * @param numberOfSeconds delay in seconds
   * @return itself
   */
  YumHosted setDeleteProcessingDelay(final long numberOfSeconds);

  /**
   * Configures repository path of a file containing Yum groups definition.
   *
   * @param yumGroupsDefinitionFile repository path of a file containing Yum groups definition
   * @return itself
   */
  YumHosted setYumGroupsDefinitionFile(final String yumGroupsDefinitionFile);

  /**
   * @return true if metadata is regenerated after a delete from a Nexus repository
   */
  boolean shouldProcessDeletes();

  /**
   * @return number of seconds between a delete from a Nexus repository and metadata regeneration
   */
  long deleteProcessingDelay();

  /**
   * @return repository path of a file containing Yum groups definition
   */
  String getYumGroupsDefinitionFile();

  /**
   * Configures an alias for a version.
   *
   * @param alias   alias name (cannot be null)
   * @param version to be aliased (cannot be null)
   * @return itself
   */
  YumHosted addAlias(String alias, String version);

  /**
   * Removes an alias.
   *
   * @param alias alias name (cannot be null)
   * @return itself
   */
  YumHosted removeAlias(String alias);

  /**
   * Resets aliases to provided mappings.
   *
   * @param aliases alias mappings (cannot be null)
   * @return itself
   */
  YumHosted setAliases(Map<String, String> aliases);

  /**
   * @param alias alias name
   * @return version mapped to provided alias, null if no mapping found
   */
  String getVersion(String alias);

  ScheduledTask<YumRepository> regenerate();

  /**
   * Adds RPM and regenerate.
   *
   * @param path to be added
   * @return regenerate future (never null)
   */
  ScheduledTask<YumRepository> addRpmAndRegenerate(String path);

  YumRepository getYumRepository(String version)
      throws Exception;

  void regenerateWhenPathIsRemoved(String path);

  void regenerateWhenDirectoryIsRemoved(String path);

  void markDirty(String version);

}
