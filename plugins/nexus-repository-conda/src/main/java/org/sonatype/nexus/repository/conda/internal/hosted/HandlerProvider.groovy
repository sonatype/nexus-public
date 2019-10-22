package org.sonatype.nexus.repository.conda.internal.hosted

import org.sonatype.nexus.repository.view.*

import javax.annotation.Nonnull

/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

class HandlerProvider {
    static String removeLeadingSlash(String path) {
        path.length() > 1 && path[0] == '/' ? path.substring(1) : path
    }

    static Handler handler = new Handler() {
        @Override
        Response handle(@Nonnull Context context) throws Exception {
            String method = context.getRequest().getAction()
            String path = removeLeadingSlash(context.getRequest().getPath())

            CondaHostedFacet condaHostedFacet = context
                    .getRepository()
                    .facet(CondaHostedFacet.class)

            switch (method) {
                case org.sonatype.nexus.repository.http.HttpMethods.GET:
                case org.sonatype.nexus.repository.http.HttpMethods.HEAD:
                    return condaHostedFacet
                            .fetch(path)
                            .map({ Content content -> org.sonatype.nexus.repository.http.HttpResponses.ok(content) })
                            .orElseGet({ org.sonatype.nexus.repository.http.HttpResponses.notFound() })
                case org.sonatype.nexus.repository.http.HttpMethods.PUT:
                    Payload payload = context.getRequest().getPayload()
                    return org.sonatype.nexus.repository.http.HttpResponses.ok(condaHostedFacet.upload(path, payload))
                case org.sonatype.nexus.repository.http.HttpMethods.DELETE:
                    def success = condaHostedFacet.delete(path)
                    return success ? org.sonatype.nexus.repository.http.HttpResponses.ok() : org.sonatype.nexus.repository.http.HttpResponses.notFound()
                default:
                    return org.sonatype.nexus.repository.http.HttpResponses.methodNotAllowed(context.getRequest().getAction(), org.sonatype.nexus.repository.http.HttpMethods.GET, org.sonatype.nexus.repository.http.HttpMethods.HEAD, org.sonatype.nexus.repository.http.HttpMethods.PUT, org.sonatype.nexus.repository.http.HttpMethods.DELETE)
            }
        }
    }
}
