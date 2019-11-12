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
package org.sonatype.nexus.repository.conda.internal.hosted

import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.HttpResponses
import org.sonatype.nexus.repository.view.*
import javax.annotation.Nonnull

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
                case HttpMethods.GET:
                case HttpMethods.HEAD:
                    return condaHostedFacet
                            .fetch(path)
                            .map({ Content content -> HttpResponses.ok(content) })
                            .orElseGet({ HttpResponses.notFound() })
                case HttpMethods.PUT:
                    Payload payload = context.getRequest().getPayload()
                    return HttpResponses.ok(condaHostedFacet.upload(path, payload))
                case HttpMethods.DELETE:
                    def success = condaHostedFacet.delete(path)
                    return success ? HttpResponses.ok() : HttpResponses.notFound()
                default:
                    return HttpResponses.methodNotAllowed(context.getRequest().getAction(), HttpMethods.GET, HttpMethods.HEAD, HttpMethods.PUT, HttpMethods.DELETE)
            }
        }
    }
}
