package com.threes.game.data

import android.content.Context
import android.content.SharedPreferences
import com.threes.game.model.Tile
import org.json.JSONArray
import org.json.JSONObject

data class SavedGame(
    val grid: Array<Array<Tile?>>,
    val nextTile: Tile,
    val deck: List<Int>,
    val score: Int,
    val maxTile: Int,
    val moveCount: Int
)

class PreferencesRepository(context: Context) {

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

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    fun updateHighScoreIfHigher(score: Int): Boolean {
        if (score > highScore) {
            highScore = score
            return true
        }
        return false
    }

    var isNightMode: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var isUndoEnabled: Boolean
        get() = prefs.getBoolean(KEY_UNDO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_UNDO_ENABLED, value).apply()

    fun getUnlockedCharacters(): Set<Int> {
        val stringSet = prefs.getStringSet(
            KEY_UNLOCKED_CHARACTERS,
            setOf("1", "2", "3")
        ) ?: setOf("1", "2", "3")
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun unlockCharacter(value: Int) {
        val current = getUnlockedCharacters().toMutableSet()
        current.add(value)
        prefs.edit().putStringSet(
            KEY_UNLOCKED_CHARACTERS,
            current.map { it.toString() }.toSet()
        ).apply()
    }

    fun saveGame(savedGame: SavedGame) {
        try {
            val json = JSONObject()
            json.put("score", savedGame.score)
            json.put("maxTile", savedGame.maxTile)
            json.put("moveCount", savedGame.moveCount)

            // Save next tile
            val nextJson = JSONObject()
            nextJson.put("id", savedGame.nextTile.id)
            nextJson.put("value", savedGame.nextTile.value)
            nextJson.put("isBonus", savedGame.nextTile.isBonus)
            json.put("nextTile", nextJson)

            // Save deck
            val deckArray = JSONArray()
            savedGame.deck.forEach { deckArray.put(it) }
            json.put("deck", deckArray)

            // Save grid
            val gridArray = JSONArray()
            for (r in 0 until 4) {
                val rowArray = JSONArray()
                for (c in 0 until 4) {
                    val tile = savedGame.grid[r][c]
                    if (tile != null) {
                        val tileObj = JSONObject()
                        tileObj.put("id", tile.id)
                        tileObj.put("value", tile.value)
                        tileObj.put("isBonus", tile.isBonus)
                        rowArray.put(tileObj)
                    } else {
                        rowArray.put(JSONObject.NULL)
                    }
                }
                gridArray.put(rowArray)
            }
            json.put("grid", gridArray)

            prefs.edit().putString(KEY_SAVED_GAME, json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadGame(): SavedGame? {
        val raw = prefs.getString(KEY_SAVED_GAME, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val score = json.getInt("score")
            val maxTile = json.getInt("maxTile")
            val moveCount = json.getInt("moveCount")

            val nextObj = json.getJSONObject("nextTile")
            val nextTile = Tile(
                id = nextObj.getLong("id"),
                value = nextObj.getInt("value"),
                isBonus = nextObj.optBoolean("isBonus", false)
            )

            val deckArray = json.getJSONArray("deck")
            val deck = mutableListOf<Int>()
            for (i in 0 until deckArray.length()) {
                deck.add(deckArray.getInt(i))
            }

            val gridArray = json.getJSONArray("grid")
            val grid = Array(4) { Array<Tile?>(4) { null } }
            for (r in 0 until 4) {
                val rowArray = gridArray.getJSONArray(r)
                for (c in 0 until 4) {
                    if (!rowArray.isNull(c)) {
                        val tileObj = rowArray.getJSONObject(c)
                        grid[r][c] = Tile(
                            id = tileObj.getLong("id"),
                            value = tileObj.getInt("value"),
                            isBonus = tileObj.optBoolean("isBonus", false)
                        )
                    }
                }
            }

            SavedGame(
                grid = grid,
                nextTile = nextTile,
                deck = deck,
                score = score,
                maxTile = maxTile,
                moveCount = moveCount
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearSavedGame() {
        prefs.edit().remove(KEY_SAVED_GAME).apply()
    }
}
