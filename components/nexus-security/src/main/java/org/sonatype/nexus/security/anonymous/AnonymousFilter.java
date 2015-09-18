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
package org.sonatype.nexus.security.anonymous;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Binds special anonymous subject if current subject is guest and anonymous access is enabled.
 *
 * @see AnonymousManager
 * @since 3.0
 */
@Named
@Singleton
public class AnonymousFilter
    extends AdviceFilter
{
  public static final String NAME = "nx-anonymous";

  private static final String ORIGINAL_SUBJECT = AnonymousFilter.class.getName() + ".originalSubject";

  private static final Logger log = LoggerFactory.getLogger(AnonymousFilter.class);

  private final Provider<AnonymousManager> anonymousManager;

  @Inject
  public AnonymousFilter(final Provider<AnonymousManager> anonymousManager) {
    this.anonymousManager = checkNotNull(anonymousManager);
  }

  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    Subject subject = SecurityUtils.getSubject();

    if (subject.getPrincipal() == null && anonymousManager.get().isEnabled()) {
      request.setAttribute(ORIGINAL_SUBJECT, subject);
      subject = anonymousManager.get().buildSubject();
      ThreadContext.bind(subject);
      log.trace("Bound anonymous subject: {}", subject);
    }

    return true;
  }

  @Override
  public void afterCompletion(final ServletRequest request, final ServletResponse response, final Exception exception)
      throws Exception
  {
    Subject subject = (Subject) request.getAttribute(ORIGINAL_SUBJECT);
    if (subject != null) {
      log.trace("Binding original subject: {}", subject);
      ThreadContext.bind(subject);
    }
  }
}
