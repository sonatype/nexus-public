<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2008-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Nexus Rapture Baseapp

This module provides the baseapp muck for ExtJS 6.5+ pre-compiled with
[Sencha CMD](https://docs.sencha.com/cmd/6.5.3/index.html).

Requires Sencha CMD 6.2+

## Styles

Style is managed in 2 parts, the 'rapture-theme' and the application styles.

Each of these is managed in 3 major parts:

* `etc/all.scss`
* `var/**/*.scss`
* `src/**/*.scss`

The order of inclusion is complex, for more details see 
[Organization of Generated Styles](http://docs.sencha.com/extjs/4.2.5/#!/guide/theming).

This boils down to:

1. theme `etc`
2. application `etc`
3. application `var`
4. theme `var`
5. theme `src`
6. application `src`

Where theme `var` and `src` includes are based on the Javascript class dependency order,
and the application includes are explicit.

### Theme

The theme styles are located in `src/main/baseapp/packages/rapture-theme/{sass|resources}` and is structured 
as a Sencha CMD package.

Styles here are to augment the extended-style 'ext-theme-neptune' with customizations for the Rapture look and feel.

### Application

The application styles are located in `src/main/baseapp/sass`.

There is a related tree under `src/main/baseapp/app` which is needed for our non-standard plugable UI for Sencha CMD
to trigger inclusion of styles, as it only includes style files for Javascript classes that it thinks are included 
in the application.  Since the bulk of the application is not contained in this module, but in other plugins,
there is no way for Sencha CMD to know what to include.

To work around this we include a single empty Javascript class `baseapp.Application`, 
and an explicit require line in `src/mian/baseapp/app.js`, so that SCSS sources will get included.

Management of other files here is managed via _explicitly ordered_ imports.

## Driver

### Build

Any time the style or application configuration changes, the baseapp needs to be rebuilt and committed to source control.
This is done so that each developer on each build is not required to have Sencha CMD installed, nor incur the cost to
build the aggregate Javascript for the ExtJS library or the theme+application styling each time.

    mvn clean install -Pdriver -Dmode=build

The baseapp-debug.js and baseapp-prod.js files will always have a "hash" in the Ext.Manifest object that changes which we can ignore.
If there are any other changes then make sure to commit all changes.

    git commit . -m "regenerated baseapp"

#### Flavors

Slightly faster, will only build the 'debug' flavors and will re-use extjs distribution and baseapp: 

    mvn -Pdriver -Dmode=build -Dflavors=debug

### Watch

To use the `app watch` feature to rebuild styling when changed: 

    mvn -Pdriver -Dmode=watch

This is functionally equivalent to:

    cd src/main/baseapp
    sencha app watch testing

### Clobber

To remove the overlayed bits from baseapp: 

    mvn -Pdriver -Dmode=clobber

### Configuration

Most of the default Sencha CMD files (it needs a lot of junk) are left ASIS only a few have been customized:

* `src/main/baseapp/app.json`
* `src/main/baseapp/workspace.json`

