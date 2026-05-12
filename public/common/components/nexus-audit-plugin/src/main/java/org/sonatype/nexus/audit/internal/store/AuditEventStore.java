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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.stereotype.Component;

/**
 * Store for persisted audit events, backed by MyBatis.
 */
@Component
@Singleton
public class AuditEventStore
    extends ConfigStoreSupport<AuditEventDAO>
{
  @Inject
  public AuditEventStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  public void insert(final AuditEventData data) {
    dao().insert(data);
  }

  @Transactional
  public List<AuditEventData> findAll(
      final String domain,
      final String type,
      final String initiator,
      final String repositoryName,
      final OffsetDateTime startDate,
      final OffsetDateTime endDate,
      final int limit,
      final int offset)
  {
    return dao().findAll(domain, type, initiator, repositoryName, startDate, endDate, limit, offset);
  }

  @Transactional
  public int count(
      final String domain,
      final String type,
      final String initiator,
      final String repositoryName,
      final OffsetDateTime startDate,
      final OffsetDateTime endDate)
  {
    return dao().count(domain, type, initiator, repositoryName, startDate, endDate);
  }

  @Transactional
  public List<AuditEventData> findByDomains(
      final Collection<String> domains,
      final String type,
      final String initiator,
      final String repositoryName,
      final OffsetDateTime startDate,
      final OffsetDateTime endDate,
      final int limit,
      final int offset)
  {
    return dao().findByDomains(domains, type, initiator, repositoryName, startDate, endDate, limit, offset);
  }

  @Transactional
  public int countByDomains(
      final Collection<String> domains,
      final String type,
      final String initiator,
      final String repositoryName,
      final OffsetDateTime startDate,
      final OffsetDateTime endDate)
  {
    return dao().countByDomains(domains, type, initiator, repositoryName, startDate, endDate);
  }
}
