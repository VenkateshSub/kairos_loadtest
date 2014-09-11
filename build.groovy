import tablesaw.*
import tablesaw.addons.GZipRule
import tablesaw.addons.TarRule
import tablesaw.addons.ivy.IvyAddon
import tablesaw.addons.ivy.PomRule
import tablesaw.addons.ivy.PublishRule
import tablesaw.addons.java.Classpath
import tablesaw.addons.java.JarRule
import tablesaw.addons.java.JavaCRule
import tablesaw.addons.java.JavaProgram
import tablesaw.addons.junit.JUnitRule
import tablesaw.definitions.Definition
import tablesaw.rules.DirectoryRule
import tablesaw.rules.Rule
import tablesaw.rules.SimpleRule

import javax.swing.*

println("===============================================");

saw.setProperty(Tablesaw.PROP_MULTI_THREAD_OUTPUT, Tablesaw.PROP_VALUE_ON)

programName = "kairos_loadtest"
//Do not use '-' in version string, it breaks rpm uninstall.
version = "0.1.0"
release = "1" //package release number
summary = "KairosDB LoadTest"
description = """\
KairosDB Load Test
"""

saw.setProperty(JavaProgram.PROGRAM_NAME_PROPERTY, programName)
saw.setProperty(JavaProgram.PROGRAM_DESCRIPTION_PROPERTY, description)
saw.setProperty(JavaProgram.PROGRAM_VERSION_PROPERTY, version+'-'+release)
saw.setProperty(PomRule.GROUP_ID_PROPERTY, "org.kairosdb")
saw.setProperty(PomRule.URL_PROPERTY, "http://kairosdb.org")

saw = Tablesaw.getCurrentTablesaw()
saw.includeDefinitionFile("definitions.xml")

ivyConfig = ["default"]


rpmDir = "build/rpm"
docsDir = "build/docs"
rpmNoDepDir = "build/rpm-nodep"
new DirectoryRule("build")
rpmDirRule = new DirectoryRule(rpmDir)
rpmNoDepDirRule = new DirectoryRule(rpmNoDepDir)

//------------------------------------------------------------------------------
//Setup java rules
ivy = new IvyAddon()
		.addSettingsFile("ivysettings.xml")

if (new File("myivysettings.xml").exists())
	ivy.addSettingsFile("myivysettings.xml")

ivy.setup()

buildLibraries = new RegExFileSet("lib", ".*\\.jar").recurse()
		.addExcludeDir("integration")
		.getFullFilePaths()

jp = new JavaProgram()
		.setLibraryJars(buildLibraries)
		.setup()

jc = jp.getCompileRule()
jc.addDepend(ivy.getResolveRule("default"))

jc.getDefinition().set("target", "1.6")
jc.getDefinition().set("source", "1.6")



//------------------------------------------------------------------------------
//Set information in the manifest file
manifest = jp.getJarRule().getManifest().getMainAttributes()
manifest.putValue("Manifest-Version", "1.0")
manifest.putValue("Tablesaw-Version", saw.getVersion())
manifest.putValue("Created-By", saw.getProperty("java.vm.version")+" ("+
			saw.getProperty("java.vm.vendor")+")")
manifest.putValue("Built-By", saw.getProperty("user.name"))
buildDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
manifest.putValue("Build-Date", buildDateFormat.format(new Date()))

buildNumberFormat = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
buildNumber = buildNumberFormat.format(new Date())
manifest.putValue("Implementation-Title", "KairosDBLoadTest")
manifest.putValue("Implementation-Vendor", "Proofpoint Inc.")
manifest.putValue("Implementation-Version", "${version}-${release}.${buildNumber}")

//Add git revision information
gitRevisionFile= ".gitrevision"
new File(gitRevisionFile).text = ""
ret = saw.exec(null, "git rev-parse HEAD", false, null, gitRevisionFile);
revision = new File(gitRevisionFile).text.trim()
new File(gitRevisionFile).delete()
if (ret == 0)
	manifest.putValue("Git-Revision", revision);


//------------------------------------------------------------------------------
//Setup unit tests
testClasspath = new Classpath(jp.getLibraryJars())
testClasspath.addPath(jp.getJarRule().getTarget())


testSources = new RegExFileSet("src/test/java", ".*Test\\.java").recurse()
		.addExcludeFiles("CassandraDatastoreTest.java")
		.getFilePaths()
testCompileRule = jp.getTestCompileRule()
testCompileRule.addDepend(ivy.getResolveRule("test"))

junitClasspath = new Classpath(testCompileRule.getClasspath())
junitClasspath.addPaths(testClasspath)
junitClasspath.addPath("src/main/java")
junitClasspath.addPath("src/test/resources")
junitClasspath.addPath("src/main/resources")

junit = new JUnitRule("junit-test").addSources(testSources)
		.setClasspath(junitClasspath)
		.addDepends(testCompileRule)

if (saw.getProperty("jacoco", "false").equals("true"))
	junit.addJvmArgument("-javaagent:lib_test/jacocoagent.jar=destfile=build/jacoco.exec")

testSourcesAll = new RegExFileSet("src/test/java", ".*Test\\.java").recurse().getFilePaths()
junitAll = new JUnitRule("junit-test-all").setDescription("Run unit tests including Cassandra and HBase tests")
		.addSources(testSourcesAll)
		.setClasspath(junitClasspath)
		.addDepends(testCompileRule)

if (saw.getProperty("jacoco", "false").equals("true"))
	junitAll.addJvmArgument("-javaagent:lib_test/jacocoagent.jar=destfile=build/jacoco.exec")


//------------------------------------------------------------------------------
//Run the Kairos application
new SimpleRule("run-debug").setDescription("Runs kairosdb so a debugger can attach to port 5005")
		.addDepends(jp.getJarRule())
		.setMakeAction("doRun")
		.setProperty("DEBUG", true)

new SimpleRule("run").setDescription("Runs kairosdb")
		.addDepends(jp.getJarRule())
		.setMakeAction("doRun")
		.setProperty("DEBUG", false)


def doRun(Rule rule)
{
	args = ""
	debug = ""
	if (rule.getProperty("DEBUG"))
		debug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

	//this is to load logback into classpath
	runClasspath = jc.getClasspath()
	runClasspath.addPath("src/main/resources")
	ret = saw.exec("java ${debug} -cp ${runClasspath} org.kairosdb.loadtest.Main ${args}", false)
	println(ret);
}



//------------------------------------------------------------------------------
//Build notification
def printMessage(String title, String message) {
	osName = saw.getProperty("os.name")

	Definition notifyDef;
	if (osName.startsWith("Linux"))
	{
		notifyDef = saw.getDefinition("linux-notify")
	}
	else if (osName.startsWith("Mac"))
	{
		notifyDef = saw.getDefinition("mac-notify")
	}

	if (notifyDef != null)
	{
		notifyDef.set("title", title)
		notifyDef.set("message", message)
		saw.exec(notifyDef.getCommand())
	}
}

def buildFailure(Exception e)
{
	printMessage("Build Failure", e.getMessage())
}

def buildSuccess(String target)
{
	printMessage("Build Success", target)
}
