package io.github.cjustinn.specialisedworkforce;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        ItemStack icon = new ItemStack(this.iconMaterial, 1);
        ItemMeta iconMeta = icon.getItemMeta();
        List<String> _desc = new ArrayList();
        Iterator var4 = this.description.iterator();

        while(var4.hasNext()) {
            String d = (String)var4.next();
            _desc.add(d);
        }

        _desc.add("");
        _desc.add("§aClick to join");
        iconMeta.setLore(_desc);
        iconMeta.setDisplayName(String.format("§6%s", this.name));
        icon.setItemMeta(iconMeta);
        return icon;
    }

    public Material getIcon() {
        return this.iconMaterial;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getDescription() {
        return this.description;
    }

    public String getPaymentEquation() {
        return this.paymentEquation;
    }

    public boolean getPaymentEnabled() {
        return this.paymentEnabled;
    }

    public List<JobAttribute> getAttributes() {
        return this.attributes;
    }

    public boolean hasJobAttribute(JobAttributeType _type) {
        return this.attributes.stream().anyMatch((attr) -> {
            return attr.getType() == _type;
        });
    }

    public JobAttribute getAttributeByType(JobAttributeType _type) {
        int idx = -1;

        for(int i = 0; i < this.attributes.size() && idx < 0; ++i) {
            if (((JobAttribute)this.attributes.get(i)).getType().equals(_type)) {
                idx = i;
            }
        }

        return idx < 0 ? null : (JobAttribute)this.attributes.get(idx);
    }
}
