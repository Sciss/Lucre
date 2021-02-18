lazy val baseName         = "Lucre"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "4.4.1"
lazy val mimaVersion      = "4.4.0"

lazy val deps = new {
  val base = new {
    val serial        = "2.0.1"
  }
  val adjunct = new {
    val numbers       = "0.2.1"
  }
  val core = new {
    val equal         = "0.1.6"
    val log           = "0.1.1"
    val model         = "0.3.5"
    val scalaSTM      = "0.11.0"
  }
  val expr = new {
    def equal: String = core.equal
    val asyncFile     = "0.1.2"
    val span          = "2.0.2"
  }
  val confluent = new {
    val finger        = "1.5.5"
  }
  val bdb = new {
    val sleepy7       = "7.5.11"  // Apache // Java 8+ required
  }
  val test = new {
    val scalaTest     = "3.2.4"
  }
}

lazy val commonJvmSettings = Seq(
  crossScalaVersions  := Seq("3.0.0-RC1", "2.13.4", "2.12.13"),
)

// sonatype plugin requires that these are in global
ThisBuild / version      := projectVersion
ThisBuild / organization := "de.sciss"

lazy val commonSettings = Seq(
//  version             := projectVersion,
//  organization        := "de.sciss",
  description         := "Extension of Scala-STM, adding optional durability layer, and providing API for confluent and reactive event layers",
  homepage            := Some(url(s"https://git.iem.at/sciss/$baseName")),
  scalaVersion        := "2.13.4",
  scalacOptions      ++= Seq(
    "-deprecation", "-unchecked", "-feature", "-encoding", "utf8"
  ),
  scalacOptions ++= {
    if (isDotty.value) Nil else Seq("-Xlint", "-Xsource:2.13")
  },
  scalacOptions in (Compile, compile) ++= {
    val jdkGt8 = scala.util.Properties.isJavaAtLeast("9")
    val dot    = isDotty.value
    // note: https://github.com/lampepfl/dotty/issues/8634 
    if (!dot && jdkGt8) Seq("-release", "8") else Nil
  }, // JDK >8 breaks API; skip scala-doc
  scalacOptions      ++= {
    val dot    = isDotty.value
    if (dot || (loggingEnabled && isSnapshot.value)) Nil else Seq("-Xelide-below", "INFO")     // elide debug logging!
  },
  sources in (Compile, doc) := {
    if (isDotty.value) Nil else (sources in (Compile, doc)).value // dottydoc is pretty much broken
  },
  testOptions in Test += Tests.Argument("-oDF"),   // ScalaTest: durations and full stack traces
  parallelExecution in Test := false,
  concurrentRestrictions in Global ++= Seq(
    Tags.limitAll(2), Tags.limit(Tags.Test, 1) // with cross-builds we otherwise get OutOfMemoryError
  ),
  libraryDependencies += {
    "org.scalatest" %%% "scalatest" % deps.test.scalaTest % Test
  },
  licenses := Seq(agpl),
) ++ publishSettings

lazy val agpl = "AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")

// i.e. root = full sub project. if you depend on root, will draw all sub modules.
lazy val root = project.withId(baseNameL).in(file("."))
  .aggregate(
    base      .jvm, base      .js,
    adjunct   .jvm, adjunct   .js,
    geom      .jvm, geom      .js,
    data      .jvm, data      .js,
    core      .jvm, core      .js,
    expr0     .jvm, expr0     .js,
    expr1     .jvm, expr1     .js,
    expr      .jvm, expr      .js,
    confluent .jvm, confluent .js,
    tests     .jvm, tests     .js,
    bdb,
    testsJVM,
  )
//  .dependsOn(base, adjunct, geom, data, core, expr, confluent, bdb)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    publishArtifact in (Compile, packageBin) := false, // there are no binaries
    publishArtifact in (Compile, packageDoc) := false, // there are no javadocs
    publishArtifact in (Compile, packageSrc) := false, // there are no sources
    mimaFailOnNoPrevious := false
  )

lazy val base = crossProject(JVMPlatform, JSPlatform).in(file("base"))
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-base",
    libraryDependencies ++= Seq(
      "de.sciss" %%% "serial" % deps.base.serial,
    ),
  )

lazy val geom = crossProject(JVMPlatform, JSPlatform).in(file("geom"))
//  .dependsOn(base)    // XXX TODO --- this is just because of new serializers
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-geom",
    libraryDependencies ++= Seq(
      "de.sciss" %%% "serial" % deps.base.serial
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-geom" % mimaVersion)
  )

lazy val adjunct = crossProject(JVMPlatform, JSPlatform).in(file("adjunct"))
  .dependsOn(base)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-adjunct",
    libraryDependencies ++= Seq(
      "de.sciss" %%% "numbers" % deps.adjunct.numbers
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-adjunct" % mimaVersion)
  )

lazy val data = crossProject(JVMPlatform, JSPlatform).in(file("data"))
  .dependsOn(base, geom)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-data",
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-data" % mimaVersion)
  )

lazy val core = crossProject(JVMPlatform, JSPlatform).in(file("core"))
  .dependsOn(data)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-core",
    libraryDependencies ++= Seq(
      "de.sciss"      %%% "equal"     % deps.core.equal, // % Provided, -- no longer provided thanks to macros gone in Dotty
      "de.sciss"      %%% "log"       % deps.core.log,
      "de.sciss"      %%% "model"     % deps.core.model,
      "org.scala-stm" %%% "scala-stm" % deps.core.scalaSTM
    ),
    buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) {
        case (k, opt) => k -> opt.get
      },
      BuildInfoKey.map(licenses) {
        case (_, Seq((lic, _))) => "license" -> lic
      }
    ),
    buildInfoPackage := "de.sciss.lucre",
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-core" % mimaVersion)
  )

// Dotty has currently cycle problems compiling F-bounded types.
// A work-around is to split the sources.

lazy val expr0 = crossProject(JVMPlatform, JSPlatform).in(file("expr0"))
  .dependsOn(core, adjunct)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-expr0",
    libraryDependencies ++= Seq(
      "de.sciss" %%% "asyncfile" % deps.expr.asyncFile,
      "de.sciss" %%% "equal"     % deps.expr.equal, // % Provided, -- no longer provided thanks to macros gone in Dotty
      "de.sciss" %%% "span"      % deps.expr.span,
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-expr0" % mimaVersion)
  )

lazy val expr1 = crossProject(JVMPlatform, JSPlatform).in(file("expr1"))
  .dependsOn(expr0)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-expr1",
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-expr1" % mimaVersion)
  )

lazy val expr = crossProject(JVMPlatform, JSPlatform).in(file("expr"))
  .dependsOn(expr0, expr1)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-expr",
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-expr" % mimaVersion)
  )

lazy val confluent = crossProject(JVMPlatform, JSPlatform).in(file("confluent"))
  .dependsOn(core)
  .settings(commonSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-confluent",
    libraryDependencies ++= Seq(
      "de.sciss" %%% "fingertree" % deps.confluent.finger
    ),
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-confluent" % mimaVersion)
  )

// JVM only
lazy val bdb = project.withId(s"$baseNameL-bdb").in(file("bdb"))
  .dependsOn(core.jvm)
  .settings(commonSettings)
  .settings(commonJvmSettings)
  .settings(
    libraryDependencies += "de.sciss" % "bdb-je" % deps.bdb.sleepy7,
    mimaPreviousArtifacts := Set("de.sciss" %% s"$baseNameL-bdb" % mimaVersion)
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform).in(file("tests"))
  .dependsOn(core, expr, confluent)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .jvmSettings(commonJvmSettings)
  .settings(
    name := s"$baseName-tests",
    publishArtifact in (Compile, packageBin) := false, // there are no binaries
    publishArtifact in (Compile, packageDoc) := false, // there are no javadocs
    publishArtifact in (Compile, packageSrc) := false, // there are no sources
  )

// XXX TODO. we could use `.jvmConfigure(_.dependsOn(bdb))` for `tests` instead
lazy val testsJVM = project.in(file("testsJVM"))
  .dependsOn(core.jvm, expr.jvm, confluent.jvm, bdb)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(commonJvmSettings)
  .settings(
    name := s"$baseName-testsJVM",
    publishArtifact in (Compile, packageBin) := false, // there are no binaries
    publishArtifact in (Compile, packageDoc) := false, // there are no javadocs
    publishArtifact in (Compile, packageSrc) := false, // there are no sources
  )

lazy val loggingEnabled = true  // only effective for snapshot versions

// ---- publishing ----

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    val h = "git.iem.at"
    val a = s"sciss/$baseName"
    Some(ScmInfo(url(s"https://$h/$a"), s"scm:git@$h:$a.git"))
  },
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishArtifact := false,
)
