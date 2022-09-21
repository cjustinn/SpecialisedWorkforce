package io.github.cjustinn.specialisedworkforce;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Job {
    private String name;
    private String paymentEquation;
    private List<String> description;
    private Material iconMaterial;

    private boolean paymentEnabled;

    private List<JobAttribute> attributes;

    public Job(String _name, List<String> _desc, boolean _paymentsEnabled, String _paymentEquation, List<JobAttribute> _attr, Material _icon) {
        this.name = _name;
        this.description = _desc;
        this.paymentEquation = _paymentEquation;
        this.paymentEnabled = _paymentsEnabled;
        this.attributes = _attr;
        this.iconMaterial = _icon;
    }

    public ItemStack getSelectionIcon() {
        ItemStack icon = new ItemStack(iconMaterial, 1);

        ItemMeta iconMeta = icon.getItemMeta();

        List<String> _desc = new ArrayList<String>();
        for (String d : description) {
            _desc.add(d);
        }

        _desc.add("");
        _desc.add("§aClick to join");

        iconMeta.setLore(_desc);
        iconMeta.setDisplayName(String.format("§6%s", name));

        icon.setItemMeta(iconMeta);

        return icon;
    }

    public Material getIcon() { return this.iconMaterial; }
    public String getName() { return this.name; }
    public List<String> getDescription() { return this.description; }
    public String getPaymentEquation() { return this.paymentEquation; }
    public boolean getPaymentEnabled() { return this.paymentEnabled; }
    public List<JobAttribute> getAttributes() { return this.attributes; }

    public boolean hasJobAttribute(JobAttributeType _type) {
        return this.attributes.stream().anyMatch(attr -> attr.getType() == _type);
    }

    public JobAttribute getAttributeByType(JobAttributeType _type) {
        int idx = -1;

        for (int i = 0; i < this.attributes.size() && idx < 0; i++) {
            if (this.attributes.get(i).getType().equals(_type)) {
                idx = i;
            }
        }

        return idx < 0 ? null : this.attributes.get(idx);
    }
}
