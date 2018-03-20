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
package org.sonatype.nexus.repository.maven.tasks;

import com.google.common.base.Strings;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.PurgeUnusedReleasesFacet;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class PurgeMavenUnusedReleasesTask  extends RepositoryTaskSupport
        implements Cancelable
{

    public static final String NUMBER_RELEASES_TO_KEEP = "numberOfReleasesToKeep";

    public static final String PURGE_UNUSED_MAVEN_RELEASES_MESSAGE = "Purge unused Maven releases versions in this repository %s";
    public static final String OPTION_FOR_PURGE_ID = "optionForPurge";

    private final Type hostedType;

    private final Format maven2Format;

    @Inject
    public PurgeMavenUnusedReleasesTask(
                                         @Named(HostedType.NAME) final Type hostedType,
                                         @Named(Maven2Format.NAME) final Format maven2Format)
    {
        this.hostedType = checkNotNull(hostedType);
        this.maven2Format = checkNotNull(maven2Format);
    }

    @Override
    protected void execute(final Repository repository) {
        String option = !Strings.isNullOrEmpty(getConfiguration().getString(OPTION_FOR_PURGE_ID)) ? getConfiguration().getString(OPTION_FOR_PURGE_ID) : "version";
        int numberOfReleasesToKeep = getConfiguration().getInteger(NUMBER_RELEASES_TO_KEEP, 1);
        repository.facet(PurgeUnusedReleasesFacet.class).purgeUnusedReleases(numberOfReleasesToKeep, option);

    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return maven2Format.equals(repository.getFormat())
                && hostedType.equals(repository.getType())
                && (repository.facet(MavenFacet.class).getVersionPolicy() == VersionPolicy.RELEASE
                    || repository.facet(MavenFacet.class).getVersionPolicy() == VersionPolicy.MIXED);
    }

    @Override
    public String getMessage() {
        return String.format(PURGE_UNUSED_MAVEN_RELEASES_MESSAGE,
                getRepositoryField()) ;
    }
}
