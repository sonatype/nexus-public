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

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.client.internal.util.Check;

/**
 * Set of basic logical operators that combines multiple {@link org.sonatype.nexus.client.core.Condition}s into one.
 *
 * @since 2.1
 */
public abstract class LogicalConditions
{

  /**
   * Creates a {@link org.sonatype.nexus.client.core.Condition} that requires that all members match. In other words,
   * it
   * applies logical "AND" operation to them.
   *
   * @return {@code true} if all passed in conditions match.
   */
  public static Condition and(final Condition... conditions) {
    Check.argument(conditions.length > 1, "At least two operators expected!");
    return new Condition()
    {
      @Override
      public boolean isSatisfiedBy(final NexusStatus status) {
        for (Condition condition : conditions) {
          if (!condition.isSatisfiedBy(status)) {
            return false;
          }
        }
        return true;
      }

      private static final String KW = " AND ";

      @Override
      public String explainNotSatisfied(final NexusStatus status) {
        final Condition lastCondition = conditions[conditions.length - 1];
        final StringBuilder explanation = new StringBuilder("(");
        for (Condition condition : conditions) {
          explanation.append(condition.explainNotSatisfied(status));
          if (condition != lastCondition) {
            explanation.append(KW);
          }
        }
        return explanation.append(")").toString();
      }
    };
  }

  /**
   * Creates a {@link org.sonatype.nexus.client.core.Condition} that requires that any members match. In other words,
   * it
   * applies logical "OR" operation to them.
   *
   * @return {@code true} if any passed in matchers match.
   */
  public static Condition or(final Condition... conditions) {
    Check.argument(conditions.length > 1, "At least two operators expected!");
    return new Condition()
    {
      @Override
      public boolean isSatisfiedBy(final NexusStatus status) {
        for (Condition condition : conditions) {
          if (condition.isSatisfiedBy(status)) {
            return true;
          }
        }
        return false;
      }

      private static final String KW = " OR ";

      @Override
      public String explainNotSatisfied(final NexusStatus status) {
        final Condition lastCondition = conditions[conditions.length - 1];
        final StringBuilder explanation = new StringBuilder("(");
        for (Condition condition : conditions) {
          explanation.append(condition.explainNotSatisfied(status));
          if (condition != lastCondition) {
            explanation.append(KW);
          }
        }
        return explanation.append(")").toString();
      }
    };
  }

  /**
   * Creates a {@link org.sonatype.nexus.client.core.Condition} that return negation of the passed in matcher match.
   * In
   * other words, it applies logical "NOT" operation to it.
   *
   * @return {@code true} if all passed in matchers match.
   */
  public static Condition not(final Condition condition) {
    Check.notNull(condition, Condition.class);
    return new Condition()
    {
      @Override
      public boolean isSatisfiedBy(final NexusStatus status) {
        return !condition.isSatisfiedBy(status);
      }

      @Override
      public String explainNotSatisfied(NexusStatus status) {
        return "(NOT " + condition.explainNotSatisfied(status) + ")";
      }
    };
  }
}
