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
package org.sonatype.nexus.repository.replication;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class ReplicationIngesterFactory extends LifecycleSupport {
    private final Map<String, ReplicationIngester> ingesters;

    @Inject
    public ReplicationIngesterFactory(final Map<String, ReplicationIngester> ingesters) {
        this.ingesters = requireNonNull(ingesters);
    }

    @Nullable
    public ReplicationIngester find(String format) {
        return ingesters.get(format);
    }
}
