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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Model will be serialized into Json representation for npm audit report.
 *
 * @since 3.24
 */
public class Action
{
  @SerializedName("isMajor")
  private final boolean isMajor;

  @SerializedName("action")
  private final String actionType;

  private final List<Resolve> resolves;

  private final String module;

  private final String target;

  private final int depth;

  public Action(
      final boolean isMajor,
      final String actionType,
      final List<Resolve> resolves,
      final String module,
      final String target,
      final int depth)
  {
    this.isMajor = isMajor;
    this.actionType = actionType;
    this.resolves = resolves;
    this.module = module;
    this.target = target;
    this.depth = depth;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Action action1 = (Action) o;

    return isMajor == action1.isMajor &&
        depth == action1.depth &&
        Objects.equals(actionType, action1.actionType) &&
        Objects.equals(resolves, action1.resolves) &&
        Objects.equals(module, action1.module) &&
        Objects.equals(target, action1.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isMajor, actionType, resolves, module, target, depth);
  }
}
