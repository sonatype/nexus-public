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
package org.sonatype.nexus.internal.security.apikey;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.authc.apikey.ApiKeyService;
import org.sonatype.nexus.security.usertoken.event.UserTokenPurgedEvent;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Purge orphaned API keys task.
 *
 * @since 3.0
 * @see ApiKeyService#purgeApiKeys()
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PurgeApiKeysTask
    extends TaskSupport
    implements Cancelable
{
  private final ApiKeyInternalService apiKeyService;

  private final EventManager eventManager;

  @Autowired
  public PurgeApiKeysTask(final ApiKeyInternalService store, final EventManager eventManager) {
    this.apiKeyService = checkNotNull(store);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected Void execute() throws Exception {
    int deleted = apiKeyService.purgeApiKeys();
    if (deleted > 0) {
      eventManager.post(new UserTokenPurgedEvent(deleted));
    }
    return null;
  }

  @Override
  public String getMessage() {
    return "Deleting orphaned API keys";
  }
}
