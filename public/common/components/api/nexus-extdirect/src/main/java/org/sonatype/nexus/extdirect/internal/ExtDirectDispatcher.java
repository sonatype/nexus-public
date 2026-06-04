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

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.extdirect.model.Response;

import com.softwarementors.extjs.djn.api.RegisteredMethod;
import com.softwarementors.extjs.djn.servlet.ssm.SsmDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.extdirect.model.Responses.success;
import org.springframework.stereotype.Component;

/**
 * @since 3.15
 */
@Component
@ManagedLifecycle(phase = SERVICES)
public class ExtDirectDispatcher
    extends SsmDispatcher
    implements ApplicationContextAware
{
  private static final Logger log = LoggerFactory.getLogger(ExtDirectDispatcher.class);

  private final ExtDirectExceptionHandler exceptionHandler;

  private ApplicationContext applicationContext;

  @Autowired
  public ExtDirectDispatcher(final ExtDirectExceptionHandler exceptionHandler) {
    this.exceptionHandler = checkNotNull(exceptionHandler);
  }

  @Override
  protected Object createInvokeInstanceForMethodWithDefaultConstructor(final RegisteredMethod method) {
    log.debug("Creating instance of action class '{}' mapped to '{}", method.getActionClass().getName(),
        method.getActionName());

    return applicationContext.getBean(method.getActionClass());
  }

  @Override
  protected Object invokeMethod(final RegisteredMethod method, final Object actionInstance, final Object[] parameters) {
    log.debug("Invoking action method: {}, java-method: {}", method.getFullName(), method.getFullJavaMethodName());

    Response response = null;

    MDC.put(getClass().getName(), method.getFullName());
    try {
      response = asResponse(super.invokeMethod(method, actionInstance, parameters));
    }
    catch (InvocationTargetException e) { // NOSONAR
      response = asResponse(exceptionHandler.handleException(method, e.getTargetException()));
    }
    catch (Exception e) {
      response = asResponse(exceptionHandler.handleException(method, e));
    }
    finally {
      MDC.remove(getClass().getName());
    }

    return response;
  }

  private Response asResponse(final Object result) {
    Response response;
    if (result == null) {
      response = success();
    }
    else {
      if (result instanceof Response) {
        response = (Response) result;
      }
      else {
        response = success(result);
      }
    }
    return response;
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
