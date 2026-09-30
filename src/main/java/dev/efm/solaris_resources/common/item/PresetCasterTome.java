package dev.efm.solaris_resources.common.item;

import com.hollingsworth.arsnouveau.api.spell.ISpellCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.items.CasterTome;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class PresetCasterTome extends CasterTome {
    private final Supplier<Spell> preset;

    public PresetCasterTome(Supplier<Spell> preset) {
        super(new Properties().stacksTo(1));
        this.preset = preset;
    }

    @Override
    public @NotNull ISpellCaster getSpellCaster(ItemStack stack) {
        ISpellCaster caster = super.getSpellCaster(stack);
        // Covers ordinary stacks from /give and recipes, not just the creative tab.
        // Keep existing spells (including creative-mode scribing) intact.
        if (caster.getSpell().isEmpty()) {
            caster.setCurrentSlot(0);
            caster.setSpell(preset.get());
        }
        return caster;
    }
}
