package online.javanese

import com.github.andrewoma.kwery.core.DefaultSession
import com.github.andrewoma.kwery.core.dialect.PostgresDialect
import nz.net.ultraq.thymeleaf.LayoutDialect
import online.javanese.exception.NotFoundException
import online.javanese.model.*
import online.javanese.repository.*
import online.javanese.route.PageHandler
import online.javanese.route.TopLevelRouteHandler
import online.javanese.template.ArticlesPageBinding
import online.javanese.template.IndexPageBinding
import online.javanese.template.PageBinding
import online.javanese.template.TreePageBinding
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.content.files
import org.jetbrains.ktor.content.static
import org.jetbrains.ktor.content.staticRootFolder
import org.jetbrains.ktor.features.StatusPages
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import org.thymeleaf.templatemode.TemplateMode
import java.io.File
import java.io.FileInputStream
import java.sql.DriverManager
import java.util.*

object JavaneseServer {

    @JvmStatic
    fun main(args: Array<String>) {

        val (dbName, dbProps, localStaticDir, exposedStaticDir) = Properties().let {
            it.load(FileInputStream("local.properties"))

            Quadruple(
                    it["database"] as String,
                    Properties().also { db ->
                        db["user"] = it["user"] as String
                        db["password"] = it["password"] as String
                    },
                    it["localStaticDir"] as String?, // e. g. '/home/<user>/IdeaProjects/javanese/src/main/resources/static' for development
                    it["exposedStaticDir"] as String // e. g. '/static' for development, 'http://static.javanese.online/' for production
            )
        }


        org.postgresql.Driver::class.java
        val connection = DriverManager.getConnection("jdbc:postgresql:$dbName", dbProps)

        val session = DefaultSession(connection, PostgresDialect())

        val templateEngine = TemplateEngine(
                templateResolver = ClassLoaderTemplateResolver(
                        classLoader = javaClass.classLoader,
                        prefix = "/templates/",
                        suffix = ".html",
                        templateMode = TemplateMode.HTML,
                        charset = Charsets.UTF_8
                ),
                messageResolver = MessageResolver(
                        stream = javaClass.getResourceAsStream("/locale/messages_ru.properties")
                ),
                dialects = *arrayOf(LayoutDialect())
        )

        val pageDao = PageDao(session)
        val courseDao = CourseDao(session)
        val chapterDao = ChapterDao(session)
        val lessonDao = LessonDao(session)
        val taskDao = TaskDao(session)
        val articleDao = ArticleDao(session)

        val pageRepo = PageRepository(pageDao)
        val taskRepo = TaskRepository(taskDao)
        val lessonRepo = LessonRepository(lessonDao, taskRepo)
        val chapterRepo = ChapterRepository(chapterDao, lessonRepo)
        val courseRepo = CourseRepository(courseDao, chapterRepo)
        val articleRepo = ArticleRepository(articleDao)

        val locale = Locale.Builder().setLanguage("ru").setScript("Cyrl").build()
        val indexPageBinding = IndexPageBinding(exposedStaticDir, templateEngine, locale)
        val treePageBinding = TreePageBinding(exposedStaticDir, templateEngine, locale)
        val articlesPageBinding = ArticlesPageBinding(exposedStaticDir, templateEngine, locale)
        val pageBinding = PageBinding(exposedStaticDir, templateEngine, locale)

        val topLevelRoute =
                TopLevelRouteHandler(
                        pageRepo,
                        PageHandler(
                                courseRepo, articleRepo,
                                indexPageBinding, treePageBinding, articlesPageBinding, pageBinding
                        )
                )

        embeddedServer(Netty, 8080) {
            routing {

                // todo: addresses as objects

                get("/") { topLevelRoute(call, "") }
                get("/{query}/") { topLevelRoute(call, call.parameters["query"]!!) }

                install(StatusPages) {
                    exception<NotFoundException> {
                        call.respondText(it.message, ContentType.Text.Plain, HttpStatusCode.NotFound) // todo: error pages
                    }
                }

                if (localStaticDir != null) {
                    static(exposedStaticDir) {
                        val localStaticDirFile = File(localStaticDir)
                        staticRootFolder = localStaticDirFile.parentFile
                        files(localStaticDirFile.name)
                    }
                }
            }
        }.start(wait = true)
    }

}

// todo: rename all 'h1's to 'heading'
// todo: urlPathComponent -> urlComponent or what??
