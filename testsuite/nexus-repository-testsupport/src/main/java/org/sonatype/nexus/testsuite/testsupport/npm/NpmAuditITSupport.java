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
package org.sonatype.nexus.testsuite.testsupport.npm;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.vulnerability.AuditComponent;
import org.sonatype.nexus.repository.vulnerability.AuditRepositoryComponent;
import org.sonatype.nexus.repository.vulnerability.ComponentsVulnerability;
import org.sonatype.nexus.repository.vulnerability.RepositoryComponentValidation;
import org.sonatype.nexus.repository.vulnerability.Vulnerability;

import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.repository.vulnerability.SeverityLevel.CRITICAL;
import static org.sonatype.nexus.repository.vulnerability.SeverityLevel.HIGH;
import static org.sonatype.nexus.repository.vulnerability.SeverityLevel.LOW;
import static org.sonatype.nexus.repository.vulnerability.SeverityLevel.MODERATE;

/**
 * Support for npm audit ITs.
 */
public abstract class NpmAuditITSupport
    extends NpmClientITSupport
{
  protected static final String PACKAGE_LOCK_FILE_NAME = "package-lock-template.json";

  protected static final String PACKAGE_FILE_NAME = "package-template.json";

  protected static final String NPM_REGISTRY_URL = "http://registry.npmjs.org";

  protected static final String USER_ERROR_MSG = "Error fetching npm audit data";

  // mock the com.sonatype.nexus.clm.vulnerability.RepositoryComponentVulnerabilityListener without vulnerabilities
  public static class VulnerabilityListenerNoViolation
      implements EventAware.Asynchronous
  {
    @Subscribe
    public void on(final RepositoryComponentValidation request) {
      ComponentsVulnerability componentsVulnerability = new ComponentsVulnerability();
      request.getVulnerabilityResult().complete(componentsVulnerability);
    }
  }

  // mock the com.sonatype.nexus.clm.vulnerability.RepositoryComponentVulnerabilityListener with vulnerabilities
  public static class VulnerabilityListener
      implements EventAware.Asynchronous
  {
    @Subscribe
    public void on(final RepositoryComponentValidation request) {
      ComponentsVulnerability componentsVulnerability = new ComponentsVulnerability();
      String url = "www.example.org";
      for (AuditRepositoryComponent repoAuditComponent : request.getRepositoryComponents()) {
        // add all types of issues.
        Vulnerability critical = new Vulnerability(CRITICAL, "Vulnerable component", null, url);
        Vulnerability high = new Vulnerability(HIGH, "Security issue", null, url);
        Vulnerability moderate = new Vulnerability(MODERATE, "Moderate issue", null, url);
        Vulnerability low = new Vulnerability(LOW, "Low issue", null, url);

        AuditComponent auditComponent = buildAuditComponent(repoAuditComponent);
        componentsVulnerability.addVulnerability(auditComponent, critical);
        componentsVulnerability.addVulnerability(auditComponent, high);
        componentsVulnerability.addVulnerability(auditComponent, moderate);
        componentsVulnerability.addVulnerability(auditComponent, low);
      }
      request.getVulnerabilityResult().complete(componentsVulnerability);
    }

    private static AuditComponent buildAuditComponent(AuditRepositoryComponent repoAuditComponent) {
      /*
       * For example, from the pathname: @type/name/-/name-1.0.0.tgz
       * npmPackageName: @type/name and npmPackageVersion: 1.0.0
       */
      String pathname = repoAuditComponent.getPathname();
      String[] npmPackageParts = pathname.split("/-/");
      String npmPackageName = npmPackageParts.length > 0 ? npmPackageParts[0] : pathname;
      String npmPackageVersion = pathname.replaceAll(".*?((?<!\\w)\\d+([.]\\d+)*).tgz", "$1");
      return new AuditComponent(null, repoAuditComponent.getFormat(), npmPackageName, npmPackageVersion);
    }
  }
}
