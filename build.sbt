name := "AkkaGC"

version := "1.0"

scalaVersion := "2.11.8"

lazy val akkaVersion = "2.4.0"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % akkaVersion

//  "-XX:SurvivorRatio=12", "-XX:NewRatio=1"
// "-XX:CMSInitiatingOccupancyFraction=40", "-XX:+UseCMSInitiatingOccupancyOnly"
fork in run := true
//javaOptions in run ++= Seq("-Xms4048m", "-Xmx4048m", "-XX:+UseConcMarkSweepGC","-XX:+PrintPromotionFailure", "-Xloggc:gc_cms_4g_5_5000_1000_60_1.log", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-XX:+PrintGCTimeStamps")
javaOptions in run ++= Seq("-Xms10048m", "-Xmx10048m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UnlockExperimentalVMOptions", "-XX:G1MixedGCCountTarget=16", "-Xloggc:gc_g1_10g_85_5_5000_1000_120_100ms.log", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps", "-XX:+PrintGCTimeStamps")