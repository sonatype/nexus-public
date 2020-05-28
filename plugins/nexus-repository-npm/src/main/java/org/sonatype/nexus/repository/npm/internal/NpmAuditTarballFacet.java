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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;
import org.sonatype.nexus.repository.vulnerability.AuditRepositoryComponent;
import org.sonatype.nexus.repository.vulnerability.exceptions.TarballLoadingException;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget.TARBALL;
import static org.sonatype.nexus.repository.npm.internal.orient.OrientNpmRecipeSupport.tarballMatcher;
import static org.sonatype.nexus.repository.view.Content.CONTENT_HASH_CODES_MAP;
import static org.sonatype.nexus.repository.view.Content.T_CONTENT_HASH_CODES_MAP;

/**
 * Facet for saving/fetching npm package tarballs.
 *
 * @since 3.24
 */
@Named
@Exposed
public class NpmAuditTarballFacet
    extends FacetSupport
{
  private final int maxConcurrentRequests;

  private final TarballGroupHandler tarballGroupHandler;

  private ExecutorService executor;

  @Inject
  public NpmAuditTarballFacet(
      @Named("${nexus.npm.audit.maxConcurrentRequests:-10}") final int maxConcurrentRequests)
  {
    checkArgument(maxConcurrentRequests > 0, "nexus.npm.audit.maxConcurrentRequests must be greater than 0");
    this.maxConcurrentRequests = maxConcurrentRequests;
    this.tarballGroupHandler = new TarballGroupHandler();
  }

  @Override
  protected void doStart() {
    executor = NexusExecutorService.forCurrentSubject(Executors.newFixedThreadPool(
        maxConcurrentRequests, new NexusThreadFactory("npm-audit-tasks", "npm-audit")));
  }

  @Override
  protected void doStop() throws InterruptedException {
    executor.shutdown();
    if (!executor.awaitTermination(10, SECONDS)) {
      log.warn("Failed to terminate thread pool in allotted time");
    }
    executor = null;
  }

  public Set<AuditRepositoryComponent> download(final Set<AuditComponent> auditComponents)
      throws TarballLoadingException
  {
    List<Callable<AuditRepositoryComponent>> tasks = new ArrayList<>();
    auditComponents.forEach(auditComponent -> tasks.add(() -> download(auditComponent)));
    // submit task to execute
    List<Future<AuditRepositoryComponent>> repositoryComponentFutures = tasks.stream()
        .map(executor::submit)
        .collect(toList());

    Set<AuditRepositoryComponent> repositoryComponents = new HashSet<>(auditComponents.size());
    try {
      for (Future<AuditRepositoryComponent> repositoryComponentFuture : repositoryComponentFutures) {
        repositoryComponents.add(repositoryComponentFuture.get());
      }
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TarballLoadingException) {
        throw new TarballLoadingException(cause.getMessage());
      }
      else {
        throw new RuntimeException(e);
      }
    }
    return repositoryComponents;
  }

  private AuditRepositoryComponent download(final AuditComponent auditComponent) throws TarballLoadingException {
    checkNotNull(auditComponent);
    String packageName = auditComponent.getName();
    String packageVersion = auditComponent.getVersion();
    String repositoryPath = NpmMetadataUtils.createRepositoryPath(packageName, packageVersion);
    final Request request = new Request.Builder()
        .action(GET)
        .path("/" + repositoryPath)
        .build();
    Repository repository = getRepository();
    Context context = new Context(repository, request);
    Matcher tarballMatcher = tarballMatcher(GET)
        .handler(new EmptyHandler()).create().getMatcher();
    tarballMatcher.matches(context);
    context.getAttributes().set(ProxyTarget.class, TARBALL);

    Optional<String> hashsumOpt;
    String repositoryType = repository.getType().getValue();
    if (repositoryType.equals(GroupType.NAME)) {
      hashsumOpt = tarballGroupHandler.getTarballHashsum(context);
    }
    else if (repositoryType.equals(ProxyType.NAME)) {
      hashsumOpt = getComponentHashsumForProxyRepo(repository, context);
    }
    else {
      // for an npm-hosted repository is no way to get npm package hashsum info.
      String errorMsg = String.format("The %s repository is not supported", repositoryType);
      throw new UnsupportedOperationException(errorMsg);
    }
    String hashsum = hashsumOpt.orElseThrow(() ->
        new TarballLoadingException(String.format("Can't get hashsum for the %s package", auditComponent)));
    return new AuditRepositoryComponent(auditComponent.getPackageType(), repositoryPath, hashsum);
  }

  private Optional<String> getComponentHashsumForProxyRepo(final Repository repository, final Context context)
      throws TarballLoadingException
  {
    try {
      UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
      Content content = repository.facet(ProxyFacet.class).get(context);
      if (content != null) {
        return getHashsum(content.getAttributes());
      }
    }
    catch (IOException e) {
      throw new TarballLoadingException(e.getMessage());
    }
    finally {
      UnitOfWork.end();
    }

    return Optional.empty();
  }

  private static Optional<String> getHashsum(final AttributesMap attributes) {
    Map<HashAlgorithm, HashCode> hashMap = attributes.get(CONTENT_HASH_CODES_MAP, T_CONTENT_HASH_CODES_MAP);
    if (hashMap != null) {
      HashCode hashCode = hashMap.get(SHA1);
      return Optional.ofNullable(hashCode.toString());
    }
    return Optional.empty();
  }

  /**
   * This handler should do nothing cause it would never be used.
   */
  private static final class EmptyHandler
      implements Handler
  {
    @Nonnull
    @Override
    public Response handle(@Nonnull final Context context) throws Exception
    {
      return context.proceed();
    }
  }

  /**
   * This handler is used to download and get npm package tarballs for a group repository.
   */
  private static final class TarballGroupHandler
      extends GroupHandler
  {
    public Optional<String> getTarballHashsum(final Context context) throws TarballLoadingException {
      DispatchedRepositories dispatched = context.getRequest().getAttributes()
          .getOrCreate(DispatchedRepositories.class);
      try {
        Response response = super.doGet(context, dispatched);
        if (response.getPayload() instanceof Content) {
          Content content = (Content) response.getPayload();
          return getHashsum(content.getAttributes());
        }
      }
      catch (Exception e) {
        throw new TarballLoadingException(e.getMessage());
      }

      return Optional.empty();
    }
  }
}
