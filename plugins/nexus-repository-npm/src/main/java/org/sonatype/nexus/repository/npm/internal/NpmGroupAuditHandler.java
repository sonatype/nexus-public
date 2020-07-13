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
package org.sonatype.nexus.repository.npm.internal;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.orient.OrientNpmGroupHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.npm.internal.NpmAuditFacet.QUICK_AUDIT_ATTR_NAME;

/**
 * Handle 'npm audit' cmd in the group repository.
 *
 * @since 3.24
 */
@Named
@Singleton
public class NpmGroupAuditHandler
    extends OrientNpmGroupHandler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception
  {
    NpmAuditFacet npmAuditFacet = context.getRepository().facet(NpmAuditFacet.class);
    context.getAttributes().set(QUICK_AUDIT_ATTR_NAME, false);
    return NpmResponses.ok(npmAuditFacet.audit(context.getRequest().getPayload()));
  }
}
