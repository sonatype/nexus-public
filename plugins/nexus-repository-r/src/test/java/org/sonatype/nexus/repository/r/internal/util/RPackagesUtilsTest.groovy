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
package org.sonatype.nexus.repository.r.internal.util

import spock.lang.Specification

/**
 * {@link RPackagesUtils} unit tests.
 */
class RPackagesUtilsTest
    extends Specification
{
  def 'Properly parse metadata'() {
    when:
      List<Map<String, String>> metadata =
          getClass().getResourceAsStream('/org/sonatype/nexus/repository/r/internal/PACKAGES').
              withCloseable { input -> RPackagesUtils.parseMetadata(input) }
    then:
      metadata == [
          [
              Package         : 'A3',
              Version         : '1.0.0',
              Depends         : 'R (>= 2.15.0), xtable, pbapply',
              Suggests        : 'randomForest, e1071',
              License         : 'GPL (>= 2)',
              NeedsCompilation: 'no'
          ],
          [
              Package         : 'abbyyR',
              Version         : '0.5.0',
              Depends         : 'R (>= 3.2.0)',
              Imports         : 'httr, XML, curl, readr, progress',
              Suggests        : 'testthat, rmarkdown, knitr (>= 1.11)',
              License         : 'MIT + file LICENSE',
              NeedsCompilation: 'no'
          ],
          [
              Package         : 'abc',
              Version         : '2.1',
              Depends         : 'R (>= 2.10), abc.data, nnet, quantreg, MASS, locfit',
              License         : 'GPL (>= 3)',
              NeedsCompilation: 'no'

          ],
          [
              Package         : 'ABCanalysis',
              Version         : '1.1.1',
              Depends         : 'R (>= 2.10)',
              Imports         : 'Hmisc, plotrix',
              License         : 'GPL-3',
              NeedsCompilation: 'no']

      ]
  }

  def 'Properly merge metadata'() {
    when:
      List<Map<String, String>> metadata = RPackagesUtils.merge(
          [
              [
                  [
                      Package         : 'abc',
                      Version         : '2.1',
                      Depends         : 'R (>= 2.10), abc.data, nnet, quantreg, MASS, locfit',
                      License         : 'GPL (>= 3)',
                      NeedsCompilation: 'no'
                  ],
                  [
                      Package         : 'ABCanalysis',
                      Version         : '1.1.2',
                      Depends         : 'R (>= 2.10)',
                      Imports         : 'Hmisc, plotrix',
                      License         : 'GPL-3',
                      NeedsCompilation: 'no'
                  ],
                  [
                      Package         : 'AquaEnv',
                      Version         : '1.0-3',
                      Depends         : 'minpack.lm, deSolve',
                      License         : 'GPL (>= 2)',
                      NeedsCompilation: 'no'
                  ]
              ],
              [
                  [
                      Package         : 'abc',
                      Version         : '2.2',
                      Depends         : 'R (>= 2.10), abc.data, nnet, quantreg, MASS, locfit',
                      License         : 'GPL (>= 3)',
                      NeedsCompilation: 'no'
                  ],
                  [
                      Package         : 'ABCanalysis',
                      Version         : '1.1.1',
                      Depends         : 'R (>= 2.10)',
                      Imports         : 'Hmisc, plotrix',
                      License         : 'GPL-3',
                      NeedsCompilation: 'no'
                  ],
                  [
                      Package         : 'AquaEnv',
                      Version         : '1.0-4',
                      Depends         : 'minpack.lm, deSolve',
                      License         : 'GPL (>= 2)',
                      NeedsCompilation: 'no'
                  ]
              ]
          ])
    then:
      metadata == [
          [
              Package         : 'abc',
              Version         : '2.1',
              Depends         : 'R (>= 2.10), abc.data, nnet, quantreg, MASS, locfit',
              License         : 'GPL (>= 3)',
              NeedsCompilation: 'no'
          ],
          [
              Package         : 'ABCanalysis',
              Version         : '1.1.2',
              Depends         : 'R (>= 2.10)',
              Imports         : 'Hmisc, plotrix',
              License         : 'GPL-3',
              NeedsCompilation: 'no'
          ],
          [
              Package         : 'AquaEnv',
              Version         : '1.0-3',
              Depends         : 'minpack.lm, deSolve',
              License         : 'GPL (>= 2)',
              NeedsCompilation: 'no'
          ]
      ]
  }
}
