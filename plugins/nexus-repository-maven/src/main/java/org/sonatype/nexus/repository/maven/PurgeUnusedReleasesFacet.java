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
package org.sonatype.nexus.repository.maven;

import org.sonatype.nexus.repository.Facet;

/**
 * Facet for purging unused Maven releases.
 *
 * @since 3.7
 */
@Facet.Exposed
public interface PurgeUnusedReleasesFacet extends Facet {


    /**
     * Purge the unused releases and keep the number of releases in parameter
     * @param numberOfReleasesToKeep - the number of releases to keep
     * @param option - Option used for purge
     */
    void purgeUnusedReleases(int numberOfReleasesToKeep, String option);

}
