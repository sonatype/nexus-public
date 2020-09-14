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
 * {@link RDescriptionUtils} unit tests.
 */
class RMetadataUtilsTest
    extends Specification
{
  def 'Properly parse metadata in a DESCRIPTION file'() {
    when: 'The metadata is extracted from a file'
      Map<String, List<String>> attributes =
          getClass().getResourceAsStream('/org/sonatype/nexus/repository/r/internal/DESCRIPTION').
              withCloseable { input -> RMetadataUtils.parseDescriptionFile(input) }
    then:
      attributes == [
          'Package': 'ggplot2',
          'Version': '2.1.0',
          'Authors@R': 'c(\n    person("Hadley", "Wickham", , "hadley@rstudio.com", c("aut", "cre")),\n    person("Winston", "Chang", , "winston@rstudio.com", "aut"),\n    person("RStudio", role = "cph")\n    )',
          'Title': 'An Implementation of the Grammar of Graphics',
          'Description': 'An implementation of the grammar of graphics in R. It combines the\n    advantages of both base and lattice graphics: conditioning and shared axes\n    are handled automatically, and you can still build up a plot step by step\n    from multiple data sources. It also implements a sophisticated\n    multidimensional conditioning system and a consistent interface to map\n    data to aesthetic attributes. See http://ggplot2.org for more information,\n    documentation and examples.',
          'Depends': 'R (>= 3.1)',
          'Imports': 'digest, grid, gtable (>= 0.1.1), MASS, plyr (>= 1.7.1),\n        reshape2, scales (>= 0.3.0), stats',
          'Suggests': 'covr, ggplot2movies, hexbin, Hmisc, lattice, mapproj, maps,\n        maptools, mgcv, multcomp, nlme, testthat (>= 0.11.0), quantreg,\n        knitr, rpart, rmarkdown, svglite',
          'Enhances': 'sp',
          'License': 'GPL-2',
          'URL': 'http://ggplot2.org, https://github.com/hadley/ggplot2',
          'BugReports': 'https://github.com/hadley/ggplot2/issues',
          'LazyData': 'true',
          'Collate': '\'ggproto.r\' \'aaa-.r\' \'aes-calculated.r\'\n        \'aes-colour-fill-alpha.r\' \'aes-group-order.r\'\n        \'aes-linetype-size-shape.r\' \'aes-position.r\' \'utilities.r\'\n        \'aes.r\' \'legend-draw.r\' \'geom-.r\' \'annotation-custom.r\'\n        \'annotation-logticks.r\' \'geom-polygon.r\' \'geom-map.r\'\n        \'annotation-map.r\' \'geom-raster.r\' \'annotation-raster.r\'\n        \'annotation.r\' \'autoplot.r\' \'bench.r\' \'bin.R\' \'coord-.r\'\n        \'coord-cartesian-.r\' \'coord-fixed.r\' \'coord-flip.r\'\n        \'coord-map.r\' \'coord-munch.r\' \'coord-polar.r\'\n        \'coord-quickmap.R\' \'coord-transform.r\' \'data.R\' \'facet-.r\'\n        \'facet-grid-.r\' \'facet-labels.r\' \'facet-layout.r\'\n        \'facet-locate.r\' \'facet-null.r\' \'facet-viewports.r\'\n        \'facet-wrap.r\' \'fortify-lm.r\' \'fortify-map.r\'\n        \'fortify-multcomp.r\' \'fortify-spatial.r\' \'fortify.r\' \'stat-.r\'\n        \'geom-abline.r\' \'geom-rect.r\' \'geom-bar.r\' \'geom-bin2d.r\'\n        \'geom-blank.r\' \'geom-boxplot.r\' \'geom-path.r\' \'geom-contour.r\'\n        \'geom-count.r\' \'geom-crossbar.r\' \'geom-segment.r\'\n        \'geom-curve.r\' \'geom-defaults.r\' \'geom-ribbon.r\'\n        \'geom-density.r\' \'geom-density2d.r\' \'geom-dotplot.r\'\n        \'geom-errorbar.r\' \'geom-errorbarh.r\' \'geom-freqpoly.r\'\n        \'geom-hex.r\' \'geom-histogram.r\' \'geom-hline.r\' \'geom-jitter.r\'\n        \'geom-label.R\' \'geom-linerange.r\' \'geom-point.r\'\n        \'geom-pointrange.r\' \'geom-quantile.r\' \'geom-rug.r\'\n        \'geom-smooth.r\' \'geom-spoke.r\' \'geom-text.r\' \'geom-tile.r\'\n        \'geom-violin.r\' \'geom-vline.r\' \'ggplot2.r\' \'grob-absolute.r\'\n        \'grob-dotstack.r\' \'grob-null.r\' \'grouping.r\' \'guide-colorbar.r\'\n        \'guide-legend.r\' \'guides-.r\' \'guides-axis.r\' \'guides-grid.r\'\n        \'hexbin.R\' \'labels.r\' \'layer.r\' \'limits.r\' \'margins.R\'\n        \'panel.r\' \'plot-build.r\' \'plot-construction.r\' \'plot-last.r\'\n        \'plot.r\' \'position-.r\' \'position-collide.r\' \'position-dodge.r\'\n        \'position-fill.r\' \'position-identity.r\' \'position-jitter.r\'\n        \'position-jitterdodge.R\' \'position-nudge.R\' \'position-stack.r\'\n        \'quick-plot.r\' \'range.r\' \'save.r\' \'scale-.r\' \'scale-alpha.r\'\n        \'scale-brewer.r\' \'scale-continuous.r\' \'scale-date.r\'\n        \'scale-discrete-.r\' \'scale-gradient.r\' \'scale-grey.r\'\n        \'scale-hue.r\' \'scale-identity.r\' \'scale-linetype.r\'\n        \'scale-manual.r\' \'scale-shape.r\' \'scale-size.r\' \'scale-type.R\'\n        \'scales-.r\' \'stat-bin.r\' \'stat-bin2d.r\' \'stat-bindot.r\'\n        \'stat-binhex.r\' \'stat-boxplot.r\' \'stat-contour.r\'\n        \'stat-count.r\' \'stat-density-2d.r\' \'stat-density.r\'\n        \'stat-ecdf.r\' \'stat-ellipse.R\' \'stat-function.r\'\n        \'stat-identity.r\' \'stat-qq.r\' \'stat-quantile.r\'\n        \'stat-smooth-methods.r\' \'stat-smooth.r\' \'stat-sum.r\'\n        \'stat-summary-2d.r\' \'stat-summary-bin.R\' \'stat-summary-hex.r\'\n        \'stat-summary.r\' \'stat-unique.r\' \'stat-ydensity.r\' \'summary.r\'\n        \'theme-defaults.r\' \'theme-elements.r\' \'theme.r\'\n        \'translate-qplot-ggplot.r\' \'translate-qplot-lattice.r\'\n        \'utilities-break.r\' \'utilities-grid.r\' \'utilities-help.r\'\n        \'utilities-matrix.r\' \'utilities-resolution.r\'\n        \'utilities-table.r\' \'zxx.r\' \'zzz.r\'',
          'VignetteBuilder': 'knitr',
          'RoxygenNote': '5.0.1',
          'NeedsCompilation': 'no',
          'Packaged': '2016-02-29 20:47:22 UTC; hadley',
          'Author': 'Hadley Wickham [aut, cre],\n  Winston Chang [aut],\n  RStudio [cph]',
          'Maintainer': 'Hadley Wickham <hadley@rstudio.com>',
          'Repository': 'CRAN',
          'Date/Publication': '2016-03-01 15:47:24',
          'Built': 'R 3.3.0; ; 2016-05-05 12:00:56 UTC; unix'
      ]
  }
}
