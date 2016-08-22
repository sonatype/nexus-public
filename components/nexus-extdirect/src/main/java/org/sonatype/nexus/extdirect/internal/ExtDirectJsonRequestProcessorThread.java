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
package org.sonatype.nexus.extdirect.internal;

import java.util.concurrent.Callable;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.security.UserIdMdcHelper;

import com.google.common.base.Throwables;
import com.google.inject.servlet.ServletScopes;
import com.softwarementors.extjs.djn.servlet.ssm.SsmJsonRequestProcessorThread;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;

import static com.google.common.base.Preconditions.checkState;

/**
 * An {@link SsmJsonRequestProcessorThread} that is binds the thread to Shiro subject as well as setting user id in
 * MDC.
 *
 * @since 3.0
 */
public class ExtDirectJsonRequestProcessorThread
    extends SsmJsonRequestProcessorThread
{

  private final SubjectThreadState threadState;

  private final Callable<String> processRequest;

  public ExtDirectJsonRequestProcessorThread() {
    Subject subject = SecurityUtils.getSubject();
    checkState(subject != null, "Subject is not set");
    // create the thread state by this moment as this is created in the master (web container) thread
    threadState = new SubjectThreadState(subject);

    final String baseUrl = BaseUrlHolder.get();

    processRequest = ServletScopes.transferRequest(new Callable<String>()
    {
      @Override
      public String call() {
        threadState.bind();
        UserIdMdcHelper.set();
        try {
          // apply base-url from the original thread
          BaseUrlHolder.set(baseUrl);

          return ExtDirectJsonRequestProcessorThread.super.processRequest();
        }
        finally {
          UserIdMdcHelper.unset();
          threadState.restore();
        }

      }
    });
  }

  @Override
  public String processRequest() {
    try {
      return processRequest.call();
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
