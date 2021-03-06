import sbt._
import sbt.Keys._
import java.io.File

object VaadinPlugin extends Plugin {

  lazy val Vaadin = config("vaadin") extend (Compile)

  val vaadinVersion = SettingKey[String]("vaadin-version")
  val vaadinGenerateWidgetSet = TaskKey[Unit]("vaadin-generate-widgetset", "Generates a combined widget set from all widget sets in the class path, including project sources")
  val vaadinCompileWidgetSet = TaskKey[Unit]("vaadin-compile-widgetset", "Runs the GWT compiler")

  val vaadinWidgetSet = SettingKey[String]("vaadin-widgetset")
  val vaadinClientWidgetSetDestination = SettingKey[File]("vaadin-client-widgetset-destination")

  lazy val vaadinSettings: Seq[Setting[_]] = inConfig(Vaadin)(Defaults.configSettings) ++ Seq(
    managedClasspath in Vaadin <<= (managedClasspath in Compile, update) map { (cp, up) =>
      cp ++ Classpaths.managedJars(Provided, Set("src"), up) },
    unmanagedClasspath in Vaadin <<= (unmanagedClasspath in Compile).identity,
    autoScalaLibrary := false,
    vaadinVersion := "7.0.2",
    libraryDependencies <++= vaadinVersion({ vaadin_version: String => Seq(
      "com.vaadin" % "vaadin-client-compiler" % vaadin_version % "provided"
    ) }),
    vaadinClientWidgetSetDestination <<= (sourceDirectory){ (src:File) => src / "main" / "webapp" / "VAADIN" / "widgetsets" },
    /**
     *  Issue #2: make sure the GWT sdk first in the classpath
     *  thanks to anovstrup
     */
    dependencyClasspath in Vaadin <<= (dependencyClasspath in Vaadin) map { cp =>
        val (a, b) = cp partition { _.data.getAbsolutePath.contains("gwt") }
        (a ++ b)
      },
    /**
     *
     */
    vaadinCompileWidgetSet <<= (dependencyClasspath in Vaadin,
                                vaadinWidgetSet,
                                javaSource in Compile,
                                resourceDirectory in Compile,
                                vaadinClientWidgetSetDestination, streams) map
      { (dependencyClasspath, widgetSet, javaSource, resourceDir, clientDst, s) =>
        {
          IO.createDirectory(clientDst)
          val cp = Seq(resourceDir.absolutePath, javaSource.absolutePath) ++ dependencyClasspath.map(_.data.absolutePath)
          val command = "java -cp " + cp.mkString(File.pathSeparator) + " com.google.gwt.dev.Compiler -war " + clientDst.absolutePath + " " + widgetSet
          s.log.info("Compiling " + widgetSet + " GWT module")
          s.log.debug("Running GWT compiler command: " + command)
          command !
        }
      },

    /**
     * Generates a combined widget set from all widget sets in the class path,
     * (including project sources?)
     * the file is then created in the main resource dir
     */
    vaadinGenerateWidgetSet <<= (dependencyClasspath in Compile,
                                 vaadinWidgetSet,
                                 javaSource in Compile,
                                 resourceDirectory in Compile,
                                 streams) map
      { (dependencyClasspath, widgetSet, javaSource, resourceDir, s) =>
        {
          s.log.info("Resource directory <" + resourceDir.absolutePath + ">")
          val cp = Seq(resourceDir.absolutePath, javaSource.absolutePath) ++ dependencyClasspath.map(_.data.absolutePath)
          val command = "java -cp " + cp.mkString(File.pathSeparator) + " com.vaadin.server.widgetsetutils.WidgetSetBuilder " + widgetSet
          s.log.info("Generating widgetset from classpath")
          s.log.debug("Running Vaadin command: " + command)
          command !
        }
      }
  )
}

