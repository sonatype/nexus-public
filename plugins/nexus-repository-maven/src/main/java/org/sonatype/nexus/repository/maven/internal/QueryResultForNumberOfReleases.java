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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Objects;

public class QueryResultForNumberOfReleases {

    private final String groupId;
    private final String artifactId;
    private final Long count;


    public QueryResultForNumberOfReleases(String groupId, String artifactId, Long count) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.count = count;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResultForNumberOfReleases that = (QueryResultForNumberOfReleases) o;
        return count == that.count &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(groupId, artifactId, count);
    }

    @Override
    public String toString() {
        return "QueryResultForNumberOfReleases{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", count=" + count +
                '}';
    }
}
