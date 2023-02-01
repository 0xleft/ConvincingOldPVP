package io.github.adomasdauda;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConvincingOldPVP extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
                new PacketAdapter(this, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {

                        List<Sound> blockedSounds = List.of(
                                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                                Sound.ENTITY_PLAYER_ATTACK_WEAK,
                                Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                                Sound.ENTITY_PLAYER_ATTACK_NODAMAGE
                        );

                        if (blockedSounds.contains(event.getPacket().getSoundEffects().getValues().get(0))) {
                            event.setCancelled(true);
                        }
                    }
                }
        );

    }


    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {

        // md5 :kiss:
        AttributeInstance attackAttribute = event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(100);
        }
    }

    @EventHandler
    private void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player) {

            ItemStack item = player.getInventory().getItemInMainHand();
            Map<Enchantment, Integer> enchantments = item.getEnchantments();
            LivingEntity entity = (LivingEntity) event.getEntity();

            double rawDamage = getRawDamage(item);

            if (enchantments.get(Enchantment.DAMAGE_ARTHROPODS) != null)
                if (enchantments.get(Enchantment.DAMAGE_ARTHROPODS) > 0
                        && entity.getCategory().equals(EntityCategory.ARTHROPOD)) {
                    rawDamage += (2.5 * enchantments.get(Enchantment.DAMAGE_ARTHROPODS));
                }

            if (enchantments.get(Enchantment.DAMAGE_UNDEAD) != null)
                if (enchantments.get(Enchantment.DAMAGE_UNDEAD) > 0
                        && entity.getCategory().equals(EntityCategory.UNDEAD)) {
                    rawDamage += (2.5 * enchantments.get(Enchantment.DAMAGE_UNDEAD));
                }

            if (enchantments.get(Enchantment.DAMAGE_ALL) != null)
                if (enchantments.get(Enchantment.DAMAGE_ALL) > 0) {
                    rawDamage += (1.25 * enchantments.get(Enchantment.DAMAGE_ALL));
                }

            if (checkCrit(player)) {
                rawDamage *= 1.5;
            }

            if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                rawDamage += (3 * Objects.requireNonNull(player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE)).getAmplifier());
            }

            event.setDamage(rawDamage);
        }
    }

    public double getRawDamage(ItemStack item) {
        Material material = item.getType();
        double rawDamage;
        switch (material) {
            default -> rawDamage = 1; // fist
            case NETHERITE_SWORD -> rawDamage = 8;
            case DIAMOND_SWORD, TRIDENT, NETHERITE_AXE -> rawDamage = 7;
            case IRON_SWORD, NETHERITE_PICKAXE, NETHERITE_SHOVEL, DIAMOND_AXE -> rawDamage = 6;
            case STONE_SWORD, DIAMOND_PICKAXE, DIAMOND_SHOVEL, IRON_AXE -> rawDamage = 5;
            case WOODEN_SWORD, IRON_PICKAXE, IRON_SHOVEL, STONE_AXE -> rawDamage = 4;
            case WOODEN_AXE, STONE_PICKAXE, STONE_SHOVEL -> rawDamage = 3;
            case WOODEN_PICKAXE, WOODEN_SHOVEL -> rawDamage = 2;
        }

        return rawDamage;
    }


    /**
     * @param player
     * Include all the blocks that you don't want player getting crits in
     * @return if the hit should be critical
     */
    public boolean checkCrit(@NotNull Player player) {
        Material blockMaterial = player.getLocation().getWorld().getBlockAt(player.getLocation()).getType();
        ArrayList<Material> blackList = new ArrayList<>() {
            {
                add(Material.WATER);
                add(Material.LADDER);
                add(Material.VINE);
                add(Material.CAVE_VINES);
                add(Material.GLOW_LICHEN);
                add(Material.TWISTING_VINES);
                add(Material.WEEPING_VINES);
            }
        };

        return (!player.isOnGround() && !blackList.contains(blockMaterial) && player.getFallDistance() != 0
                && !player.isSprinting() && !player.hasPotionEffect(PotionEffectType.BLINDNESS));
    }
}
