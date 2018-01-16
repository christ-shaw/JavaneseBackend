package online.javanese.page

import kotlinx.html.*
import online.javanese.locale.Language
import online.javanese.model.Meta

interface Layout : (HTML, Layout.Page) -> Unit {

    interface Page {
        val meta: Meta
        fun additionalHeadMarkup(head: HEAD)
        fun bodyMarkup(body: BODY)
        fun scripts(body: BODY)
    }

}

class MainLayout(
        private val static: String,
        private val mainScript: String,
        private val language: Language
) : Layout {

    override fun invoke(root: HTML, page: Layout.Page) = with(root) {
        head {
            unsafe {
                +"\n    <meta charset=\"utf-8\" />"
                +"\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />"
            }

            title("${page.meta.title} — ${language.siteTitle}")

            // Tenor Sans, PT Sans, Material Icons; mdl, dialog polyfill, highlight darkula, custom styles, sandbox, trace
            styleLink("$static/main.min.css?1")

            link(rel = "alternate", type = "application/rss+xml", href = "/articles.rss") {
                attributes["title"] = "Статьи"
            }

            meta(name = "description", content = page.meta.description)
            meta(name = "keywords", content = page.meta.keywords)

            page.additionalHeadMarkup(this)
        }

        body {
            page.bodyMarkup(this)

            button(classes = "mdl-button mdl-js-button mdl-button--fab") {
                id = "up-button"

                i("material-icons") { +"keyboard_arrow_up" }
            }

            // Vue.js, Zepto, Material Design Lite, dialog polyfill, highlight.js, trace, scroll to top,
            // blur link on click, mdl tabs fix to work with url #fragment, async form handling
            script(src = "$static/$mainScript")

            div(classes = "mdl-js-snackbar mdl-snackbar") {
                id = "toast-container"

                div(classes="mdl-snackbar__text")
                button(classes="mdl-snackbar__action")
            }

            page.scripts(this)
        }
    }

}
