import laika.ast.LengthUnit.px
import laika.ast.Path.Root
import laika.ast._
import laika.helium.Helium
import laika.helium.config.{Favicon, HeliumIcon, IconLink, ReleaseInfo, Teaser, TextLink, VersionMenu}
import laika.config.*
import laika.sbt.LaikaConfig
import laika.theme.ThemeProvider

object ManualSettings {

  private object links {
    val github = "https://github.com/FabioPinheiro/scala-did"
    val javadoc = "https://javadoc.io/doc/app.fmgp"
    val demo = "https://did.fmgp.app"
    val discord = "https://discord.gg/atala"
  }

  // val config: LaikaConfig = LaikaConfig.defaults
  //   .withConfigValue(LinkConfig.empty.addApiLinks(ApiLinks(paths.apiURL)))
  //   .withConfigValue(LinkValidation.Global(Seq(Root / "api")))
  //   .withConfigValue(
  //     Selections(
  //       SelectionConfig(
  //         "config",
  //         ChoiceConfig("sbt", "sbt Plugin"),
  //         ChoiceConfig("library", "Library API")
  //       ).withSeparateEbooks
  //     )
  //   )
  //   .withConfigValue(LaikaKeys.artifactBaseName, s"laika-${versions.current.displayValue}")
  //   .withConfigValue(LaikaKeys.versioned, true)

  def themeProvider(version: String): ThemeProvider = Helium.defaults.all
    .metadata(
      title = Some("Scala-DID"),
      description = Some("Scala-did a DID Comm library for scala"),
      version = Some(version),
      language = Some("en")
    )
    // .all.tableOfContent("Table of Content", depth = 4)
    .site
    .topNavigationBar(
      navLinks = Seq(
        IconLink.external(links.github, HeliumIcon.github),
        IconLink.external(links.javadoc, HeliumIcon.api),
        IconLink.external(links.demo, HeliumIcon.demo),
        IconLink.external(links.discord, HeliumIcon.chat)
      ),
    )
    .site
    .footer(
      TemplateString("Documentation website for "),
      Emphasized("Scala-did"),
      TemplateString(" a DID Comm library for scala.")
    )
    // .site
    // .baseURL(paths.siteBaseURL)
    .site
    .favIcons(Favicon.external("https://did.fmgp.app/favicon.ico", "32x32"))
    .site
    .pageNavigation(sourceBaseURL = Some(links.github + "/blob/master/docs/src"))
    // .site
    // .downloadPage("Documentation Downloads", Some(text.downloadDesc))
    // .site
    // .versions(versions.config)
    .site
    .landingPage(
      // logo = Some(
      //   Image.internal(
      //     paths.logo,
      //     width = Some(px(327)),
      //     height = Some(px(393)),
      //     alt = Some("Laika Logo")
      //   )
      // ),
      subtitle = Some("A Scala & Scala.js implementation of DID and DID Comm messaging spec"),
      latestReleases = Seq(
        ReleaseInfo("Latest Release", "0.1.0-M15")
      ),
      license = Some("Apache 2.0"),
      documentationLinks = Seq(
        TextLink.internal(Root / "readme.md", "README"),
        TextLink.internal(Root / "quickstart-get-started.md", "Quickstart"),
        TextLink.internal(Root / "quickstart-basic-examples.md", "Examples"),
        TextLink.internal(Root / "test-coverage.md", "Test Coverage"),
        TextLink.internal(Root / "limitations.md", "Limitations"),
        TextLink.internal(Root / "troubleshooting.md", "Troubleshooting"),
        TextLink.internal(Root / "external-documentation.md", "Tools and Links"),
      ),
      projectLinks = Seq(
        TextLink.external(links.github, "Source on GitHub"),
        TextLink.external(links.demo, "Live Demo"),
        TextLink.external(links.javadoc, "API Javadoc"),
      ),
      teasers = Seq(
        Teaser(
          "No External Tools",
          "Easy setup without any external tools or languages and only minimal library dependencies."
        ),
        Teaser(
          "Purely Functional",
          "Fully referentially transparent, no exceptions or runtime reflection and integration with ZIO for polymorphic effect handling."
        ),
        Teaser(
          "Highly Extensible",
          "Process the DID Comm Message easy to modify any of the layers and service of the Framework"
        )
      )
    )
    .build,

}
