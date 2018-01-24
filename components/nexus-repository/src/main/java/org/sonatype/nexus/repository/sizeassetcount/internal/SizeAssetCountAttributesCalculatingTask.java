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
package org.sonatype.nexus.repository.sizeassetcount.internal;


import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.sizeassetcount.SizeAssetCountAttributesFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

/**
 * Task that calculates the size and the asset count of a repository
 *
 * @since 3.7.0
 */
@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class SizeAssetCountAttributesCalculatingTask extends RepositoryTaskSupport
        implements Cancelable
{

    public static final String PREFIX_MESSAGE = "Calculate the size and the asset count of the repository ";
    private final Type hostedType;

    @Inject
    public SizeAssetCountAttributesCalculatingTask(@Named(HostedType.NAME) final Type hostedType) {
        this.hostedType = checkNotNull(hostedType);
    }


    @Override
    public String getMessage() {
        return PREFIX_MESSAGE + getRepositoryField();
    }


    @Override
    protected void execute(Repository repository) {
        repository.facet(SizeAssetCountAttributesFacet.class).calculateSizeAssetCount();

    }

    @Override
    protected boolean appliesTo(Repository repository) {
        return hostedType.equals(repository.getType());
    }
}
