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
package org.sonatype.nexus.quartz.orient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;

import static org.quartz.TriggerKey.triggerKey;

/**
 * OrientDB specific JDBC job store delegate.
 *
 * Currently overrides {@link #selectJobForTrigger(Connection, ClassLoadHelper, TriggerKey, boolean)} since
 * the standard implementation uses a table join which is not supported by the OrientDB SQL dialect.
 *
 * @since 3.19
 */
public class OrientDelegate
    extends StdJDBCDelegate
{
  @Override
  public JobDetail selectJobForTrigger(final Connection conn,
                                       final ClassLoadHelper loadHelper,
                                       final TriggerKey triggerKey,
                                       boolean loadJobClass)
      throws ClassNotFoundException, SQLException
  {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String jobName;
    String jobGroup;

    try {
      ps = conn.prepareStatement(rtp(SELECT_TRIGGER));
      ps.setString(1, triggerKey.getName());
      ps.setString(2, triggerKey.getGroup());
      rs = ps.executeQuery();

      if (rs.next()) {
        jobName = rs.getString(COL_JOB_NAME);
        jobGroup = rs.getString(COL_JOB_GROUP);
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("No job for trigger '{}'.", triggerKey);
        }
        return null;
      }
    }
    finally {
      closeResultSet(rs);
      closeStatement(ps);
    }

    try {
      ps = conn.prepareStatement(rtp(SELECT_JOB_DETAIL));
      ps.setString(1, jobName);
      ps.setString(2, jobGroup);
      rs = ps.executeQuery();

      if (rs.next()) {
        JobDetailImpl job = new JobDetailImpl();
        job.setName(jobName);
        job.setGroup(jobGroup);
        job.setDurability(getBoolean(rs, COL_IS_DURABLE));
        if (loadJobClass) {
          job.setJobClass(loadHelper.loadClass(rs.getString(COL_JOB_CLASS), Job.class));
        }
        job.setRequestsRecovery(getBoolean(rs, COL_REQUESTS_RECOVERY));

        return job;
      }
      else {
        return null;
      }
    } finally {
      closeResultSet(rs);
      closeStatement(ps);
    }
  }

  @Override
  protected Object getObjectFromBlob(final ResultSet rs, final String colName)
      throws ClassNotFoundException, IOException, SQLException
  {
    byte[] bytes = rs.getBytes(colName);

    if(bytes != null && bytes.length != 0) {
      InputStream binaryInput = new ByteArrayInputStream(bytes);
      try (ObjectInputStream in = new ObjectInputStream(binaryInput)) {
        return in.readObject();
      }
    }

    return null;
  }

  @Override
  protected Object getJobDataFromBlob(final ResultSet rs, final String colName)
      throws ClassNotFoundException, IOException, SQLException
  {
    if (canUseProperties()) {
      byte[] bytes = rs.getBytes(colName);
      if(bytes == null || bytes.length == 0) {
        return null;
      }
      return new ByteArrayInputStream(bytes);
    }
    return getObjectFromBlob(rs, colName);
  }

  // Workaround for Orient bug with "LIKE '%'"
  // https://www.prjhub.com/#/issues/10825
  @Override
  protected String toSqlLikeClause(final GroupMatcher<?> matcher) {
    String groupName = super.toSqlLikeClause(matcher);
    return "%".equals(groupName) ? "%%" : groupName;
  }

  // Workaround for Orient not respecting setMaxRows and setFetchSize
  // on PreparedStatement and a Quartz bug not respecting maxCount
  // https://github.com/quartz-scheduler/quartz/issues/491
  @Override
  public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan, int maxCount)
      throws SQLException {
    PreparedStatement ps = null;
    ResultSet rs = null; // NOSONAR
    List<TriggerKey> nextTriggers = new LinkedList<>();
    try {
      ps = conn.prepareStatement(rtp(SELECT_NEXT_TRIGGER_TO_ACQUIRE));

      // Set max rows to retrieve
      if (maxCount < 1) {
        maxCount = 1; // we want at least one trigger back. NOSONAR
      }

      ps.setString(1, STATE_WAITING);
      ps.setBigDecimal(2, new BigDecimal(String.valueOf(noLaterThan)));
      ps.setBigDecimal(3, new BigDecimal(String.valueOf(noEarlierThan)));
      rs = ps.executeQuery();

      while (rs.next() && nextTriggers.size() < maxCount) {
        nextTriggers.add(triggerKey(
            rs.getString(COL_TRIGGER_NAME),
            rs.getString(COL_TRIGGER_GROUP)));
      }

      return nextTriggers;
    } finally {
      closeResultSet(rs);
      closeStatement(ps);
    }
  }
}
