package com.threes.game.ui.characters

import androidx.compose.ui.graphics.Color
import com.threes.game.ui.theme.*

data class CharacterInfo(
    val value: Int,
    val name: String,
    val title: String,
    val quote: String,
    val description: String,
    val primaryColor: Color,
    val textColor: Color,
    val edgeColor: Color
)

object CharacterRegistry {
    val characters: Map<Int, CharacterInfo> = mapOf(
        1 to CharacterInfo(
            value = 1,
            name = "Uno",
            title = "The Energetic One",
            quote = "I'm number one!",
            description = "Bright and energetic. Always looking for a 2 to make something greater.",
            primaryColor = Card1Bg,
            textColor = Card1Text,
            edgeColor = Card1Edge
        ),
        2 to CharacterInfo(
            value = 2,
            name = "Dos",
            title = "The Passionate Two",
            quote = "Two's company!",
            description = "Fiery and bold. Can't wait to team up with 1 to become a 3.",
            primaryColor = Card2Bg,
            textColor = Card2Text,
            edgeColor = Card2Edge
        ),
        3 to CharacterInfo(
            value = 3,
            name = "Trey",
            title = "The Optimist",
            quote = "Three is a magic number!",
            description = "The cheerful foundation of the kingdom. Always smiling with unyielding joy.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        6 to CharacterInfo(
            value = 6,
            name = "Todd",
            title = "The Scholar",
            quote = "According to my calculations...",
            description = "Wears stylish glasses and a headband. Loves studying grid dynamics.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        12 to CharacterInfo(
            value = 12,
            name = "Terry",
            title = "The Cool Rebel",
            quote = "Turn it up to twelve!",
            description = "Rockstar attitude with fiery hair and shades. Never plays by the rules.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        24 to CharacterInfo(
            value = 24,
            name = "Torval",
            title = "The Friendly Giant",
            quote = "Big hugs for everyone!",
            description = "Charming rosy cheeks and a giant smile. A crowd favorite.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        48 to CharacterInfo(
            value = 48,
            name = "Twee",
            title = "The Sweetheart",
            quote = "Bloom where you are planted~",
            description = "Decorated with lovely flowers and long eyelashes. Sweetness in every step.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        96 to CharacterInfo(
            value = 96,
            name = "Thelma",
            title = "The Diva",
            quote = "Darling, fabulous as always.",
            description = "High fashion, cat-eye shades, and pearls. The epitome of elegance.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        192 to CharacterInfo(
            value = 192,
            name = "Thurston",
            title = "The Gentleman",
            quote = "Splendid game, my good fellow!",
            description = "Distinguished monocle and a magnificent curled mustache.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        384 to CharacterInfo(
            value = 384,
            name = "Torrance",
            title = "The Pirate",
            quote = "Ahoy! Shiver me timbers!",
            description = "One-eyed pirate captain with a shiny gold tooth and pirate bandana.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        768 to CharacterInfo(
            value = 768,
            name = "Tycho",
            title = "The Cyborg",
            quote = "System computation optimized.",
            description = "From a neon future. High-tech cybernetic visor and quantum antennae.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        1536 to CharacterInfo(
            value = 1536,
            name = "Terrence",
            title = "The King",
            quote = "By royal decree, more merges!",
            description = "Crowned in pure gold with a majestic royal beard.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        3072 to CharacterInfo(
            value = 3072,
            name = "Theron",
            title = "The Mystic",
            quote = "The cosmos aligns with the grid.",
            description = "Enveloped in celestial purple aura with glowing star-filled eyes.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        ),
        6144 to CharacterInfo(
            value = 6144,
            name = "Trinity",
            title = "The Legend",
            quote = "Infinite harmony achieved.",
            description = "The ultimate embodiment of Threes! Divine golden prismatic wings.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        )
    )

    fun getInfo(value: Int): CharacterInfo {
        return characters[value] ?: CharacterInfo(
            value = value,
            name = "T-$value",
            title = "The Legendary $value",
            quote = "Beyond all limits!",
            description = "A mythical number rarely seen by mortal eyes.",
            primaryColor = Card3Bg,
            textColor = Card3Text,
            edgeColor = Card3Edge
        )
    }
}
