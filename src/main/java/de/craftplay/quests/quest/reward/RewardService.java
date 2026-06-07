package de.craftplay.quests.quest.reward;

import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestReward;
import java.util.ArrayList;

public final class RewardService {

    public RewardPlan planRewards(Quest quest) {
        ArrayList<String> warnings = new ArrayList<>();
        for (QuestReward reward : quest.rewards()) {
            if (reward.data().isEmpty()) {
                warnings.add("reward-has-no-data:" + reward.id());
            }
        }
        return new RewardPlan(quest.id(), quest.rewards(), warnings);
    }
}
