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
# design notes #

some files on rubygems.org are actually virtual, like <http://rubygems.org/gems/maven-tools-1.0.3.gem>. storing all gems inside a single directory may or may not the right thing. the nexus-ruby-tools decided it is not a good thing to do because of past inode on the filesystem with such huge directories.

## directory layout - local vs. remote ##

this is the main idea of having ```storage```-path and the ```remote```-path in a ```RubygemsFile``` object. for each file type of the rubygems repo there is special implementation for this type, with ```storage``` and```remote``` path, ```type``` and ```name```.

together with the ```RubygemsFileFactory``` interface all those sub-classes of ```RubygemsFile``` allow to define the storage directory layout vs. the remote layout.

the remote layout is used to access a rubygems repository via any ruby tools.

## converting a path into a RubygemsFile object ##

the pattern matching follows the some ideas of a **cuba** gem from the ruby-world - to explain the name of the package used. so basically each directory knows its content and either creates the right ```RubygemsFile``` or delegates to the sub-directory. each directory has its own **cuba** class.

so the implementations of ```Cuba``` and the ```DefaultRubygemsFileFactory``` define the directory layout.

### the remote directory layout ###

    .
    ├── api
    │   └── v1
    │       └── dependencies
    ├── gems
    │   ├── _-0.1.gem
    │   ├── yaml-1.2.0.gem
    │   └── zip-2.0.2.gem
	├── latest_specs.4.8.gz
    ├── prerelease_specs.4.8.gz
    ├── quick
    |   └── Marshal.4.8
    |       ├── _-.0.1.gemspec.rz
    |       ├── yaml-1.1.0.gem
    |       ├── yaml-1.2.0.gem
    |       └── zip-2.0.2.gem
    └── specs.4.8.gz

### the storage directory layout ###

    .
	├── api
	│   └── v1
	│       └── dependencies
	|           ├── _.json.rz
    |           ├── yaml.json.rz
    |           └── zip.json.rz
	├── gems
	│   ├── _
	|   │   └── _-0.1.gem
    |   ├── y
    |   │   ├── yaml-1.1.0.gem
	|   │   └── yaml-1.2.0.gem
    |   └── z
    |       └── zip-2.0.2.gem
	├── latest_specs.4.8.gz
    ├── prerelease_specs.4.8.gz
    ├── quick
    |   └── Marshal.4.8
    |       ├── _
	|       │   └── _-0.1.gemspec.rz
    |       ├── y
    |       │   ├── yaml-1.1.0.gemspec.rz
	|       │   └── yaml-1.2.0.gemspec.rz
    |       └── z
    |           └── zip-2.0.2.gemspec.rz
    └── specs.4.8.gz

## rubygems repositories - hosted and proxied ##

on hosted rubygems repositories you can add gems, maybe you can delete gems and you can retrieve gems. on proxied rubygems repository there is only retrieve, maybe an additional delete is possible.

the add and delete on hosted one is only allowed on gem files since these actions need to update some metadata (specs.4.8.gz, latest_specs.4.8.gz, prerelease\_specs.4.8.gz). other files can be derieved from the gem file, like quick/Marshal.4.8/zip-2.0.2.gemspec.rz or api/v1/dependencies/zip.json.rz

the hosted repository uses some kind of local storage and the proxied repository will get the data from remote http server.

some request will be generated on the fly like the ones used by a very popular gem dependency manager (bundler gem):

looking at this example from the "bundler API" <http://some.rubygems.repo/api/v1/dependencies?gems=maven-tools,zip,bundler>. the response will be generated from the files api/v1/dependencies/maven-tools.json.rz, api/v1/dependencies/zip.json.rz and api/v1/dependencies/bundler.json.rz. in the hosted case these files can be again generated from the stored gem files. in the proxied case for example api/v1/dependencies/bundler.json.rz is just the response of <http://rubygems.org/api/v1/dependencies?gems=bundler>.

## layout concept ##

the layout is almost like the ```RubygemsFileFactory``` but adds another aspect to ```RubygemsFile``` a payload and a state. the payload can be anything or an Exception. the latter goes allong with state ```ERROR```

possible states are NEW\_INSTANCE, NOT\_EXISTS, ERROR, NO\_PAYLOAD, TEMP\_UNAVAILABLE, FORBIDDEN, PAYLOAD

a layout implementation is associated with a ```Storage``` and modelled to serve one http method: GET, POST, DELETE. it knows where and how to store the different ```RubygemsFile```s or how to generate others content from the stored ones. 

### layouts for hosted repo ###

* HostedGETLayout
* HostedPOSTLayout
* HostedDELETELayout

### layouts for proxied/grouped repo ###

* ProxiedGETLayout
* DeleteLayout

there is not POST-layout since nothing can be posted onto a proxy.

the ```DeleteLayout``` allows to delete all "stored" content but the generated content returns a ```RubygemsFile``` with state FORBIDDEN

## assemble everything into a RubygemsFileSystem ##

a ```RubygemsFileSystem``` has methods to return a plain ```RubygemsFile``` (with state NEW_INSTANCE)

* file( String path )
* file( String path, String query )

or the get methods:
* get( String path )
* get( String path, String query )

the post methods
* post( InputStream is, String path )
* post( InputStream is, RubygemsFile file )

the delete method
* delete( String path )

# tests with using SimpleStorage and CachingStorage #

the ```SimpleStorage``` uses the hard disk to store files and uses ```InputStream```s as payload for ```RubygemsFile```. the ```CachingStorage``` is ```ProxyStoarge``` again caching the files on hard disk and using an URL for the 'remote' rubygems repository.

these are used to run a big part of the unit tests with.

## sample using the test storages ##

just to illustrate how to use a ```ProxiedRubygemsFileSystem``` or a ```HostedRubygemsFileSystem``` using the ```SimpleStorage``` or ```CachingStorage``` respectively. not that the difference between the hosted and proxied case is how the instance variable ```fileSystem``` get set with either a ```ProxiedRubygemsFileSystem``` or a ```HostedRubygemsFileSystem```.

     protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
     {
        RubygemsFile file = fileSystem.get( getPathInfo( req ), req.getQueryString() );
        switch( file.state() )
        {
        case FORBIDDEN:
            resp.sendError( HttpServletResponse.SC_FORBIDDEN );
            break;
        case NOT_EXISTS:
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            break;
        case NO_PAYLOAD:
		    // do something
            break;
        case ERROR:
            throw new ServletException( file.getException() );
        case TEMP_UNAVAILABLE:
            resp.setHeader("Retry-After", "120");//seconds
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE );
            break;
        case PAYLOAD:
            resp.setContentType( file.type().mime() );
            if ( file.type().encoding() != null )
            {
                resp.setCharacterEncoding( file.type().encoding() );
            }
            if( file.type().isVaryAccept() )
            {
                resp.setHeader("Vary", "Accept");
            }
            IOUtil.copy( (InputStream) file.get(), resp.getOutputStream() );
            break;
        case NEW_INSTANCE:
            throw new ServletException( "BUG: should never reach here" );
        }
     }

the code for using a ```RubygemsFileSystem``` in a nexus plugin is very similar, it just needs to deal with a different payload and exceptions.

## Storage and ProxyStorage ##

these interfaces are the main ones to integrate with the nexus-2.x plugins. the ```ProxyStorage``` has some extra methods to allow to deal with Bundler API of rubygems repositories. those Bundler API responses are volatile but to allow some short lifed cache they have to treated differently from the hosted case.

the nexus-2.x ruby plugin has three implementation of the ```Storage``` and ```ProxyStorage``` for hosted, proxied and grouped case but uses the ```RubygemsFileSystem``` from here.
