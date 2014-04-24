Bridge (Play Framework, Spring, AngularJS)
=========================================

[![Build Status](https://travis-ci.org/Sage-Bionetworks/BridgePF.svg?branch=develop)](https://travis-ci.org/Sage-Bionetworks/BridgePF)

Install Play Framework 2.2.x, node + npm, and bower (npm install -g bower). 

To get up and running, first start play and generate the Eclipse meta 
files:

    play (starts play CLI)
    eclipse (within the CLI)

It should be possible to import the project as an existing project in Eclipse. 
Play will handle Java-based dependencies. For the AngularJS code, do the 
following:

    cd public
    npm install
    bower install

Thereafter while working you can run `grunt` or even better, `grunt watch`, and 
the JS/SASS/CSS files will be pre-processed while you work (the "built" versions 
are the versions linked to in the code, so you must do this build to see your 
changes). This pre-processing happens faster than Play can typically update on a 
refresh.

Development
-----------

Bridge uses Synapse as a back end service. Setting up Synapse in development to 
support Bridge can be slow, so a stub version of the Java SynapseClient exists. 
Unless we find the moral equivalent of Maven profiles in the Play Framework, you'll
have to swap the implementation in the application-context.xml file in order to 
use it.

Tests
-----

For AngularJS tests:

    cd public
    grunt test
    
Runs Jasmine-based tests (there's good integration with Angular for Jasmine tests), 
from the command line, using PhantomJS. At the moment, Play does not need to be 
running in order to run the tests (they are true unit tests with mocks). 

This file will be packaged with your application, when using `play dist`.