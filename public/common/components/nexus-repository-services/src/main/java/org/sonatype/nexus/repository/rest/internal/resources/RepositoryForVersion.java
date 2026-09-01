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
package org.sonatype.nexus.repository.rest.internal.resources;

/**
 * One row in the response of the "repositories containing a component version" endpoint.
 *
 * @param repositoryName the repository containing the selected version
 * @param type repository type value: "hosted", "proxy", or "group"
 * @param versionCount component-wide distinct-version count in this repository
 *          (across all versions of the component, not just the selected one)
 */
public record RepositoryForVersion(
    String repositoryName,
    String type,
    long versionCount)
{
}
