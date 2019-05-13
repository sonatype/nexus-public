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
package org.sonatype.nexus.onboarding;

/**
 * An item that needs to be configured in the onboarding process in the UI, before other actions can be taken.
 *
 * @see OnboardingManager
 * @since 3.next
 */
public interface OnboardingItem
{
  /**
   * @return A unique string that defines the type of this onboarding item
   */
  String getType();

  /**
   * @return true if the item needs to be resolved, false if already done
   */
  boolean applies();

  /**
   * @return priority of the item in relation to other items (i.e. order shown in the onboarding wizard),
   * {@link Integer#MIN_VALUE} is highest priority
   */
  int getPriority();
}
