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
package org.sonatype.nexus.blobstore.group.internal

import javax.inject.Provider

import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link RoundRobinFillPolicy} tests.
 */
class RoundRobinFillPolicyTest
    extends Specification
{
  RoundRobinFillPolicy roundRobinFillPolicy = new RoundRobinFillPolicy()

  @Unroll
  def 'nextIndex gives expected value when starting at #initialValue'() {
    when: 'a roundRobinFillPolicy has a given value'
      roundRobinFillPolicy.sequence.set(initialValue)
      def currentIndex = roundRobinFillPolicy.nextIndex()
      def nextIndex = roundRobinFillPolicy.nextIndex()

    then: 'the next value wraps around to 0 to avoid negative indexes'
      currentIndex == expectedCurrentIndex
      nextIndex == expectedNextIndex

    where:
      initialValue      || expectedCurrentIndex | expectedNextIndex
      0                 || 0                    | 1
      Integer.MAX_VALUE || Integer.MAX_VALUE    | 0
  }
}
