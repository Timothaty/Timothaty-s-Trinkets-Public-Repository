package net.timothaty.timothatystrinkets.mechanics.cleric_quests.display;

public interface ClericQuestRewardDisplayState {
	byte NONE = 0;
	byte HUMILITY = 1;
	byte SACRAMENT = 2;

	byte timothatys_trinkets$getClericQuestRewardDisplay();

	void timothatys_trinkets$setClericQuestRewardDisplay(byte displayType);
}
