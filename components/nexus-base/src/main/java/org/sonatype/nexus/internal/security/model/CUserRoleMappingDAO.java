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
package org.sonatype.nexus.internal.security.model;

import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

import static org.sonatype.nexus.common.text.Strings2.lower;
import static org.sonatype.nexus.security.config.SecuritySourceUtil.isCaseInsensitiveSource;

/**
 * {@link CUserRoleMappingData} access.
 *
 * @since 3.21
 */
public interface CUserRoleMappingDAO
    extends DataAccess
{
  Iterable<CUserRoleMappingData> browse();

  void create(CUserRoleMappingData mapping);

  Optional<CUserRoleMappingData> read(
      @Param("userId") String userId, // usual case-sensitive userId, non-null when doing usual search
      @Param("userLo") String userLo, // lowercase userId, non-null when doing case-insensitive search
      @Param("source") String source);

  default Optional<CUserRoleMappingData> read(String userId, String source) {
    return isCaseInsensitiveSource(source) ? read(null, lower(userId), source) : read(userId, null, source);
  }

  boolean update(CUserRoleMappingData mapping);

  boolean delete(
      @Param("userId") String userId, // usual case-sensitive userId, non-null when doing usual search
      @Param("userLo") String userLo, // lowercase userId, non-null when doing case-insensitive search
      @Param("source") String source);

  default boolean delete(String userId, String source) {
    return isCaseInsensitiveSource(source) ? delete(null, lower(userId), source) : delete(userId, null, source);
  }
}
