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
package org.sonatype.nexus.client.core.condition;

import java.util.regex.Pattern;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.client.internal.util.Check;

/**
 * Edition conditions that contains {@link Condition}s to match remote Nexus edition.
 *
 * @since 2.1
 */
public abstract class EditionConditions
{

  /**
   * Edition pattern that matches everything.
   */
  private static final Pattern ALL_EDITIONS = Pattern.compile(".*");

  /**
   * Edition pattern that matches OSS edition.
   */
  private static final Pattern OSS_EDITION = Pattern.compile("OSS");

  /**
   * Edition pattern that matches Professional editions (both registered and evaluation).
   */
  private static final Pattern PRO_EDITION = Pattern.compile("^PRO.*$");

  /**
   * Edition pattern that matches registered Professional edition (not matched evaluation instances).
   */
  private static final Pattern REGISTERED_PRO_EDITION = Pattern.compile("^PRO$");

  public static Condition anyEdition() {
    return new EditionCondition(ALL_EDITIONS);
  }

  public static Condition anyOssEdition() {
    return new EditionCondition(OSS_EDITION);
  }

  public static Condition anyProEdition() {
    return new EditionCondition(PRO_EDITION);
  }

  public static Condition anyRegisteredProEdition() {
    return new EditionCondition(REGISTERED_PRO_EDITION);
  }

  private static class EditionCondition
      implements Condition
  {

    private final Pattern editionPattern;

    private EditionCondition(final Pattern editionPattern) {
      this.editionPattern = Check.notNull(editionPattern, Pattern.class);
    }

    @Override
    public boolean isSatisfiedBy(final NexusStatus status) {
      final String shortEdition = status.getEditionShort();
      return editionPattern.matcher(shortEdition).matches();
    }

    @Override
    public String explainNotSatisfied(final NexusStatus status) {
      return String.format("(edition \"%s\" matches \"%s\")", status.getEditionShort(), editionPattern);
    }
  }
}
