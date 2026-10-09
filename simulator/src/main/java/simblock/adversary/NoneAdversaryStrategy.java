package simblock.adversary;

import java.util.Collection;
import java.util.Collections;

import simblock.task.AbstractMessageTask;

public class NoneAdversaryStrategy extends AdversaryStrategy {

    public NoneAdversaryStrategy() {
        super(null);
    }

    public NoneAdversaryStrategy(AdversaryKnowledge knowledge) {
        super(knowledge);
    }

    @Override
    public Collection<Object> findTargets(AdversaryCapability capability) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Object> findTargets(AdversaryCapability capability, AbstractMessageTask msgTask) {
        return Collections.emptySet();
    }
}
