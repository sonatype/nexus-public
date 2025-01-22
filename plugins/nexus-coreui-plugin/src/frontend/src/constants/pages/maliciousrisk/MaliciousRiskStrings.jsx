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
import {NxTextLink} from "@sonatype/react-shared-components";
import React from "react";

export default {
  MALICIOUS_RISK: {
    MENU: {
      text: 'Malware Risk',
      textComplement: '<span class="nxrm-new-tag">NEW</span>',
      description: 'Visualize risk in your repositories'
    },
    TITLE: 'Malware Risk',
    DESCRIPTION: 'Open source malware is cached in the proxy repositories on your Nexus Repository.',
    COMPONENTS_IN_HIGH_RISK_ECOSYSTEMS: {
      TEXT: 'Open Source Malware in High Risk Ecosystems',
      REPOSITORIES_PROTECTED: '0 repositories protected',
      PUBLIC_MALICIOUS_COMPONENT: 'Public malicious components',
      TOOLTIP: 'Total amount of malicious components found across this ecosystem’s public repositories'
    },
    OPEN_SOURCE_MALWARE_PROTECTION_STATUS: 'Open Source Malware Protection Status',
    COMPONENT_MALWARE: {
      MALICIOUS_COMPONENTS: {
        TEXT: 'What Is Open Source Malware?',
        DESCRIPTION: <>
          Open Source malware exploits the open source DevOps tool chain to introduce malware such as
          <strong> credential harvesting, data exfiltration, backdoor, file system corruption, etc.</strong>
        </>
      },
      AVERAGE_ATTACK: {
        TEXT: 'Attacks Are on a Sharp Rise',
        DESCRIPTION: '700%',
        SUB_TEXT: 'year-over-year increase in OSS malware'
      },
      LEARN_MORE: {
        TEXT: 'Learn More',
        URL: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/press-releases'
      }
    },
    MALICIOUS_EVENTS: {
      UNPROTECTED_MALWARE: {
        TEXT: 'Malware Components in Public Component Repositories',
        DESCRIPTION: <>
          identified by Sonatype in npmjs.org, PyPI.org and <NxTextLink
            href="https://links.sonatype.com/nexus-repository-firewall/malicious-risk/language-and-package-support"
            external>more</NxTextLink>
        </>,
      },
      PROXY_PROTECTION: {
        TITLE: 'Proxy Repository Protection',
        DESCRIPTION: 'Proxy repositories protected',
        TOOLTIP: 'Your total number of proxied repositories that are protected from malicious components',
      },
      HOW_TO_PROTECT: {
        TEXT: 'How can I protect my repositories?',
        URL: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall'
      }
    },
    RISK_ON_DISK: {
      TITLE_PLURAL: 'Malware Components Found',
      TITLE_SINGULAR: 'Malware Component Found',
      DESCRIPTION: {
        TITLE: <><strong>Open source malware is cached in the proxy repositories of this instance of Nexus Repository Manager</strong></>,
        CONTENT: 'Open source malware is cached in the proxy repositories on your Nexus Repository, review the components flagged by Sonatype as containing malware and remove them.',
        ADDITIONAL_NON_ADMIN_CONTENT: <><strong>Contact your instance administrator to resolve.</strong></>
      },
      CONTACT_SONATYPE: {
        TEXT: 'Contact Sonatype to Resolve',
        URL: {
          OSS: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/oss-admin-learn-more',
          PRO: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/pro-admin-learn-more'
        }
      },
      VIEW_MALWARE_RISK: 'View Malware Risk'
    },
    LOAD_ERROR: 'An error occurred while fetching the malicious risk data',
    HDS_CONNECTION_WARNING: <>
      OSS Malware Risk data relies on backend services that are currently unreachable. To view malware risk, <NxTextLink
        href="https://links.sonatype.com/nexus-repository-firewall/malicious-risk/repository-health-check-overview"
        external>ensure the required URLs are accessible</NxTextLink>
    </>,
    MALWARE_REMEDIATION: {
      DESCRIPTION: 'Sonatype has identified malware components in your repository. Use the CSV below to identify which components are malicious and remediate.',
      REMEDIATION_STEPS: {
        TITLE: 'Steps to Identify and Address Malware',
        FIRST: <>Create and run Automatic Malware Management tasks on your proxy repositories. <NxTextLink
          href="https://links.sonatype.com/products/nxrm3/docs/scheduled-task" className="scheduled-task-link" external>
          Learn about maintenance tasks</NxTextLink></>,
        SECOND: 'Download the CSV file using the link below to review the components flagged by Sonatype as containing malware.',
        THIRD: <>Search your proxy repository to remove the components. <NxTextLink
          href="https://links.sonatype.com/nexus-repository-firewall/malware-risk/guide-to-removing-malware"
          className="guide-to-removing-malware-link" external>
          Guide to removing malware</NxTextLink></>,
        FOURTH: <>Learn how to protect your repository to keep developers from downloading Malware again. <NxTextLink
          href="https://links.sonatype.com/nexus-repository-firewall/malware-risk/malware-risk" className="malware-risk-link" external>
          How to protect your repository from malware</NxTextLink></>
      },
      DOWNLOAD_CSV: 'Download CSV',
      CURRENT_TASKS_CONFIGURED: (count) => <>Current tasks configured: <strong>{count}</strong></>,
    },
    OPEN_SOURCE_MALWARE: {
      TITLE: 'What is Open Source Malware?',
      INFO: <>
        <p>Open Source Malware in proxy repositories poses a critical risk to the integrity of the software supply chain,
        introducing malware such as credential harvesting, data exfiltration, backdoor, file system corruption leads to
        compromised applications, data breaches, and regulatory non-compliance.</p>
        <p>Remediation requires immediate removal of infected components, identifying impacted dependencies, and
        Developers must be informed of the threat and prevented from accessing to compromised artifacts.</p>
        <NxTextLink href="https://links.sonatype.com/nexus-repository-firewall/malware-risk/vulnerabilities-and-malware"
                    className="vulnerabilities-and-malware-link" external>
        Differentiating Software Vulnerabilities and Malware</NxTextLink>
      </>,
    },
    POWERED_BY: {
      TEXT: 'Powered by',
      IMAGE: './static/rapture/resources/images/sonatype-firewall-logo-nav.svg'
    },
    MALWARE_MANAGEMENT_TASKS_COUNT: "malwareManagementTasksCount",
  }
}
