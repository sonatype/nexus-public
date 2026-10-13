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

/**
 * Shared types used across the Nexus API layers (ExtDirect and REST), so both layers can depend on a single shared
 * copy instead of duplicating it per distribution. This comprises:
 * <ul>
 * <li>exchange-object (XO) / DTO model types; and</li>
 * <li>the Rapture {@code StateContributor} implementations shared by the coreui and cloudui distributions
 * (see the {@code state} sub-packages).</li>
 * </ul>
 *
 * <p>
 * Beyond those two categories the module is intended to stay lightweight: anything requiring heavier service or
 * endpoint infrastructure (services, resources, endpoints) belongs in a dedicated API module. New Spring components
 * added here should be justified against that intent (enforcing this via an ArchUnit/Checkstyle rule is tracked as a
 * follow-up).
 */
package org.sonatype.nexus.api.common.shared;
