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
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';

export default {
  MALICIOUS_RISK: {
    MENU: {
      text: 'Malware Risk',
      textComplement: '<span class="nxrm-new-tag">NEW</span>',
      description: 'Visualize risk in your repositories',
      icon: faExclamationTriangle
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
      TITLE_PLURAL: 'Malicious Packages Found',
      TITLE_SINGULAR: 'Malicious Package Found',
      DESCRIPTION: {
        TITLE: <><strong>Open source malware is cached in the proxy repositories of this instance of Nexus Repository Manager</strong></>,
        CONTENT: 'Open source malware is cached in the proxy repositories on your Nexus Repository. Review the components flagged by Sonatype as containing malware and remove them.',
        ADDITIONAL_NON_ADMIN_CONTENT: <><strong>Contact your instance administrator to resolve.</strong></>
      },
      CONTACT_SONATYPE: {
        TEXT: 'Contact Sonatype to Resolve',
        URL: {
          OSS: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/oss-admin-learn-more',
          PRO: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/firewall/pro-admin-learn-more'
        }
      },
      VIEW_MALWARE_RISK: 'Remediate Malicious Packages'
    },
    LOAD_ERROR: 'An error occurred while fetching the malicious risk data',
    HDS_CONNECTION_WARNING: <>
      Malicious Packages data relies on backend services that are currently unreachable. To view malware risk, <NxTextLink
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
      TEXT: 'Powered by'
    },
    MALWARE_MANAGEMENT_TASKS_COUNT: "malwareManagementTasksCount",

    // Redesign – Malicious vs Vulnerable, state-dependent UX
    REDESIGN: {
      MALICIOUS_VS_VULNERABLE: {
        TITLE: 'Malicious vs Vulnerable Open Source',
        MALICIOUS: {
          TITLE: 'Malicious (this page)',
          DEFINITION: 'Intentional harm',
          EXAMPLES: 'Backdoors, credential theft, crypto miners',
          LEARN_MORE: 'Learn more',
          LEARN_URL: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/vulnerabilities-and-malware',
        },
        VULNERABLE: {
          TITLE: 'Vulnerable (Health Check)',
          DEFINITION: 'Unintentional security bugs',
          EXAMPLES: 'CVEs, misconfigurations',
          VIEW_HEALTH: 'View Health Check',
        },
      },
      HERO: {
        NO_MALWARE: 'No malware detected',
        PROTECTED: "You're protected. No malware detected.",
        COMPONENTS_FOUND: (n) => `${n.toLocaleString()} Malware Component${n !== 1 ? 's' : ''} Found`,
        ALL_PROTECTED: 'All proxy repositories protected by Firewall.',
      },
      TASKS: {
        CONFIGURED: (n) => `${n.toLocaleString()} task${n !== 1 ? 's' : ''} configured`,
        NONE: '0 tasks configured',
        CONFIGURE: 'Configure Malware Management Tasks',
      },
      STEPS: {
        TITLE: 'Remediation Steps',
        STEP1: { TITLE: 'Configure tasks', DESC: 'Set up Automatic Malware Management tasks on proxy repositories.', CTA: 'Configure Tasks' },
        STEP2: { TITLE: 'Download CSV', DESC: 'Export components cleaned by Malware Removal Tasks.', CTA: 'Download CSV', DISABLED: 'No components to export' },
        STEP3: { TITLE: 'Search & remove', DESC: 'Find and remove malicious components from your repositories.', CTA: 'Search repositories' },
        STEP4: { TITLE: 'Protect', DESC: 'Enable Firewall to block future malware from entering.', CTA_ENABLE: 'Enable Firewall', CTA_LEARN: 'Learn how to protect', CTA_MANAGE: 'Manage Firewall', CTA_PROTECTED: "You're protected" },
      },
      UPSELL: {
        NO_FIREWALL: 'Keep bad code out of your repository. Repository Firewall blocks malicious and vulnerable components at the moment they\'re requested\u2014before they ever enter your repo.',
        LEARN_MORE: 'Learn more',
        CONTACT_SALES: 'Contact sales',
        WITH_MALWARE: 'Remove these components, then protect your repos. Without Firewall, malware can re-enter.',
      },
      ENABLEMENT: {
        HAVE_FIREWALL: 'You have Repository Firewall. Enable it on proxy repos to keep malware out.',
        ENABLE_CTA: 'Enable Firewall',
        REMOVE_THEN_ENABLE: 'Remove these components, then enable Firewall on your proxy repos to prevent future infections.',
      },
      WIZARD: {
        TITLE: 'Malware Remediation',
        STEPS: [
          { title: "What's infected", key: '1' },
          { title: 'Clean up', key: '2' },
          { title: 'Add protection to repo', key: '3' },
          { title: 'Protect all repos', key: '4' },
          { title: 'Investigation', key: '5' },
        ],
      },
      COMPONENT_LIST: {
        TITLE: 'Malware Components',
        DESC: 'Remove these components with one click. Each removal deletes the asset from the repository.',
        REPOSITORY: 'Repository',
        PATH: 'Path',
        FORMAT: 'Format',
        RECORDED: 'Recorded',
        REMOVE: 'Remove',
        REMOVING: 'Removing…',
        REMOVED: 'Removed',
        REMOVE_FAILED: 'Failed to remove',
      },
      // Main Tab Status View (UX Spec MAIN-TAB-UX-DESIGN.md)
      STATUS_VIEW: {
        FOOTER: {
          EDUCATION_LINK: 'What is Open Source Malware?',
          TASKS_LINK: 'Configure automatic malware removal',
        },
      },
      // Education Modal (UX Spec MAIN-TAB-UX-DESIGN.md Section 9)
      EDUCATION_MODAL: {
        TITLE: 'Understanding Open Source Security Threats',
        MALICIOUS: {
          TITLE: 'Malicious Packages',
          DESCRIPTION: 'Purpose-built to attack developers and CI/CD pipelines.',
          EXAMPLES_LABEL: 'Examples:',
          EXAMPLES: [
            'Typosquats (e.g., "coloUrs" mimicking "colors")',
            'Backdoored packages with credential stealers',
            'Dependency confusion attacks',
          ],
          DETECTION: 'Sonatype Repository Firewall identifies malicious packages using behavioral analysis and threat intelligence.',
        },
        VULNERABLE: {
          TITLE: 'Vulnerable Packages',
          DESCRIPTION: 'Legitimate packages with known security flaws (CVEs).',
          EXAMPLES_LABEL: 'Examples:',
          EXAMPLES: [
            'Log4Shell (CVE-2021-44228)',
            'Spring4Shell (CVE-2022-22965)',
            'Packages with outdated dependencies',
          ],
          DETECTION: 'Repository Health Check scans for known vulnerabilities in your components.',
        },
        KEY_DIFFERENCE: {
          TITLE: 'Key Difference',
          MALICIOUS_LINE: 'Intentionally harmful - block immediately',
          VULNERABLE_LINE: 'Unintentionally flawed - upgrade when possible',
        },
        LEARN_MORE: 'Learn More',
        LEARN_MORE_URL: 'https://links.sonatype.com/nexus-repository-firewall/malware-risk/vulnerabilities-and-malware',
      },
    },
  }
}
