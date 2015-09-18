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
package org.sonatype.nexus.orient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Database connection that analyzes queries as they run and logs the 'explain' report.
 *
 * @since 3.0
 */
public class ExplainODatabaseDocumentTx
    extends ODatabaseDocumentTx
{
  private Logger log;

  public ExplainODatabaseDocumentTx(final String url, final Logger log) {
    super(url, false);

    this.log = checkNotNull(log);
  }

  @Override
  public OCommandRequest command(final OCommandRequest command) {
    final OCommandRequest request = super.command(command);

    // only analyze the performance of synchronous SQL commands/queries
    if (log.isTraceEnabled() && (request instanceof OCommandSQL || request instanceof OSQLSynchQuery)) {
      final String sql = ((OCommandRequestText) request).getText();

      // intercept 'execute' so we can enable metrics and report performance
      return (OCommandRequest) Proxy.newProxyInstance(OCommandRequest.class.getClassLoader(),
          new Class<?>[] { OCommandRequest.class }, new InvocationHandler()
          {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
              if (!"execute".equals(method.getName())) {
                return method.invoke(request, args);
              }

              request.getContext().setRecordingMetrics(true);

              final long startTime = System.nanoTime();
              final Object result = method.invoke(request, args);
              final long stopTime = System.nanoTime();

              final ODocument report = new ODocument(request.getContext().getVariables());
              report.field("elapsed", (stopTime - startTime) / 1000000f);
              log.debug("\n{}\n{}\n", sql, report.toJSON("prettyPrint"));

              request.getContext().setRecordingMetrics(false);

              return result;
            }
          });
    }
    return request;
  }
}
