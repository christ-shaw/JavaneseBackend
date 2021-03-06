package online.javanese.model

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.*
import online.javanese.Html
import online.javanese.krud.kwery.Uuid

class Task(
        val basicInfo: BasicInfo,
        val heading: String,
        val condition: Html,
        val technicalInfo: TechnicalInfo,
        val sortIndex: Int
) {

    class BasicInfo(
            val id: Uuid,
            val lessonId: Uuid,
            val linkText: String,
            val urlPathComponent: String
    )

    class TechnicalInfo(
            val compiledClassName: String,
            val inputMethod: InputMethod,
            val initialCode: String,
            val codeToAppend: String,
            val allowSystemIn: Boolean,
            val checkRules: String,
            val expectedOutput: String,
            val timeLimit: Int, // seconds
            val memoryLimit: Int // MB
    )

    enum class InputMethod {
        MethodBody, // user inputs method body
        ClassBody,  // ... class body
        WholeClass  // ... whole class code, including imports
    }

}


private val _basic = Task::basicInfo

private val _id = Task.BasicInfo::id
private val _lessonId = Task.BasicInfo::lessonId
private val _linkText = Task.BasicInfo::linkText
private val _urlPathComponent = Task.BasicInfo::urlPathComponent

private val _tech = Task::technicalInfo


object TaskTable : Table<Task, Uuid>("tasks") {

    val Id by idCol(_id, _basic)
    val LessonId by uuidCol(_lessonId, _basic, name = "lessonId")
    val LinkText by linkTextCol(_linkText, _basic)
    val UrlPathComponent by urlSegmentCol(_urlPathComponent, _basic)

    val Heading by col(Task::heading, name = "heading")
    val Condition by col(Task::condition, name = "condition")

    val CompiledClassName by col(Task.TechnicalInfo::compiledClassName, _tech, name = "compiledClassName")
    val InputMethod by col(Task.TechnicalInfo::inputMethod, _tech, name = "inputMethod", default = Task.InputMethod.MethodBody)
    val InitialCode by col(Task.TechnicalInfo::initialCode, _tech, name = "initialCode")
    val CodeToAppend by col(Task.TechnicalInfo::codeToAppend, _tech, name = "codeToAppend")
    val AllowSystemIn by col(Task.TechnicalInfo::allowSystemIn, _tech, name = "allowSystemIn")
    val CheckRules by col(Task.TechnicalInfo::checkRules, _tech, name = "checkRules")
    val ExpectedOutput by col(Task.TechnicalInfo::expectedOutput, _tech, name = "expectedOutput")
    val TimeLimit by col(Task.TechnicalInfo::timeLimit, _tech, name = "timeLimit")
    val MemoryLimit by col(Task.TechnicalInfo::memoryLimit, _tech, name = "memoryLimit")

    val SortIndex by sortIndexCol(Task::sortIndex)


    override fun idColumns(id: Uuid): Set<Pair<Column<Task, *>, *>> =
            setOf(Id of id)

    override fun create(value: Value<Task>): Task = Task(
            basicInfo = Task.BasicInfo(
                    id = value of Id,
                    lessonId = value of LessonId,
                    linkText = value of LinkText,
                    urlPathComponent = value of UrlPathComponent
            ),
            heading = value of Heading,
            condition = value of Condition,
            technicalInfo = Task.TechnicalInfo(
                    compiledClassName = value of CompiledClassName,
                    inputMethod = value of InputMethod,
                    initialCode = value of InitialCode,
                    codeToAppend = value of CodeToAppend,
                    allowSystemIn = value of AllowSystemIn,
                    checkRules = value of CheckRules,
                    expectedOutput = value of ExpectedOutput,
                    timeLimit = value of TimeLimit,
                    memoryLimit = value of MemoryLimit
            ),
            sortIndex = value of SortIndex
    )

}


object BasicTaskInfoTable : Table<Task.BasicInfo, Uuid>("tasks") {

    val Id by idCol(_id)
    val LessonId by uuidCol(_lessonId, name = "lessonId")
    val LinkText by linkTextCol(_linkText)
    val UrlPathComponent by urlSegmentCol(_urlPathComponent)

    override fun idColumns(id: Uuid): Set<Pair<Column<Task.BasicInfo, *>, *>> =
            setOf(Id of id)

    override fun create(value: Value<Task.BasicInfo>): Task.BasicInfo = Task.BasicInfo(
            id = value of Id,
            lessonId = value of LessonId,
            linkText = value of LinkText,
            urlPathComponent = value of UrlPathComponent
    )

}


class TaskDao(session: Session) : AbstractDao<Task, Uuid>(session, TaskTable, { it.basicInfo.id }) {

    private val tableName = TaskTable.name
    private val idColName = TaskTable.Id.name
    private val lessonIdColName = TaskTable.LessonId.name
    private val basicColNames = """"id", "lessonId", "linkText", "urlSegment""""

    override val defaultOrder: Map<Column<Task, *>, OrderByDirection> =
            mapOf(TaskTable.SortIndex to OrderByDirection.ASC)

    fun findAllBasicSorted(lessonId: Uuid): List<Task.BasicInfo> =
            session.select(
                    sql = """SELECT $basicColNames FROM "$tableName" WHERE "$lessonIdColName" = :lessonId""",
                    parameters = mapOf("lessonId" to lessonId),
                    mapper = BasicTaskInfoTable.rowMapper()
            )

    fun findForLessonSorted(lessonId: Uuid): List<Task> =
            session.select(
                    sql = """SELECT * FROM "$tableName" WHERE "$lessonIdColName" = :lessonId""",
                    parameters = mapOf("lessonId" to lessonId),
                    mapper = TaskTable.rowMapper()
            )

    fun findById(taskId: Uuid) =
            session.select(
                    sql = """SELECT * FROM "$tableName" WHERE "$idColName" = :id LIMIT 1""",
                    parameters = mapOf("id" to taskId),
                    mapper = TaskTable.rowMapper()
            ).singleOrNull()

}


/*
CREATE TABLE public.tasks (
	"id" uuid NOT NULL,
	"lessonId" uuid NOT NULL,
	"linkText" varchar(256) NOT NULL,
	"urlSegment" varchar(64) NOT NULL,
	"heading" varchar(256) NOT NULL,
	"condition" text NOT NULL,
	"compiledClassName" varchar(64) NOT NULL,
	"inputMethod" varchar(64) NOT NULL,
	"initialCode" text NOT NULL,
	"codeToAppend" text NOT NULL,
	"allowSystemIn" bool NOT NULL,
	"checkRules" text NOT NULL,
	"expectedOutput" text NOT NULL,
	"timeLimit" int4 NOT NULL,
	"memoryLimit" int4 NOT NULL,
	"sortIndex" int4 NOT NULL,
	CONSTRAINT tasks_pk PRIMARY KEY (id),
	CONSTRAINT tasks_lessons_fk FOREIGN KEY ("lessonId") REFERENCES public.lessons(id)
)
WITH (
	OIDS=FALSE
) ;
CREATE INDEX tasks_urlsegment_idx ON public.tasks ("urlSegment") ;
 */
