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
package org.sonatype.nexus.internal.app;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.cache.BaseUrlChangedEvent;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Base-URL capability.
 *
 * @since 3.0
 */
@Component(BaseUrlCapabilityDescriptor.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BaseUrlCapability
    extends CapabilitySupport<BaseUrlCapabilityConfiguration>
{
  private final BaseUrlManager baseUrlManager;

  private final EventManager eventManager;

  @Autowired
  public BaseUrlCapability(final BaseUrlManager baseUrlManager, final EventManager eventManager) {
    this.baseUrlManager = requireNonNull(baseUrlManager);
    this.eventManager = requireNonNull(eventManager);
  }

  @Override
  protected BaseUrlCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new BaseUrlCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final BaseUrlCapabilityConfiguration config) throws Exception {
    String oldUrl = baseUrlManager.getUrl();
    baseUrlManager.setUrl(config.getUrl());
    maybePostUrlChangedEvent(oldUrl, config.getUrl());
  }

  @Override
  protected void onPassivate(final BaseUrlCapabilityConfiguration config) throws Exception {
    String oldUrl = baseUrlManager.getUrl();
    baseUrlManager.setUrl(null);
    maybePostUrlChangedEvent(oldUrl, null);
  }

  @Override
  protected void onUpdate(final BaseUrlCapabilityConfiguration config) throws Exception {
    super.onUpdate(config);
    String oldUrl = baseUrlManager.getUrl();
    baseUrlManager.setUrl(config.getUrl());
    maybePostUrlChangedEvent(oldUrl, config.getUrl());
  }

  /**
   * Posts a {@link BaseUrlChangedEvent} when the effective URL has changed.
   * No event is posted when old and new are equal (normalizing {@code null} == {@code null}).
   */
  private void maybePostUrlChangedEvent(final String oldUrl, final String newUrl) {
    if (!Objects.equals(oldUrl, newUrl)) {
      eventManager.post(new BaseUrlChangedEvent(oldUrl, newUrl));
    }
  }
}
