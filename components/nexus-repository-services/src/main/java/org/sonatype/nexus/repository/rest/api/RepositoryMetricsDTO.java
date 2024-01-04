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
package org.sonatype.nexus.repository.rest.api;

import org.joda.time.DateTime;

/**
 * Repository Metrics DTO
 */
public class RepositoryMetricsDTO
{
    public String repositoryName;

    public String repositoryFormat;

    public Long totalSize;

    public Long blobCount;

    public DateTime lastUpdated;

    public RepositoryMetricsDTO(String repositoryName, String repositoryFormat, Long totalSize,
                                Long blobCount, DateTime lastUpdated) {
        this.repositoryName = repositoryName;
        this.repositoryFormat = repositoryFormat;
        this.totalSize = totalSize;
        this.blobCount = blobCount;
        this.lastUpdated = lastUpdated;
    }

    public RepositoryMetricsDTO(String repositoryName, Long totalSize) {
        this.repositoryName = repositoryName;
        this.totalSize = totalSize;
    }

    public String getName() { return repositoryName; }

    public Long getSize() { return totalSize; }

    public DateTime getLastUpdated() { return lastUpdated; }

    @Override
    public String toString() {
        return "RepositoryMetricsDTO{" +
                "repositoryName='" + repositoryName + '\'' +
                ", repositoryFormat='" + repositoryFormat + '\'' +
                ", totalSize=" + totalSize +
                ", blobCount=" + blobCount +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
