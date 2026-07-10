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
package org.sonatype.nexus.bootstrap.entrypoint;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the NEXUS-46395 fix in {@link SpringComponentScan#getChildContext()} that propagates the parent
 * context's {@code Environment} to the child via {@code setEnvironment(parent.getEnvironment())} rather than
 * relying on {@code setParent(...)}'s implicit merge.
 *
 * <p>
 * The bug this fix addresses only reproduces against the cloud-aws assembly at boot — a downstream
 * {@code NoSuchBeanDefinitionException} for {@code RepositoryManager} caused by
 * {@code @ConditionalOnEdition} not seeing {@code nexus.edition} on the child's environment. Without a
 * focused test, a future refactor (e.g. someone removing the explicit {@code setEnvironment} call thinking
 * the implicit merge is sufficient) would silently regress against that assembly only.
 */
class SpringComponentScanTest
{
  private AnnotationConfigApplicationContext parent;

  private AnnotationConfigApplicationContext child;

  @AfterEach
  void tearDown() {
    if (child != null) {
      child.close();
    }
    if (parent != null) {
      parent.close();
    }
  }

  /**
   * After {@code getChildContext()} runs, the child's {@code Environment} must be the parent's
   * (same identity, not a merged copy) so that property sources added to the parent <em>after</em>
   * the child is created remain visible during the child's condition evaluation.
   */
  @Test
  void childContextSharesParentEnvironmentIdentity() {
    parent = new AnnotationConfigApplicationContext();
    parent.refresh();

    SpringComponentScan underTest = new SpringComponentScan(parent);
    child = underTest.getChildContext();

    assertThat(child.getEnvironment())
        .as("Child must reuse the parent's Environment instance, not a merged copy. "
            + "If this fails, the @ConditionalOnEdition evaluation timing fix has regressed.")
        .isSameAs(parent.getEnvironment());
  }

  /**
   * Property sources added to the parent <em>after</em> {@code getChildContext()} ran must be visible
   * to the child. This is the actual cloud-aws boot scenario: {@code ApplicationLauncher} adds
   * {@code nexus.edition} to the parent's environment between {@code getChildContext()} and the
   * scan-driven condition evaluation. With the implicit-merge approach this property would not show
   * up; with the explicit {@code setEnvironment} fix it does.
   */
  @Test
  void parentPropertySourcesAddedAfterChildCreationAreVisibleToChild() {
    parent = new AnnotationConfigApplicationContext();
    parent.refresh();

    SpringComponentScan underTest = new SpringComponentScan(parent);
    child = underTest.getChildContext();

    // Add the property source to the parent AFTER the child has been created. With Spring's implicit
    // setParent merge, the child's environment would have already been frozen with the parent's then-
    // current sources; with the NEXUS-46395 fix the child reads through to the parent's live env.
    parent.getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource("nexus-edition", Map.of("nexus.edition", "PRO_EDITION")));

    assertThat(child.getEnvironment().getProperty("nexus.edition"))
        .as("Child must observe property sources added to parent after the child was created. "
            + "If null, @ConditionalOnEdition will silently reject every cloud-only bean.")
        .isEqualTo("PRO_EDITION");
  }

  /**
   * The child's parent reference must point at the supplied parent context — sanity check that the
   * environment-identity fix didn't accidentally also break the parent linkage.
   */
  @Test
  void childContextIsLinkedToParent() {
    parent = new AnnotationConfigApplicationContext();
    parent.refresh();

    SpringComponentScan underTest = new SpringComponentScan(parent);
    child = underTest.getChildContext();

    assertThat(child.getParent()).isSameAs(parent);
  }
}
