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
package org.sonatype.nexus.repository.webhooks;

import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookConfiguration;
import org.sonatype.nexus.webhooks.WebhookType;

/**
 * Repository {@link Webhook}.
 *
 * @since 3.1
 */
public abstract class RepositoryWebhook
    extends Webhook
{
  public static final WebhookType TYPE = new WebhookType("repository") {};

  public final WebhookType getType() {
    return TYPE;
  }

  /**
   * Additional configuration exposed for {@link RepositoryWebhook}.
   */
  public interface Configuration
      extends WebhookConfiguration
  {
    String getRepository();
  }
}
