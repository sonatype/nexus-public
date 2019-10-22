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

import org.sonatype.nexus.repository.conda.internal.CondaFormat
import org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.conda.internal.CondaRecipeSupport
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Matcher
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import static org.sonatype.nexus.repository.http.HttpMethods.*

/**
 * Conda Hosted Recipe
 */
@Named(CondaHostedRecipe.NAME)
@Singleton
class CondaHostedRecipe
        extends CondaRecipeSupport {
    public static final String NAME = 'conda-hosted'

    @Inject
    Provider<CondaHostedFacetImpl> hostedFacet

    @Inject
    CondaHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(CondaFormat.NAME) final Format format) {
        super(type, format)
    }

    @Override
    void apply(@Nonnull final Repository repository) throws Exception {
        repository.attach(securityFacet.get())
        repository.attach(configure(viewFacet.get()))
        repository.attach(httpClientFacet.get())
        repository.attach(componentMaintenanceFacet.get())
        repository.attach(storageFacet.get())
        repository.attach(hostedFacet.get())
        repository.attach(searchFacet.get())
        repository.attach(attributesFacet.get())
    }

    /**
     * Match conda metadata files
     */
    static Matcher condaMetaDataMather = {
        Context context ->
            def path = context.getRequest().getPath()
            log.warn("Searching condaMetaDataMather " + path)
            String candidate = path.substring(path.lastIndexOf('/'))
            CondaPathUtils.CONDA_META_DATA.contains(candidate)
    }

    /**
     * Conda path matcher - verifies path has conda like structure
     */
    static Matcher condaPathMatcher = {
        Context context ->
            // TODO: fix logging, use try monad
            try {
                final String path = context.getRequest().getPath()
                log.warn("Searching condaPathMatcher " + path)
                CondaPath.build(path)
            }
            catch (Exception ex) {
                false
            }
            true
    }

    static Matcher fetchMetaDataMatcher = LogicMatchers.and(new ActionMatcher(GET, HEAD), condaMetaDataMather)
    static Matcher fetchCondaFileMatcher = LogicMatchers.and(new ActionMatcher(GET, HEAD), condaPathMatcher)
    static Matcher uploadCondaFileMatcher = LogicMatchers.and(new ActionMatcher(PUT), condaPathMatcher)
    static Matcher deleteCondaFileMatcher = LogicMatchers.and(new ActionMatcher(DELETE), condaPathMatcher)

    /**
     * Configure {@link org.sonatype.nexus.repository.view.ViewFacet}.
     */
    private ViewFacet configure(final ConfigurableViewFacet facet) {

        Router.Builder builder = new Router.Builder()
        [fetchCondaFileMatcher, fetchMetaDataMatcher, uploadCondaFileMatcher, deleteCondaFileMatcher].each { matcher ->
            builder.route(new Route.Builder().matcher(matcher)
                    .handler(timingHandler)
                    .handler(securityHandler)
                    .handler(exceptionHandler)
                    .handler(handlerContributor)
                    .handler(partialFetchHandler)
                    .handler(contentHeadersHandler)
                    .handler(unitOfWorkHandler)
                    .handler(HandlerProvider.handler)
                    .create())
        }

        addBrowseUnsupportedRoute(builder)

        builder.defaultHandlers(HttpHandlers.notFound())
        facet.configure(builder.create())
        return facet
    }

}
