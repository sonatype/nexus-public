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
package org.sonatype.nexus.api.common.shared.state;

import java.util.Map;

import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.HOSTED_REPOSITORY_EVALUATION_ENABLED;

/**
 * Only registered when the hosted-repository-evaluation backend module (private/Pro) is on the
 * classpath — gated on the presence of {@code GlobalEvaluationSettingsApiResource} rather than on
 * the {@code HOSTED_REPOSITORY_EVALUATION_ENABLED} flag, because the flag only says whether the
 * feature is switched on, not whether its backend module is loaded (OSS Core lacks the module
 * entirely, while the flag defaults to {@code true}).
 * <p>
 * Once registered, the published value still follows the flag: an operator who sets
 * {@code nexus.hosted.repository.evaluation.enabled=false} switches the backend beans off, so the
 * UI must hide the evaluation screens too rather than render pages whose REST endpoints are gone.
 * The flag is read the same way as the backend's bean conditions: only unset or {@code true} (any
 * case) counts as on, so values such as {@code off} or {@code yes} hide the screens just as they
 * drop the beans.
 */
@Component
@ConditionalOnClass(name = "com.sonatype.nexus.hostedrepository.evaluation.rest.GlobalEvaluationSettingsApiResource")
public class HostedRepositoryEvaluationStateContributor
    implements StateContributor
{
  private final boolean isHostedRepositoryEvaluationEnabled;

  @Autowired
  public HostedRepositoryEvaluationStateContributor(final Environment environment) {
    this("true".equalsIgnoreCase(environment.getProperty(HOSTED_REPOSITORY_EVALUATION_ENABLED, "true")));
  }

  HostedRepositoryEvaluationStateContributor(final boolean isHostedRepositoryEvaluationEnabled) {
    this.isHostedRepositoryEvaluationEnabled = isHostedRepositoryEvaluationEnabled;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of("hostedRepositoryEvaluationEnabled", isHostedRepositoryEvaluationEnabled);
  }
}
