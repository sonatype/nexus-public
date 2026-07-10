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
package org.sonatype.nexus.repository.manager.internal;

import java.net.URI;
import java.util.Map;

import javax.annotation.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
import org.sonatype.nexus.validation.ConstraintViolationFactory;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class RemoteUrlSsrfValidator
    implements ConfigurationValidator
{
  private static final String PROXY = "proxy";

  private static final String REMOTE_URL = "remoteUrl";

  private final ConstraintViolationFactory constraintViolationFactory;

  private final AntiSsrfService antiSsrfService;

  @Autowired
  public RemoteUrlSsrfValidator(
      final ConstraintViolationFactory constraintViolationFactory,
      final AntiSsrfService antiSsrfService)
  {
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.antiSsrfService = checkNotNull(antiSsrfService);
  }

  @Nullable
  @Override
  public ConstraintViolation<?> validate(final Configuration configuration) {
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes != null && attributes.containsKey(PROXY)) {
      Map<String, Object> proxyAttributes = attributes.get(PROXY);
      if (proxyAttributes != null && proxyAttributes.containsKey(REMOTE_URL)) {
        Object remoteUrlObj = proxyAttributes.get(REMOTE_URL);
        if (remoteUrlObj != null) {
          try {
            URI remoteUrl;
            if (remoteUrlObj instanceof String uri) {
              remoteUrl = new URI(uri);
            }
            else {
              return null;
            }

            String host = remoteUrl.getHost();
            if (host != null) {
              antiSsrfService.validateHostWithoutCache(host);
            }
          }
          catch (ValidationException e) {
            return constraintViolationFactory.createViolation(
                PROXY + "." + REMOTE_URL,
                "Proxy URL blocked: " + e.getMessage());
          }
          catch (Exception e) {
            return constraintViolationFactory.createViolation(
                PROXY + "." + REMOTE_URL,
                "Invalid proxy URL format");
          }
        }
      }
    }
    return null;
  }
}
