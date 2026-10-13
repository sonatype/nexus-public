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

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.common.app.FeatureFlags.HOSTED_REPOSITORY_EVALUATION_ENABLED;

public class HostedRepositoryEvaluationStateContributorTest
{
  private static final String STATE_KEY = "hostedRepositoryEvaluationEnabled";

  @Test
  public void shouldReturnEnabledWhenFeatureFlagIsTrue() {
    HostedRepositoryEvaluationStateContributor underTest =
        new HostedRepositoryEvaluationStateContributor(true);

    assertThat(underTest.getState().get(STATE_KEY), is(true));
  }

  @Test
  public void shouldReturnDisabledWhenFeatureFlagIsFalse() {
    HostedRepositoryEvaluationStateContributor underTest =
        new HostedRepositoryEvaluationStateContributor(false);

    assertThat(underTest.getState().get(STATE_KEY), is(false));
  }

  @Test
  public void shouldReturnEnabledWhenFeatureFlagIsUnset() {
    assertThat(stateFor(environmentWithoutSystemSources()), is(true));
  }

  @Test
  public void shouldReadTheFlagLikeTheBackendBeanConditions() {
    assertThat(stateFor(environmentWith("true")), is(true));
    assertThat(stateFor(environmentWith("TRUE")), is(true));
    assertThat(stateFor(environmentWith("false")), is(false));
    // Spring would convert these to a boolean, but the backend's bean conditions treat them as disabled
    assertThat(stateFor(environmentWith("off")), is(false));
    assertThat(stateFor(environmentWith("yes")), is(false));
    assertThat(stateFor(environmentWith("1")), is(false));
  }

  private static Object stateFor(final StandardEnvironment environment) {
    return new HostedRepositoryEvaluationStateContributor(environment).getState().get(STATE_KEY);
  }

  private static StandardEnvironment environmentWith(final String value) {
    StandardEnvironment environment = environmentWithoutSystemSources();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("test", Map.of(HOSTED_REPOSITORY_EVALUATION_ENABLED, value)));
    return environment;
  }

  /**
   * An environment that ignores system properties and environment variables, so a flag set on the machine running the
   * tests can't change the "unset" case.
   */
  private static StandardEnvironment environmentWithoutSystemSources() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    return environment;
  }
}
