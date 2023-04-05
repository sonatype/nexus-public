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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.mybatis.AbstractBytesTypeHandler;

import org.quartz.JobDataMap;

/*
 * Simplified from org.quartz.impl.jdbcjobstore.StdJDBCDelegate as Nexus configures Quartz for Property style
 * serialization rather than ObjectInputStream
 *
 * See also org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
 */
@Named
@Singleton
public class QuartzJobDataTypeHandler
    extends AbstractBytesTypeHandler<JobDataMap>
{
  @Inject
  public QuartzJobDataTypeHandler() {
    super(JobDataMap::new);
  }

  @Override
  protected JobDataMap deserialize(final InputStream in) {
    try (ObjectInputStream ois = new ObjectInputStream(in)) {
      return new JobDataMap((Map<?, ?>) ois.readObject());
    }
    catch (IOException e) {
      log.debug("An unepected error occurred", e);
      return null;
    }
    catch (ClassNotFoundException e) {
      log.debug("An unepected error occurred", e);
      return null;
    }
  }
}
