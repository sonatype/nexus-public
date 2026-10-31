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
package org.sonatype.nexus.audit.internal.store;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

/**
 * Data access for audit events.
 */
public interface AuditEventDAO
    extends DataAccess
{
  List<AuditEventData> findAll(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("initiator") String initiator,
      @Param("repositoryName") String repositoryName,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate,
      @Param("limit") int limit,
      @Param("offset") int offset);

  int count(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("initiator") String initiator,
      @Param("repositoryName") String repositoryName,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate);

  void insert(AuditEventData data);

  /**
   * Find audit events by multiple domains.
   */
  List<AuditEventData> findByDomains(
      @Param("domains") Collection<String> domains,
      @Param("type") String type,
      @Param("initiator") String initiator,
      @Param("repositoryName") String repositoryName,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /**
   * Count audit events by multiple domains.
   */
  int countByDomains(
      @Param("domains") Collection<String> domains,
      @Param("type") String type,
      @Param("initiator") String initiator,
      @Param("repositoryName") String repositoryName,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate);

  /**
   * Delete up to {@code batchSize} rows whose {@code timestamp} is strictly older than {@code cutoff}.
   * Returns the number of rows actually deleted. Callers should invoke in a loop until this returns 0
   * to prune the full backlog in bounded-size transactions.
   */
  int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff, @Param("batchSize") int batchSize);
}
