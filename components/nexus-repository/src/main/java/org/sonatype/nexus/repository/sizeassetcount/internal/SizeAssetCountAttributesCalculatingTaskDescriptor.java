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

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.sizeassetcount.SizeAssetCountAttributesFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;

/**
 * @since 3.7.0
 */
@Named
@Singleton
public class SizeAssetCountAttributesCalculatingTaskDescriptor extends TaskDescriptorSupport
{
    public static final String TYPE_ID = "repository.calculate-size-assetcount";

    @Inject
    public SizeAssetCountAttributesCalculatingTaskDescriptor() {
        super(TYPE_ID,
                SizeAssetCountAttributesCalculatingTask.class,
                " Calculate the size and the asset count of a repository",
                VISIBLE,
                EXPOSED,
                new RepositoryCombobox(
                        REPOSITORY_NAME_FIELD_ID,
                        "Repository",
                        "Select the repository which you will calculate the size and the asset count",
                        FormField.MANDATORY
                ).includingAnyOfFacets(SizeAssetCountAttributesFacet.class).includingAnyOfTypes(HostedType.NAME).includeAnEntryForAllRepositories()
        );
    }
}
