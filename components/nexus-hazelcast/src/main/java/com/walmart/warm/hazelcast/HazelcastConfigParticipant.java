/**
 * Copyright (c) 2016-current Walmart, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.walmart.warm.hazelcast;

import com.hazelcast.config.Config;

/**
 * Implementations of this component may alter or verify that Hazelcast configuration contains needed elements.
 *
 * @since 1.2.14
 */
public interface HazelcastConfigParticipant
{
  /**
   * Applies needed changes to loaded up Hazelcast configuration by mutating whatever is needed by given participant.
   * The passed in {@code config} is never {@code null}.
   */
  void apply(Config config);
}
