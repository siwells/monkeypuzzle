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

The following updates are planned:
* Export to JSON
* Headless use with CLI
* Export to SVG
* Export to DOT (for 3rd party rendering- Omnigraffle & GraphViz both render very nice graphs and could be made to easily accept argument graph descriptions)
