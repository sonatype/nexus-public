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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Response sender selector.
 *
 * @since 3.0
 */
@Component
class HttpResponseSenderSelector
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, HttpResponseSender> responseSenders;

  private final DefaultHttpResponseSender defaultHttpResponseSender;

  @Autowired
  public HttpResponseSenderSelector(
      final List<HttpResponseSender> responseSendersList,
      final DefaultHttpResponseSender defaultHttpResponseSender)
  {
    this.responseSenders = QualifierUtil.buildQualifierBeanMap(checkNotNull(responseSendersList));
    this.defaultHttpResponseSender = checkNotNull(defaultHttpResponseSender);
  }

  /**
   * Returns the default sender.
   */
  @Nonnull
  public HttpResponseSender defaultSender() {
    return defaultHttpResponseSender;
  }

  /**
   * Find sender for repository format.
   *
   * If no format-specific sender is configured, the default is used.
   */
  @Nonnull
  public HttpResponseSender sender(final Repository repository) {
    String format = repository.getFormat().getValue();
    log.debug("Looking for HTTP response sender: {}", format);
    HttpResponseSender sender = responseSenders.get(format);
    if (sender == null) {
      return defaultHttpResponseSender;
    }
    return sender;
  }
}
