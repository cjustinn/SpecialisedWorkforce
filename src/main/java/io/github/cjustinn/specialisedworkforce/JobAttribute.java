package io.github.cjustinn.specialisedworkforce;

import java.util.List;

public class JobAttribute {
    private JobAttributeType type;
    private List<String> targets;
    private String equation;
    private String chance;

    // Constructors
    public JobAttribute(JobAttributeType _type, List<String> _targets, String _equation, String _chance) {
        this.type = _type;
        this.targets = _targets;

        this.equation = _equation;
        this.chance = _chance;
    }

    // Member functions
    public String getEquation() { return this.equation; }
    public String getChance() { return this.chance; }

    public List<String> getTargets() { return this.getTargets(); }
    public JobAttributeType getType() { return this.type; }

    public boolean StringEndsWithTarget(String _s) {
        boolean found = false;

        for (int i = 0; i < this.targets.size() && !found; i++) {
            if (_s.toLowerCase().endsWith(this.targets.get(i).replace("{*}", "").toLowerCase()))
                found = true;
        }

        return found;
    }
}
