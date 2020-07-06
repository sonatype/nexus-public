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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.npm.internal.audit.exceptions.PackageLockParsingException;
import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLock;
import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLockParser;
import org.sonatype.nexus.repository.npm.internal.audit.report.ReportCreator;
import org.sonatype.nexus.repository.npm.internal.audit.report.ResponseReport;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;
import org.sonatype.nexus.repository.vulnerability.AuditRepositoryComponent;
import org.sonatype.nexus.repository.vulnerability.ComponentValidation;
import org.sonatype.nexus.repository.vulnerability.ComponentsVulnerability;
import org.sonatype.nexus.repository.vulnerability.RepositoryComponentValidation;
import org.sonatype.nexus.repository.vulnerability.Vulnerability;
import org.sonatype.nexus.repository.vulnerability.VulnerabilityList;
import org.sonatype.nexus.repository.vulnerability.exceptions.ConfigurationException;
import org.sonatype.nexus.repository.vulnerability.exceptions.TarballLoadingException;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.sonatype.nexus.repository.npm.internal.audit.model.NpmAuditError.ABSENT_PARSING_FILE;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

/**
 * Facet for processing dependencies from 'npm audit' cmd to analyze them on vulnerabilities by IQ Server.
 *
 * @since 3.24
 */
@Named
@Exposed
public class NpmAuditFacet
    extends FacetSupport
{
  private static final Logger logger = LoggerFactory.getLogger(NpmAuditFacet.class);

  public static final String QUICK_AUDIT_ATTR_NAME = "QUICK_AUDIT";

  private static final String CACHE_NAME = "npm-audit-data";

  // Nexus IQ Hosted Data Services (HDS) update vulnerabilities data every 12 hours.
  private static final Duration CACHE_DURATION = new Duration(TimeUnit.HOURS, 12);

  // npm audit timeout in sec to wait for a response
  private final int timeout;

  private final EventManager eventManager;

  private final ReportCreator reportCreator;

  private NpmAuditTarballFacet npmAuditTarballFacet;

  private final CacheHelper cacheHelper;

  private Cache<AuditComponent, VulnerabilityList> cache;

  private final Gson gson =
      new GsonBuilder()
          .serializeNulls()
          .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
          .create();

  @Inject
  public NpmAuditFacet(
      @Named("${nexus.npm.audit.timeout:-600}") final int timeout,
      final EventManager eventManager,
      final ReportCreator reportCreator,
      final CacheHelper cacheHelper)
  {
    checkArgument(timeout > 0, "nexus.npm.audit.timeout must be greater than 0");
    this.timeout = timeout;
    this.eventManager = checkNotNull(eventManager);
    this.reportCreator = checkNotNull(reportCreator);
    this.cacheHelper = checkNotNull(cacheHelper);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.npmAuditTarballFacet = facet(NpmAuditTarballFacet.class);
    maybeCreateCache();
  }

  @Override
  protected void doDelete() {
    maybeDestroyCache();
  }

  public Payload audit(final Payload payload)
      throws ExecutionException, ConfigurationException, PackageLockParsingException, TarballLoadingException
  {
    if (payload == null) {
      throw new ConfigurationException(ABSENT_PARSING_FILE.getMessage());
    }

    PackageLock packageLock = parseRequest(payload);
    ComponentsVulnerability componentsVulnerability =
        analyzeComponents(packageLock.getComponents(), packageLock.getRoot().getApplicationId());
    return buildResponse(packageLock, componentsVulnerability);
  }

  private static PackageLock parseRequest(final Payload payload) throws PackageLockParsingException {
    try (GZIPInputStream stream = new GZIPInputStream(payload.openInputStream(), (int) payload.getSize())) {
      return PackageLockParser.parse(IOUtils.toString(stream));
    }
    catch (IOException e) {
      logger.warn(e.getMessage(), e);
      throw new PackageLockParsingException();
    }
  }

  private ComponentsVulnerability analyzeComponents(
      final Set<AuditComponent> componentsToAnalyze,
      final String applicationId) throws ExecutionException, TarballLoadingException
  {
    ComponentsVulnerability componentsVulnerability = new ComponentsVulnerability();
    if (!componentsToAnalyze.isEmpty()) {
      if (log.isTraceEnabled()) {
        log.trace("Handle {} npm components to analyze", componentsToAnalyze.size());
        log.trace("Evaluating components: {}", componentsToAnalyze);
      }

      // get vulnerabilities from the cache
      for (AuditComponent auditComponent : componentsToAnalyze) {
        VulnerabilityList componentVulnerability = cache.get(auditComponent);
        if (componentVulnerability != null) {
          List<Vulnerability> vulnerabilities = componentVulnerability.getVulnerabilities();
          componentsVulnerability.addVulnerabilities(auditComponent, vulnerabilities);
        }
      }

      // get vulnerabilities from the IQ Server
      Set<AuditComponent> foundAuditComponents = componentsVulnerability.getAuditComponents().keySet();
      Set<AuditComponent> auditComponents = componentsToAnalyze.stream()
          .filter(c -> !foundAuditComponents.contains(c))
          .collect(Collectors.toSet());
      if (!auditComponents.isEmpty()) {
        ComponentsVulnerability vulnerabilities;
        try {
          if (applicationId != null) {
            vulnerabilities = getComponentsVulnerabilityFromRemoteServer(auditComponents, applicationId);
          }
          else {
            vulnerabilities = getComponentsVulnerabilityFromRemoteServer(auditComponents);
          }
          componentsVulnerability.addComponentsVulnerabilities(vulnerabilities);
          vulnerabilities.getAuditComponents().forEach((auditComponent, componentVulnerabilities) ->
              cache.put(auditComponent, new VulnerabilityList(componentVulnerabilities)));

          // components without vulnerabilities also should be added to the cache
          Set<AuditComponent> componentsWithVulnerabilities = componentsVulnerability.getAuditComponents().keySet();
          componentsToAnalyze.stream()
              .filter(c -> !componentsWithVulnerabilities.contains(c))
              .forEach(component -> cache.put(component, new VulnerabilityList()));
        }
        catch (InterruptedException | TimeoutException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return componentsVulnerability;
  }

  private ComponentsVulnerability getComponentsVulnerabilityFromRemoteServer(
      final Set<AuditComponent> componentsToAnalyze)
      throws InterruptedException, ExecutionException, TimeoutException, TarballLoadingException
  {
    Set<AuditRepositoryComponent> repositoryComponents = getAuditRepositoryComponents(componentsToAnalyze);
    RepositoryComponentValidation componentValidation = new RepositoryComponentValidation(
        getRepository().getName(), repositoryComponents);
    /* post repository npm components to IQ Server
     * see com.sonatype.nexus.clm.vulnerability.RepositoryComponentVulnerabilityListener */
    eventManager.post(componentValidation);

    return getVulnerabilityResult(componentValidation.getVulnerabilityResult());
  }

  private ComponentsVulnerability getComponentsVulnerabilityFromRemoteServer(
      final Set<AuditComponent> componentsToAnalyze,
      final String applicationId)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    ComponentValidation componentValidation = new ComponentValidation(componentsToAnalyze, applicationId);
    /* post npm components to IQ Server with app id
     * see com.sonatype.nexus.clm.violation.ComponentVulnerabilityListener */
    eventManager.post(componentValidation);

    return getVulnerabilityResult(componentValidation.getVulnerabilityResult());
  }

  private Set<AuditRepositoryComponent> getAuditRepositoryComponents(final Set<AuditComponent> componentsToAnalyze)
      throws TarballLoadingException
  {
    Stopwatch sw = Stopwatch.createStarted();
    Set<AuditRepositoryComponent> repositoryComponents = npmAuditTarballFacet.download(componentsToAnalyze);
    log.debug("Downloaded {} npm packages in {}", repositoryComponents.size(), sw.stop());
    return repositoryComponents;
  }

  private ComponentsVulnerability getVulnerabilityResult(final CompletableFuture<ComponentsVulnerability> future)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    ComponentsVulnerability componentsVulnerability = future.get(timeout, SECONDS);
    if (log.isTraceEnabled()) {
      log.trace("Report: {}", gson.toJson(componentsVulnerability.getAuditComponents()));
    }

    return componentsVulnerability;
  }

  private StringPayload buildResponse(
      final PackageLock packageLock,
      final ComponentsVulnerability componentsVulnerability)
  {
    ResponseReport responseReport = reportCreator.buildResponseReport(componentsVulnerability, packageLock);
    String responseReportString = gson.toJson(responseReport);
    log.trace("npm audit report: {}", responseReportString);
    log.debug("Build report with metadata: {}", responseReport.getMetadata());
    return new StringPayload(responseReportString, APPLICATION_JSON);
  }

  private void maybeCreateCache() {
    if (cache == null) {
      log.debug("Creating {} for: {}", CACHE_NAME, getRepository());
      cache = cacheHelper.maybeCreateCache(CACHE_NAME, AuditComponent.class, VulnerabilityList.class,
          CreatedExpiryPolicy.factoryOf(CACHE_DURATION));
      log.debug("Created {}: {}", CACHE_NAME, cache);
    }
  }

  private void maybeDestroyCache() {
    if (cache != null) {
      log.debug("Destroying {} for: {}", CACHE_NAME, getRepository());
      cacheHelper.maybeDestroyCache(CACHE_NAME);
      cache = null;
    }
  }
}
