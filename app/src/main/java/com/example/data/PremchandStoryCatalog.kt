package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AudioType
import com.example.model.Story
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object PremchandStoryCatalog {

    private const val TAG = "PremchandStoryCatalog"

    @Volatile
    private var cachedStories: List<Story>? = null
    private val paragraphsCache = mutableMapOf<String, List<String>>()
    private var appContext: Context? = null

    val fallbackStories: List<Story> = listOf(
        Story(
            id = "eidgah",
            titleHindi = "ईदगाह",
            titleEnglish = "Eidgah",
            description = "चार-पाँच साल के नन्हे हामिद और उसकी दादी अमीना के अगाध प्रेम और त्याग की अमर कहानी।",
            category = "बाल-मनोविज्ञान",
            estimatedReadingMinutes = 12,
            audioAsset = null,
            audioType = AudioType.TTS,
            hasAudio = true,
            isFeatured = true,
            paragraphs = listOf(
                "रमजान के पूरे तीस रोजों के बाद आज ईद आई है। कितना मनोहर, कितना सुहावना प्रभाव है!",
                "गाँव से मेला चला। बच्चों के साथ हामिद भी जा रहा था।",
                "हामिद ने दुकानदार से पूछा—यह चिमटा कितने का है? उसने दादी के लिए चिमटा खरीदा।"
            )
        ),
        Story(
            id = "poos_ki_raat",
            titleHindi = "पूस की रात",
            titleEnglish = "Poos Ki Raat",
            description = "कड़ाके की शीत में भारतीय किसान हल्कू और उसके वफादार श्वान जबरा की मार्मिक और यथार्थवादी गाथा।",
            category = "ग्रामीण यथार्थ",
            estimatedReadingMinutes = 10,
            audioAsset = null,
            audioType = AudioType.TTS,
            hasAudio = true,
            isFeatured = true,
            paragraphs = listOf(
                "हल्कू ने आकर अपनी स्त्री से कहा—सहना आया है, लाओ जो रुपये रखे हैं, उसे दे दूँ।",
                "पूस की अंधेरी रात। आकाश पर तारे भी ठिठुरते हुए मालूम होते थे।"
            )
        ),
        Story(
            id = "panch_parmeshwar",
            titleHindi = "पंच परमेश्वर",
            titleEnglish = "Panch Parmeshwar",
            description = "न्याय की गद्दी पर बैठा व्यक्ति निष्पक्ष होता है—अलगू चौधरी और जुम्मन शेख की मित्रता और न्याय की मिसाल।",
            category = "सामाजिक न्याय",
            estimatedReadingMinutes = 11,
            audioAsset = null,
            audioType = AudioType.TTS,
            hasAudio = true,
            isFeatured = true,
            paragraphs = listOf(
                "जुम्मन शेख और अलगू चौधरी में गाढ़ी मित्रता थी। साझे में खेती होती थी।",
                "बेटा, दोस्ती के लिए कोई अपना ईमान नहीं बेचता। पंच के दिल में खुदा बसता है।"
            )
        ),
        Story(
            id = "bade_bhai_sahab",
            titleHindi = "बड़े भाई साहब",
            titleEnglish = "Bade Bhai Sahab",
            description = "रटने की विद्या बनाम जीवन के व्यावहारिक अनुभव का अनोखा और हास्य-व्यंग्यपूर्ण यथार्थ।",
            category = "बाल-मनोविज्ञान",
            estimatedReadingMinutes = 11,
            audioAsset = null,
            audioType = AudioType.TTS,
            hasAudio = true,
            isFeatured = true,
            paragraphs = listOf(
                "मेरे भाई साहब मुझसे पाँच साल बड़े थे, लेकिन केवल तीन दरजे आगे।",
                "उनकी इस नसीहत ने मेरी आँखें खोल दीं। मुझे अपनी लघुता का अहसास हुआ।"
            )
        )
    )

    val stories: List<Story>
        get() = cachedStories ?: fallbackStories

    fun init(context: Context) {
        appContext = context.applicationContext
        if (cachedStories == null) {
            val loaded = loadCatalogFromAssets(context)
            if (loaded.isNotEmpty()) {
                cachedStories = loaded
            }
        }
    }

    private fun loadCatalogFromAssets(context: Context): List<Story> {
        return try {
            val inputStream = context.assets.open("stories_catalog.json")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val content = reader.use { it.readText() }
            val jsonArray = JSONArray(content)
            val list = mutableListOf<Story>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val titleHindi = obj.optString("titleHindi", "")
                val titleEnglish = obj.optString("titleEnglish", "")
                val description = obj.optString("description", "")
                val category = obj.optString("category", "मानवीय संवेदना")
                val estimatedReadingMinutes = obj.optInt("estimatedReadingMinutes", 8)
                val isFeatured = obj.optBoolean("isFeatured", false)

                list.add(
                    Story(
                        id = id,
                        titleHindi = titleHindi,
                        titleEnglish = titleEnglish,
                        description = description,
                        category = category,
                        estimatedReadingMinutes = estimatedReadingMinutes,
                        audioAsset = null,
                        audioType = AudioType.TTS,
                        hasAudio = true,
                        isFeatured = isFeatured,
                        paragraphs = emptyList() // Loaded on demand when opened to preserve memory
                    )
                )
            }
            if (list.isNotEmpty()) list else fallbackStories
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load stories_catalog.json, using fallback", t)
            fallbackStories
        }
    }

    fun getStoryById(id: String, context: Context? = null): Story? {
        val norm = id.replace("-", "").replace("_", "").lowercase()
        val baseStory = stories.find {
            it.id == id || it.id.replace("-", "").replace("_", "").lowercase() == norm
        } ?: return null

        val ctx = context ?: appContext
        val loadedParagraphs = if (baseStory.paragraphs.isNotEmpty()) {
            baseStory.paragraphs
        } else {
            val loaded = getParagraphsForStory(baseStory.id, ctx)
            if (loaded.isNotEmpty()) loaded else listOf(baseStory.description)
        }

        return baseStory.copy(paragraphs = loadedParagraphs)
    }

    fun getParagraphsForStory(storyId: String, context: Context? = null): List<String> {
        paragraphsCache[storyId]?.let { return it }

        val ctx = context ?: appContext ?: return emptyList()
        return try {
            val norm = storyId.replace("-", "_")
            val filename = "story_texts/$norm.json"
            val inputStream = ctx.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val content = reader.use { it.readText() }
            val jsonArray = JSONArray(content)
            val paragraphs = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val p = jsonArray.optString(i, "").trim()
                if (p.isNotEmpty()) {
                    paragraphs.add(p)
                }
            }
            if (paragraphs.isNotEmpty()) {
                paragraphsCache[storyId] = paragraphs
            }
            paragraphs
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load paragraphs for story: $storyId", t)
            // Fallback: check fallbackStories for this story
            val fallback = fallbackStories.find { it.id == storyId }
            fallback?.paragraphs ?: emptyList()
        }
    }

    fun getAudioStories(): List<Story> = stories.filter { it.hasAudio }
    fun getBundledAudioStories(): List<Story> = stories
    fun getTtsStories(): List<Story> = stories
}
