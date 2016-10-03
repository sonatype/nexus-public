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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook
import org.sonatype.nexus.webhooks.GlobalWebhook
import org.sonatype.nexus.webhooks.WebhookService
import org.sonatype.nexus.webhooks.WebhookType

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions

/**
 * Webhook {@link DirectComponent}.
 *
 * @since 3.1
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Webhook')
class WebhookComponent
    extends DirectComponentSupport
{
  @Inject
  WebhookService webhookService

  private List<ReferenceXO> findWebhooksWithType(final WebhookType type) {
    return webhookService.webhooks.findAll {
      it.type == type
    }
    .collect {
      new ReferenceXO(id: it.name, name: it.name)
    }
  }

  /**
   * Returns all {@link GlobalWebhook} instances.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  List<ReferenceXO> listWithTypeGlobal() {
    return findWebhooksWithType(GlobalWebhook.TYPE)
  }

  /**
   * Returns all {@link RepositoryWebhook} instances.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  List<ReferenceXO> listWithTypeRepository() {
    return findWebhooksWithType(RepositoryWebhook.TYPE)
  }
}
