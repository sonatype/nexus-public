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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLock;
import org.sonatype.nexus.repository.npm.internal.audit.parser.PackageLockNode;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;
import org.sonatype.nexus.repository.vulnerability.ComponentsVulnerability;
import org.sonatype.nexus.repository.vulnerability.Vulnerability;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Class which accept report from IQ server and result of parsing package-lock.json and generate npm report for npm
 * audit command
 *
 * @since 3.24
 */
@Named
@Singleton
public class ReportCreator
{
  private static final String N_A = "N/A";

  private static final String APP_ID_ADVICE = "Please specify APP_ID in package-lock.json"
      + lineSeparator() + "https://links.sonatype.com/npm-audit";

  private static final Logger log = LoggerFactory.getLogger(ReportCreator.class);

  public ResponseReport buildResponseReport(
      final ComponentsVulnerability report,
      final PackageLock packageLock)
  {
    List<Action> actions = new ArrayList<>();
    Map<String, Advisory> advisories = new HashMap<>();
    List<Object> muted = new ArrayList<>();

    int index = 1;
    for (Entry<AuditComponent, List<Vulnerability>> entry : report.getAuditComponents().entrySet()) {
      AuditComponent component = entry.getKey();
      List<Vulnerability> vulnerabilities = entry.getValue();
      // from the critical issues to the lowest ones
      vulnerabilities.sort(Comparator.comparing(Vulnerability::getSeverity,
          Comparator.comparingInt(v -> v.getSeverityRange().getMinimum())).reversed());
      for (Vulnerability vulnerability : vulnerabilities) {
        List<Resolve> resolvesForOneComponent =
            packageLock.createResolve(index, component.getName(), component.getVersion());
        if (resolvesForOneComponent.isEmpty()) {
          log.warn("NPM component not found: {}", component.getName() + "-" + component.getVersion());
        }

        String patchedVersion =
            StringUtils.isBlank(vulnerability.getPatchedIn()) ? N_A : vulnerability.getPatchedIn();
        boolean isAppIdAvailable = isNotBlank(packageLock.getRoot().getApplicationId());

        List<String> paths = resolvesForOneComponent.stream()
            .map(Resolve::getPath)
            .collect(toList());
        Advisory advisory = createAdvisory(index, component, vulnerability, patchedVersion, isAppIdAvailable);
        advisories.put(String.valueOf(index), advisory);
        resolvesForOneComponent.forEach(resolve -> actions.add(createAction(component, resolve, patchedVersion)));
        Set<Finding> findings = advisory.getFindings();
        Finding finding = new Finding(component.getVersion(), paths);
        findings.add(finding);
        index++;
      }
    }

    VulnerabilityReport vulnerabilityReport = createVulnerabilityReport(new ArrayList<>(advisories.values()));
    Metadata metadata = createMetadata(vulnerabilityReport, packageLock);
    return new ResponseReport(actions, advisories, muted, metadata);
  }

  private Metadata createMetadata(final VulnerabilityReport vulnerabilityReport, final PackageLock packageLock) {
    int dependencies = 0;
    int devDependencies = 0;
    int optionalDependencies = 0;
    for (AuditComponent component : packageLock.getComponents()) {
      for (PackageLockNode node : packageLock.getNodes(component.getName(), component.getVersion())) {
        if (!node.isDev()) {
          dependencies++;
        }
        else {
          devDependencies++;
        }
        if (node.isOptional()) {
          optionalDependencies++;
        }
      }
    }

    return new Metadata(vulnerabilityReport, dependencies, devDependencies, optionalDependencies,
        dependencies + devDependencies);
  }

  private Action createAction(
      final AuditComponent auditComponent,
      final Resolve resolve,
      final String patchedVersion)
  {
    ActionType actionType = getActionType(resolve, patchedVersion);
    return new Action(false, actionType.getType(), singletonList(resolve), auditComponent.getName(),
        patchedVersion, resolve.getPathList().size());
  }

  private VulnerabilityReport createVulnerabilityReport(final List<Advisory> advisories) {
    int low = 0;
    int moderate = 0;
    int high = 0;
    int critical = 0;
    for (Advisory advisory : advisories) {
      switch (advisory.getSeverityLevel()) {
        case LOW:
          low += getAdvisoryCount(advisory);
          break;
        case MODERATE:
          moderate += getAdvisoryCount(advisory);
          break;
        case HIGH:
          high += getAdvisoryCount(advisory);
          break;
        case CRITICAL:
          critical += getAdvisoryCount(advisory);
          break;
        default:
          throw new IllegalStateException("Unsupported enum for " + advisory.getSeverityLevel());
      }
    }

    return new VulnerabilityReport(0, low, moderate, high, critical);
  }

  private int getAdvisoryCount(final Advisory advisory) {
    return advisory.getFindings().stream().map(a -> a.getPaths().size()).reduce(0, Integer::sum);
  }

  private Advisory createAdvisory(
      final int index,
      final AuditComponent auditComponent,
      final Vulnerability vulnerability,
      final String patchedVersion,
      final boolean isAppIdAvailable)
  {
    String patchedVersionAdvisory = !isAppIdAvailable && N_A.equals(patchedVersion) ? APP_ID_ADVICE : patchedVersion;
    return new Advisory(newHashSet(), index, vulnerability.getDescription(), auditComponent.getName(),
        patchedVersionAdvisory, vulnerability.getDescription(), vulnerability.getSeverity(),
        vulnerability.getSeverity().getNpmLevel(), vulnerability.getMoreInfo());
  }

  private ActionType getActionType(final Resolve resolve, final String patchedVersion) {
    ActionType actionType;
    if (N_A.equals(patchedVersion)) {
      actionType = ActionType.REVIEW;
    }
    else if (resolve.isDev()) {
      actionType = ActionType.UPDATE;
    }
    else {
      actionType = ActionType.INSTALL;
    }
    return actionType;
  }
}
