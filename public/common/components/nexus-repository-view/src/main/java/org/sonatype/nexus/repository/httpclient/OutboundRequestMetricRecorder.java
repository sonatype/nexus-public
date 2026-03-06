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
package org.sonatype.nexus.repository.httpclient;

/**
 * Interface for recording outbound HTTP request metrics from proxy repositories.
 * <p>
 * Implementations collect telemetry data about requests made to remote registries
 * (e.g., npmjs.org, Maven Central) for analytics purposes.
 */
public interface OutboundRequestMetricRecorder
{
  String CONTEXT_FORMAT = "nexus.outbound.format";

  String CONTEXT_REPOSITORY_TYPE = "nexus.outbound.repositoryType";

  /**
   * Record an outbound request.
   *
   * @param format Repository format (e.g., "maven2", "npm", "docker", "nuget")
   * @param repositoryType Repository type (e.g., "proxy")
   * @param httpMethod HTTP method (e.g., "GET", "HEAD")
   */
  void record(String format, String repositoryType, String httpMethod);
}
