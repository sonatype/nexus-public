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
      text: 'OSS Malware Risk',
      textComplement: '<span class="nxrm-new-tag">NEW</span>',
      description: 'Visualize risk in your repositories'
    },
    TITLE: 'Open Source Malware Risk',
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
        DESCRIPTION: 'Open Source malware exploits the open source DevOps tool chain to introduce malware such as ' +
            'credential harvester, crypto-miner, a virus, ransomware, data corruption, malicious code injector, etc.'
      },
      AVERAGE_ATTACK: {
        TEXT: 'Average Cost to Remediate OSS Malware',
        DESCRIPTION: '$5.12 million'
      },
      LEARN_MORE: {
        TEXT: 'Learn More',
        URL: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/press-releases'
      }
    },
    MALICIOUS_EVENTS: {
      UNPROTECTED_MALWARE: {
        TEXT: 'You Are Unprotected',
        DESCRIPTION: <>
          malicious events identified by Sonatype (npmjs.org, PyPI.org and
          <NxTextLink
              href="https://links.sonatype.com/nexus-repository-firewall/malicious-risk/language-and-package-support"
              external> more</NxTextLink> )
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
    LOAD_ERROR: 'An error occurred while fetching the malicious risk data',
  }
}
