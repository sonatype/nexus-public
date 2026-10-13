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
package org.sonatype.nexus.common.app;

/**
 * Whether this instance is obliged to send telemetry.
 * <p>
 * The answer comes from the license, so the only implementation lives in a commercial module. Public
 * modules consume this interface with {@code @Autowired(required = false)} and <b>must treat an
 * absent bean as {@code true}</b>: the bean is missing on every edition and distribution that cannot
 * hold the opt-out entitlement — Community and Core among them — and those instances are all obliged
 * to send telemetry. Reading absence as "not required" would silently exempt the largest population
 * of instances, and no commercial-only test would catch it.
 */
public interface TelemetryEnforcement
{
  /**
   * Returns {@code true} unless the license carries the telemetry opt-out entitlement.
   */
  boolean isTelemetryRequired();
}
