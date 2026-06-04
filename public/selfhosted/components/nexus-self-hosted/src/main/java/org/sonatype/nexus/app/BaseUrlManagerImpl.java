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
package org.sonatype.nexus.app;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.common.app.AbstractBaseUrlManager;
import org.sonatype.nexus.common.app.BaseUrlManager;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

/**
 * Default {@link BaseUrlManager}.
 *
 * @since 3.0
 */
@Component
public class BaseUrlManagerImpl
    extends AbstractBaseUrlManager
{
  private volatile boolean force;

  @Autowired
  public BaseUrlManagerImpl(
      final Provider<HttpServletRequest> requestProvider,
      @Value("${org.sonatype.nexus.internal.app.BaseUrlManagerImpl.force:false}") final boolean force)
  {
    super(requestProvider);
    this.force = force;
  }

  @Override
  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public boolean isForce() {
    return force;
  }

  @Override
  public void setForce(final boolean force) {
    this.force = force;
  }

  /**
   * Detect base-url from forced settings, request or non-forced settings.
   */
  @Nullable
  @Override
  public String detectUrl() {
    // force base-url always wins if set
    if (force && !Strings.isNullOrEmpty(url)) {
      return url;
    }
    return super.detectUrl();
  }
}
