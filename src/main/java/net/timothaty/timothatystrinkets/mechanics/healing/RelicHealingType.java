package net.timothaty.timothatystrinkets.mechanics.healing;

/**
 * Natural - healing that comes from food consumation, natural vanilla regeneration and Gorge "Digestive Surge" ability. This WON'T affect potions. (I will move compatibility with potions to a separate enum in the future if necessary)
 * Holy - holy sources such as Holy Light ability the Repentance + Sacrament bead combination will have.
 * Unholy - 1.2a no such healing sources yet
 * Vampirism - healing from sources that give specific amount health on damaging other creatures (for example - FANGS, or "Vampiric Fangs")
 * Soul - healing that depends on "absorbing" souls of various creatures, e.g. Champion's Gauntlet ability "Soul Absorption" that heals player depending on how many creatures been slain during Soul Absorption effect.
 */
public enum RelicHealingType {
	NATURAL,
	HOLY,
	UNHOLY,
	VAMPIRISM,
	SOUL
}
