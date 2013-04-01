monkeypuzzle
============

Monkeypuzzle is a fork of the Araucaria Project

The following housekeeping is planned:

* Ensure that the app builds underi current versions of Java (eg. Java 7).
    * Mostly this requires replacing the use of com.sun.image.codec.jpeg with javax.imageio.ImageIO. Not only is com.sun.image.codec.jpeg unavailable under Java 7 but com.sun imports should not be used where there are equivalent javax imports available. Currently this means that JPEG export is broken.
* Create a better package organisation for the src. Currently src is neither name-spaced nor organised hierarchically which makes it difficult to find extensions points for new functionality.
* Institute a CLI build strategy that uses ant (or maven) and which enables scripted builds to be reliably performed.
* There are NO TESTS - how can we add new functionality if we do not know whether the new code will break existing functionality.
* The src code is not stored under a version control system. This is a priority.
* Once version control is implemented, a continuous integration infrastructure should be instituted so that regular & reliable build, test, deploy actions can be taken.
* Once the src is under version control, all libs must be either added to the repo or scripted for inclusion at build/packaging.
* If libs are added then licensing information for each lib must be included.
* Licensing for the app must be made clear and included with both the src and release versions.
* To avoid any problems with the Araucaria brand, the src will be taken and rebranded Monkeypuzzle inline with the requirements of the GPL
* Currently glyphs/icons/images etc are hardlinked into the src from a directory relative to the src directory. This should be adjusted so that:
    * All assets are stored consistently within the project repository
	* Builds can be consistently & reliable performed
	* Either assets should be included within the output jar file to enable a single monkeypuzzle uber-jar distribution (ready to run without hassle of installing extra libs/dependencies) or else sufficient scripts should be included to ensure that everything runs correctly
* Get rid of hard-coded remote DB addresses, at the very least these should be specified in an external configuration file so that they can be altered at runtime. [see below for discussion of using multiple remote DBs & hosting a local monkeypuzzle DB & Running a standalone local instance].

The following development activities are planned:
* Export to JSON
* Headless use with CLI
* Export to SVG
* Export to DOT (for 3rd party rendering- Omnigraffle & GraphViz both render very nice graphs and could be made to easily accept argument graph descriptions)
* Update in-app rendering to support AIF-based argument graphs rather than AML trees - I think that this should be another pane in the default view so that Monkeypuzzle supports diagrammin using the following formats as distinct approaches: AIF, AML, Toulmin, Wigmore
    * Whilst AML might be deprecated to some degree due to the relative dormancy of Araucaria over the last 5-6 years, and due to the simultaneous ascendancy of AIF, however, the simplicity of AML, pedagogical utility, it is easy and simple to use to teach the basics of argument diagramming, and the corresponding complexity of AIF mean that AML should be maintained in Monkeypuzzle for the forseeable, at least as a diagramming style.
* Export HTML web-page containing embedded AML/AIF markup (RDF annotations/linked data schema/etc) so that arguments can be published to the web directly from Monkeypuzzle and so that those publised arguments are discoverable.
* Integrate with OS X to get a native look & feel, e.g. menus in the right place.a
* Argument DBs:
    * Users should be able to select which remote DB they wish to push their arguments to.
	* Users should be able to save arguments to a local DB (internal to monkeypuzzle, e.g. SQLite)
	* Users should be able to run their won local, standalone argument DB, and save arguments to it from their own Monkeypuzzle instance. They should also be able to query this DB from a web/HTTP REST I/F as well as from within Monkeypuzzle, and via a CLI.
* Create mode:
    * Users should be able to build an argument from scratch which can subsequently be used to structure a longer piece of argumentative prose. This is useful pedagogically, to ensure that the student has a clear and well reasoned argument that they then write about (NB. The student could always supply the argument diagram alongside the prose so that their instructor can see where the written record deviated from the plan). This is also useful in all circumstances where people are performing evidencial reasoning such as in legal argument. Instead of merely analysing others arguments from available texts, the users builds a new argument, perhaprs rooted in existing analysed evidence from within the user's argument DB.
