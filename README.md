# Webby: A Web Application Runner for Maven WAR Projects powered by [Cargo](http://cargo.codehaus.org/).

To ease running and debugging of web applications created by the Maven WAR Plugin, a feature named "Web Application Launcher" (nickname "Webby") can be installed from the update site (https://repository.takari.io/content/sites/m2e.extras/m2eclipse-webby/0.2.2/N/). Once installed, the feature provides a new launch configuration type to run or debug a Maven WAR project in a servlet container. The Run Jetty Run plugin for Eclipse served as inspiration for Webby.

Warning: Webby cannot be installed side-by-side with the m2e-wtp extension. Trying to do so will render one or both of the extensions unusable.

![webby-launch-config](http://takari.io/assets/webby/webby-launch-config.png)

The primary focus of Webby is to allow running WAR projects without the time consumption of creating the actual WAR file, yet supporting advanced features like resource filtering and overlays while the developer makes incremental changes to the project within the IDE.

Right now, Webby supports running applications in Jetty 6.x/7.x/8.x, TomEE 1.x and Apache Tomcat 5.x/6.x/7.x/8.x. The developer selects the container by pointing the launch configuration at a local directory where the container has been previously installed. In the case of Jetty, the developer can also choose to use embedded Jetty distributions that are included in Webby itself, assuming the corresponding add-on features for Webby were installed. In general, the launch configuration dialog should be rather self-explanatory. The "System Properties Files" field in the "JRE" tab might deserve some details though. Each line of this multi-line text field gives the path to a properties file, empty/blank lines are ignored. The properties files are read in declared order and merged, with latter files taking precedence in case of conflicts. The resulting set of properties is added to the system properties of the launched JVM.

Given the similarity with the jetty:run goal that developers often use on the commandline to debug their application, Webby allows to initialize a launch configuration from the configuration of the jetty-maven-plugin or maven-jetty-plugin, respectively. So when you already use jetty:run in your POM and now want to try Webby, open the context menu for the WAR project in Eclipse and choose the command "Debug as Webby" from the "Debug As" sub menu. The resulting launch configuration will pick an embedded Jetty container according to the plugin version and apply the port, context path and system properties configured for Jetty.

Once a WAR project has been launched by Webby, it will be listed in Webby's "Web Apps" view. You can open this view via the menu command "Window" > "Show View" > "Other...". This view simply allows one to open the web application in a browser and to stop it. For simple applications that don't require an orderly shutdown, you could also stop the application by terminating the JVM running it, e.g. via the "Terminate" button in the console view created for the web application.

![webby-view](http://takari.io/assets/webby/webby-view.png)

You will get the most out of Webby when you launch your web applications in debug mode. Debug mode enables Eclipse's hot code replace feature that allows to incorporate many changes to Java sources like servlets on the fly into the running web application without restarting it.

# Contribution
When submitting patches, please follow the existing code style. The corresponding formatter settings for Eclipse can
be imported from the codestyle.xml file found in the root directory.

Any patches submitted will need to be accompanied by a signed Contributor License Agreement (CLA). If you have not
aleady signed this CLA, please download the [Takari Contributor License Agreement](http://takari.io/support/TakariCLA.pdf)
and follow its instructions.

# Update Site

You can find the latest build of Webby here:

https://repository.takari.io/content/sites/m2e.extras/m2eclipse-webby/0.2.2/N/

#License

[Eclipse Public License, v1.0](http://www.eclipse.org/legal/epl-v10.html)
