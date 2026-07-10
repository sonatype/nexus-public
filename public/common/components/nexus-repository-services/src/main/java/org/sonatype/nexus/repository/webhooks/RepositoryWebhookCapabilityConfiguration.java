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

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.config.RepositoryName;
import org.sonatype.nexus.validation.constraint.Url;
import org.sonatype.nexus.validation.constraint.SsrfSafeUrl;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class RepositoryWebhookCapabilityConfiguration
    extends CapabilityConfigurationSupport
    implements RepositoryWebhook.Configuration
{
  static final String P_REPOSITORY = "repository";

  static final String P_NAMES = "names";

  static final String P_URL = "url";

  static final String P_SECRET = "secret";

  @RepositoryName
  String repository;

  @RepositoryWebhookType
  List<String> names;

  @SsrfSafeUrl
  @Url
  URI url;

  @Nullable
  private final String secretId;

  private final SecretsService secretsService;

  RepositoryWebhookCapabilityConfiguration(
      final Map<String, String> properties,
      final SecretsService secretsService)
  {
    repository = properties.get(P_REPOSITORY);
    names = parseList(properties.get(P_NAMES));
    url = parseUri(properties.get(P_URL));
    // Store only the secret ID - decrypt on-demand when needed
    this.secretId = Strings.emptyToNull(properties.get(P_SECRET));
    this.secretsService = secretsService;
  }

  private static final Splitter LIST_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private static List<String> parseList(final String value) {
    return LIST_SPLITTER.splitToList(value);
  }

  @Override
  public String getRepository() {
    return repository;
  }

  public List<String> getNames() {
    return names;
  }

  @Override
  public URI getUrl() {
    return url;
  }

  @Override
  @Nullable
  public String getSecret() {
    return decryptSecret(secretId, secretsService);
  }
}
