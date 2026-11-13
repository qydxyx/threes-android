package com.threes.game.data

import android.content.Context
import android.content.SharedPreferences
import com.threes.game.engine.GameSnapshot
import com.threes.game.model.Tile
import org.json.JSONArray
import org.json.JSONObject

data class SavedGame(
    val grid: List<List<Tile?>>,
    val nextTile: Tile,
    val deck: List<Int>,
    val score: Int,
    val maxTile: Int,
    val moveCount: Int,
    val isNewRecord: Boolean = false,
    val baselineHighScore: Int = 0,
    val previousSnapshot: GameSnapshot? = null
)

interface GameStorage {
    var highScore: Int
    var isNightMode: Boolean
    var isSoundEnabled: Boolean
    var isHapticsEnabled: Boolean
    var isUndoEnabled: Boolean
    fun getUnlockedCharacters(): Set<Int>
    fun unlockCharacter(value: Int)
    fun saveGame(savedGame: SavedGame)
    fun loadGame(): SavedGame?
    fun clearSavedGame()
}

class PreferencesRepository(context: Context) : GameStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("threes_game_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_UNLOCKED_CHARACTERS = "unlocked_characters"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_UNDO_ENABLED = "undo_enabled"
        private const val KEY_SAVED_GAME = "saved_game_state"
    }

    override var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    fun updateHighScoreIfHigher(score: Int): Boolean {
        if (score > highScore) {
            highScore = score
            return true
        }
        return false
    }

    override var isNightMode: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()

    override var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    override var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    override var isUndoEnabled: Boolean
        get() = prefs.getBoolean(KEY_UNDO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_UNDO_ENABLED, value).apply()

    override fun getUnlockedCharacters(): Set<Int> {
        val stringSet = prefs.getStringSet(
            KEY_UNLOCKED_CHARACTERS,
            setOf("1", "2", "3")
        ) ?: setOf("1", "2", "3")
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    override fun unlockCharacter(value: Int) {
        val current = getUnlockedCharacters().toMutableSet()
        if (current.add(value)) {
            prefs.edit().putStringSet(
                KEY_UNLOCKED_CHARACTERS,
                current.map { it.toString() }.toSet()
            ).apply()
        }
    }

    override fun saveGame(savedGame: SavedGame) {
        try {
            val json = JSONObject()
            json.put("score", savedGame.score)
            json.put("maxTile", savedGame.maxTile)
            json.put("moveCount", savedGame.moveCount)
            json.put("isNewRecord", savedGame.isNewRecord)
            json.put("baselineHighScore", savedGame.baselineHighScore)

            json.put("nextTile", tileToJson(savedGame.nextTile, includeBonusCandidates = true))
            json.put("deck", intListToJsonArray(savedGame.deck))
            json.put("grid", gridToJsonArray(savedGame.grid))

            savedGame.previousSnapshot?.let { snap ->
                val snapJson = JSONObject()
                snapJson.put("score", snap.score)
                snapJson.put("maxTileValue", snap.maxTileValue)
                snapJson.put("moveCount", snap.moveCount)
                snapJson.put("nextTile", tileToJson(snap.nextTile, includeBonusCandidates = true))
                snapJson.put("deck", intListToJsonArray(snap.deck))
                snapJson.put("grid", gridToJsonArray(snap.grid))
                json.put("previousSnapshot", snapJson)
            }

            prefs.edit().putString(KEY_SAVED_GAME, json.toString()).apply()
        } catch (_: Exception) {}
    }

    override fun loadGame(): SavedGame? {
        val raw = prefs.getString(KEY_SAVED_GAME, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val score = json.getInt("score")
            val maxTile = json.getInt("maxTile")
            val moveCount = json.getInt("moveCount")
            val isNewRecord = json.optBoolean("isNewRecord", false)
            val baselineHighScore = json.optInt("baselineHighScore", highScore)

            val nextTile = jsonToTile(json.getJSONObject("nextTile"))
            val deck = jsonArrayToIntList(json.getJSONArray("deck"))
            val grid = jsonArrayToGrid(json.getJSONArray("grid"))

            val previousSnapshot = json.optJSONObject("previousSnapshot")?.let { snapJson ->
                GameSnapshot(
                    grid = jsonArrayToGrid(snapJson.getJSONArray("grid")),
                    score = snapJson.getInt("score"),
                    nextTile = jsonToTile(snapJson.getJSONObject("nextTile")),
                    deck = jsonArrayToIntList(snapJson.getJSONArray("deck")),
                    maxTileValue = snapJson.getInt("maxTileValue"),
                    moveCount = snapJson.getInt("moveCount")
                )
            }

            SavedGame(
                grid = grid,
                nextTile = nextTile,
                deck = deck,
                score = score,
                maxTile = maxTile,
                moveCount = moveCount,
                isNewRecord = isNewRecord,
                baselineHighScore = baselineHighScore,
                previousSnapshot = previousSnapshot
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun clearSavedGame() {
        prefs.edit().remove(KEY_SAVED_GAME).apply()
    }

    private fun tileToJson(tile: Tile, includeBonusCandidates: Boolean): JSONObject {
        return JSONObject().apply {
            put("id", tile.id)
            put("value", tile.value)
            put("isBonus", tile.isBonus)
            if (includeBonusCandidates) {
                put("bonusCandidates", intListToJsonArray(tile.bonusCandidates))
            }
        }
    }

    private fun jsonToTile(obj: JSONObject): Tile {
        val candidates = mutableListOf<Int>()
        val candidatesArr = obj.optJSONArray("bonusCandidates")
        if (candidatesArr != null) {
            for (i in 0 until candidatesArr.length()) {
                candidates.add(candidatesArr.getInt(i))
            }
        }
        return Tile(
            id = obj.getLong("id"),
            value = obj.getInt("value"),
            isBonus = obj.optBoolean("isBonus", false),
            bonusCandidates = candidates
        )
    }

    private fun intListToJsonArray(list: List<Int>): JSONArray {
        return JSONArray().apply {
            list.forEach { put(it) }
        }
    }

    private fun jsonArrayToIntList(array: JSONArray): List<Int> {
        return (0 until array.length()).map { array.getInt(it) }
    }

    private fun gridToJsonArray(grid: List<List<Tile?>>): JSONArray {
        val gridArray = JSONArray()
        for (r in 0 until 4) {
            val rowArray = JSONArray()
            for (c in 0 until 4) {
                val tile = grid.getOrNull(r)?.getOrNull(c)
                if (tile != null) {
                    rowArray.put(tileToJson(tile, includeBonusCandidates = false))
                } else {
                    rowArray.put(JSONObject.NULL)
                }
            }
            gridArray.put(rowArray)
        }
        return gridArray
    }

    private fun jsonArrayToGrid(gridArray: JSONArray): List<List<Tile?>> {
        return (0 until 4).map { r ->
            val rowArray = gridArray.getJSONArray(r)
            (0 until 4).map { c ->
                if (!rowArray.isNull(c)) {
                    jsonToTile(rowArray.getJSONObject(c))
                } else {
                    null
                }
            }
        }
    }
}
