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

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.PurgeUnusedReleasesFacet;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Named;
import java.util.*;

import static java.util.Collections.*;
import static org.sonatype.nexus.orient.entity.AttachedEntityHelper.id;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.maven.internal.QueryPurgeReleasesBuilder.DATE_RELEASE_OPTION;
import static org.sonatype.nexus.repository.maven.internal.QueryPurgeReleasesBuilder.VERSION_OPTION;

@Named
public class PurgeUnusedReleasesFacetImpl extends FacetSupport
        implements PurgeUnusedReleasesFacet {


    private static final String MESSAGE_PURGE_NOT_EXECUTED = "The purge of the releases {}.{} in the repository {} cannot be done because the number of existing releases is below the number of releases to keep";

    static final int PAGINATION_LIMIT = 10;

    private final String REQUEST_TOTAL_RELEASES_BY_RELEASE = "select count(*), attributes.maven2.groupId as groupId, attributes.maven2.artifactId as artifactId from component where bucket = %s " +
            " group by attributes.maven2.artifactId, attributes.maven2.groupId limit %s";


    @Override
    @Guarded(by = STARTED)
    public void purgeUnusedReleases(int numberOfReleasesToKeep, String option) {
        if (optionalFacet(StorageFacet.class).isPresent()) {
            TransactionalStoreMetadata.operation.withDb(facet(StorageFacet.class).txSupplier()).call(() -> {
                final StorageTx tx = UnitOfWork.currentTx();
                ORID bucketId = id(Objects.requireNonNull(tx.findBucket(getRepository())));
                List<QueryResultForNumberOfReleases> listReleases = listNumberOfReleases(tx, bucketId);
                for (QueryResultForNumberOfReleases q : listReleases) {
                    long nbReleasesToPurge = q.getCount() - numberOfReleasesToKeep;
                    String repositoryName = getRepository().getName();
                    if (nbReleasesToPurge <= 0) {
                        log.debug(MESSAGE_PURGE_NOT_EXECUTED, q.getGroupId(), q.getArtifactId(), repositoryName);
                    } else {
                        log.debug("Number of releases to purge for the repository {} : {} ", nbReleasesToPurge, repositoryName);
                        process(tx, q.getGroupId(), q.getArtifactId(), option, nbReleasesToPurge, bucketId);
                    }
                }

                return null;
            });
        }
    }


    /**
     * Processing the purge
     * @param groupId - Group Id of the component
     * @param artifactId - Artifact Id of the component
     * @param option - Option used to order by for purge
     * @param bucketId - The bucket id
     */
    private void process(final  StorageTx tx, String groupId, String artifactId, String option, long nbReleasesToPurge, ORID bucketId) {
        //First retrieve the last component of the releases which have been not purged
        String lastComponentVersion = null;
        Date lastReleaseDate = null;
        List<Component> components = retrieveReleases(groupId, artifactId, option, nbReleasesToPurge, bucketId);
        if (VERSION_OPTION.equals(option)) {
            lastComponentVersion = getLastComponentVersion(components);
        } else if (DATE_RELEASE_OPTION.equals(option)) {
            lastReleaseDate = getLastComponentReleaseDate(components);
        }
        //
        int n = 0;

        while (n < nbReleasesToPurge && !isCanceled()) {
            List<Component> filteredComponents = retrieveReleases(groupId,
                    artifactId,
                    option,
                    PAGINATION_LIMIT,
                    lastComponentVersion,
                    lastReleaseDate,
                    "desc",
                    bucketId);
            int totalComponents = filteredComponents.size();
            log.debug("{} components will be purged ", totalComponents);
            lastComponentVersion = getLastComponentVersion(filteredComponents);

            for (Component component : filteredComponents) {
                if (isCanceled()) {
                    break;
                }
                deleteComponent(component);
            }

            tx.commit();
            tx.begin();

            n += totalComponents;
        }
    }

    private List<Component> retrieveReleases(String groupId,
                                             String artifactId,
                                             String option,
                                             long pagination,
                                             String lastComponentVersion,
                                             Date lastReleaseDate,
                                             String order,
                                             ORID bucketId) {
        StorageTx tx = UnitOfWork.currentTx();
        QueryPurgeReleasesBuilder queryPurgeReleasesBuilder = null;
        if (VERSION_OPTION.equals(option)) {
            queryPurgeReleasesBuilder = QueryPurgeReleasesBuilder.buildQueryForVersionOption(bucketId,
                    groupId, artifactId, lastComponentVersion, pagination, order);
        } else if (DATE_RELEASE_OPTION.equals(option)) {
            queryPurgeReleasesBuilder = QueryPurgeReleasesBuilder.buildQueryForReleaseDateOption(bucketId,
                    groupId, artifactId, lastReleaseDate, pagination, order);
        }

        log.debug("Query executed {} ", Objects.requireNonNull(queryPurgeReleasesBuilder).toString());

        Iterable<Component> components = tx.findComponents(queryPurgeReleasesBuilder.getWhereClause(),
                queryPurgeReleasesBuilder.getQueryParams(),
                singletonList(getRepository()),
                queryPurgeReleasesBuilder.getQuerySuffix());
        return Lists.newArrayList(components);

    }

    public List<Component> retrieveReleases(String groupId, String artifactId, String option, long pagination, ORID bucketId) {
        return retrieveReleases(groupId, artifactId, option, pagination, null, null, "asc", bucketId);
    }

    public long countTotalReleases(String groupId, String artifactId, ORID bucketId) {
        long nbComponents;
        StorageTx tx = UnitOfWork.currentTx();
        QueryPurgeReleasesBuilder queryPurgeReleasesBuilder = QueryPurgeReleasesBuilder.buildQueryForCount(bucketId,
                groupId,
                artifactId);
        nbComponents = tx.countComponents(queryPurgeReleasesBuilder.getWhereClause(),
                queryPurgeReleasesBuilder.getQueryParams(),
                singletonList(getRepository()),
                queryPurgeReleasesBuilder.getQuerySuffix());
        log.debug("Total number of releases components for {} {} int the repository {} : {} ", groupId, artifactId, getRepository().getName(), nbComponents);
        return nbComponents;
    }


    @TransactionalDeleteBlob
    private void deleteComponent(final Component component) {
        log.debug("Deleting unused released component {}", component);
        StorageTx tx = UnitOfWork.currentTx();
        MavenFacetUtils.deleteComponent(tx, facet(MavenFacet.class), component);
    }

    public String getLastComponentVersion(List<Component> components) {
        return components.get(components.size() - 1).version();
    }

    private Date getLastComponentReleaseDate(List<Component> components) {
        return components.get(components.size() - 1).lastUpdated().toDate();
    }

    private boolean isCanceled() {
        try {
            CancelableHelper.checkCancellation();
            return false;
        } catch (TaskInterruptedException e) {
            log.warn("Purge unused Maven releases job is canceled");
            return true;
        }
    }

    /**
     * List the count of releases group by groupId/artifactId in a repository
     * @param tx
     * @param bucketId
     * @return
     */
    public List<QueryResultForNumberOfReleases> listNumberOfReleases(final StorageTx tx,
                                                                     ORID bucketId) {
        List<QueryResultForNumberOfReleases> releases = new ArrayList<>();
        long totalComponents = tx.countComponents(Query.builder().where("1").eq(1).build(), Collections.singletonList(getRepository()));
        log.debug("Bucket Id {} , total components {}", bucketId, totalComponents);
        String query = String.format(REQUEST_TOTAL_RELEASES_BY_RELEASE, bucketId,
                totalComponents != 0 ? totalComponents : -1);
        List<ODocument> documentList = tx.getDb().command(new OCommandScript("sql", query)).execute();

        for (ODocument document : documentList) {
            releases.add(new QueryResultForNumberOfReleases(document.field("groupId"),
                    document.field("artifactId"),
                    document.field("count")));
        }
        return releases;

    }
}
