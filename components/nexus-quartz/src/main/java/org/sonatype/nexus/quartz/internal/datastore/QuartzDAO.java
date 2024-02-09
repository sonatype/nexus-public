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
package org.sonatype.nexus.quartz.internal.datastore;

import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;

import org.quartz.impl.jdbcjobstore.JobStoreTX;

/**
 * Quartz data access object used to create the schema that {@link JobStoreTX} expects.
 *
 * @since 3.19
 */
public interface QuartzDAO
    extends DataAccess
{
  List<QuartzTaskStateData> getStates();

  Optional<QuartzTaskStateData> getState(String jobName);

  void updateJobDataMap(QuartzTaskStateData taskState);
}
