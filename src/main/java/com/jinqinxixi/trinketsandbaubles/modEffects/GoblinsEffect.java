package com.jinqinxixi.trinketsandbaubles.modEffects;

import com.jinqinxixi.trinketsandbaubles.config.Config;
import com.jinqinxixi.trinketsandbaubles.items.ModItem;
import com.jinqinxixi.trinketsandbaubles.capability.mana.ManaData;
import com.jinqinxixi.trinketsandbaubles.capability.shrink.ModCapabilities;
import com.jinqinxixi.trinketsandbaubles.TrinketsandBaublesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.ArrayList;
import java.util.List;

import static com.jinqinxixi.trinketsandbaubles.TrinketsandBaublesMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class GoblinsEffect extends MobEffect {

    private static final String ORIGINAL_MAX_MANA_KEY = "OriginalMaxMana";
    private static final String MANA_PENALTY_TAG = "GoblinManaPenaltyApplied";
    private static final String CRYSTAL_BONUS_TAG = "CrystalManaBonus"; // 新增：水晶加成标记


    public GoblinsEffect() {
        super(MobEffectCategory.NEUTRAL, 0x355E3B); // 深绿色
    }

    @Override
    public void addAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                "d141ef28-51c6-4b47-8a0d-6946e841c132",
                Config.GOBLIN_ATTACK_DAMAGE.get(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        this.addAttributeModifier(
                Attributes.MAX_HEALTH,
                "dc3b4b8c-a02c-4bd8-82e9-204088927d1f",
                Config.GOBLIN_MAX_HEALTH.get(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "91AEAA56-376B-4498-935B-2F7F68070635",
                Config.GOBLIN_MOVEMENT_SPEED.get(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        this.addAttributeModifier(
                Attributes.LUCK,
                "501E39C3-9F2A-4CCE-9A89-ACD6C7C3546A",
                Config.GOBLIN_LUCK.get(),
                AttributeModifier.Operation.ADDITION
        );

        this.addAttributeModifier(
                ForgeMod.SWIM_SPEED.get(),
                "606E2F94-D4C5-4B50-B89F-A023A0F3C102",
                Config.GOBLIN_SWIM_SPEED.get(),
                AttributeModifier.Operation.MULTIPLY_BASE
        );

        this.addAttributeModifier(
                ForgeMod.STEP_HEIGHT_ADDITION.get(),
                "8D062387-C3E4-4FD7-B47A-32E54CCB13C6",
                Config.GOBLIN_STEP_HEIGHT.get(),
                AttributeModifier.Operation.ADDITION
        );

        super.addAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);

        // 强制同步玩家属性
        if (pLivingEntity instanceof Player player) {
            // 强制同步生命值
            player.setHealth(player.getHealth());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        MobEffectInstance effect = player.getEffect(ModEffects.GOBLIN.get());

        // 如果玩家有精灵露效果
        if (effect != null) {
            // 直接移除当前效果
            player.removeEffect(ModEffects.GOBLIN.get());

            // 直接应用一个新的永久效果
            player.addEffect(new MobEffectInstance(
                    ModEffects.GOBLIN.get(),
                    -1, // 永久持续
                    0,  // 0级效果
                    false,
                    false,
                    false
            ));
        }
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            CompoundTag data = player.getPersistentData();
            if (!data.contains(MANA_PENALTY_TAG)) {
                int currentMaxMana = ManaData.getMaxMana(player);
                int crystalBonus = data.getInt(CRYSTAL_BONUS_TAG);
                int permanentDecrease = data.getInt("PermanentManaDecrease");

                int baseMaxMana = currentMaxMana - crystalBonus + permanentDecrease;
                data.putInt(ORIGINAL_MAX_MANA_KEY, baseMaxMana);

                // 直接添加配置值(可以是正数或负数)
                int newMaxMana = baseMaxMana - permanentDecrease + crystalBonus + Config.GOBLIN_MANA_PENALTY.get();
                ManaData.setMaxMana(player, Math.max(0, newMaxMana));
                data.putBoolean(MANA_PENALTY_TAG, true);
            }

            // 处理缩放效果
            entity.getCapability(ModCapabilities.SHRINK_CAPABILITY).ifPresent(cap -> {
                if (!cap.isShrunk()) {
                    float scaleFactor = Config.GOBLIN_SCALE_FACTOR.get().floatValue();
                    TrinketsandBaublesMod.LOGGER.debug("Applying shrink effect to player: {}, setting scale to: {}",
                            player.getName().getString(), scaleFactor);
                    cap.setScale(scaleFactor);
                    cap.shrink(entity);
                }
            });

            if (player.getVehicle() instanceof Horse horse) {
                // 给玩家添加BUFF
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));

                // 给马添加生命恢复效果
                if (horse.isAlive() && !horse.hasEffect(MobEffects.REGENERATION)) {
                    horse.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, 60,1,false,false));
                }
            }
        }
    }
    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting() &&
                event.getEntityMounting() instanceof Player player &&
                event.getEntityBeingMounted() instanceof AbstractHorse horse) {

            if (player.hasEffect(ModEffects.GOBLIN.get()) && !horse.level().isClientSide) {
                // 使用官方方法装备鞍
                horse.equipSaddle(SoundSource.NEUTRAL);

                // 自动驯服并设置主人
                horse.setTamed(true);
                horse.setOwnerUUID(player.getUUID());
            }
        }
    }


    // 处理伤害减免和苦力怕无视
    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.hasEffect(ModEffects.GOBLIN.get())) {
                DamageSource source = event.getSource();
                if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
                        source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)) {

                    float currentDamage = event.getAmount();
                    float reducedDamage = currentDamage * Config.GOBLIN_DAMAGE_REDUCTION.get().floatValue();
                    event.setAmount(reducedDamage);
                }
            }
        }
    }

    // 处理苦力怕无视玩家
    @SubscribeEvent
    public static void onCreeperTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player &&
                event.getEntity() instanceof Creeper) {
            if (player.hasEffect(ModEffects.GOBLIN.get())) {
                event.setCanceled(true);
            }
        }
    }

    // 处理玩家死亡时保存效果数据
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            MobEffectInstance effect = player.getEffect(ModEffects.GOBLIN.get());
            if (effect != null) {
                CompoundTag playerData = player.getPersistentData();
                CompoundTag effectData = new CompoundTag();
                effectData.putInt("Duration", effect.getDuration());
                effectData.putInt("Amplifier", effect.getAmplifier());

                // 保存所有魔力相关数据
                if (playerData.contains(ORIGINAL_MAX_MANA_KEY)) {
                    effectData.putInt(ORIGINAL_MAX_MANA_KEY,
                            playerData.getInt(ORIGINAL_MAX_MANA_KEY));
                }
                if (playerData.contains(CRYSTAL_BONUS_TAG)) {
                    effectData.putInt(CRYSTAL_BONUS_TAG,
                            playerData.getInt(CRYSTAL_BONUS_TAG));
                }
                if (playerData.contains("PermanentManaDecrease")) {
                    effectData.putInt("PermanentManaDecrease",
                            playerData.getInt("PermanentManaDecrease"));
                }

                playerData.put("GoblinEffect", effectData);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        // 创建一个新的物品列表
        List<ItemStack> items = new ArrayList<>();
        // 只添加恢复药剂作为治疗物品
        items.add(new ItemStack(ModItem.RESTORATION_SERUM.get()));
        return items;
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player player = event.getEntity();
        CompoundTag originalData = original.getPersistentData();

        if (originalData.contains("GoblinEffect")) {
            CompoundTag effectData = originalData.getCompound("GoblinEffect");

            // 获取服务器实例以延迟应用效果
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server != null) {
                // 延迟50ms (1 tick = 50ms)
                server.tell(new net.minecraft.server.TickTask(
                        server.getTickCount() + 1,
                        () -> {
                            // 应用效果
                            player.addEffect(new MobEffectInstance(
                                    ModEffects.GOBLIN.get(),
                                    effectData.getInt("Duration"),
                                    effectData.getInt("Amplifier"),
                                    false,
                                    false,
                                    false
                            ));

                            // 如果存在原始魔力值数据，直接设置正确的魔力值
                            if (effectData.contains(ORIGINAL_MAX_MANA_KEY)) {
                                int originalMana = effectData.getInt(ORIGINAL_MAX_MANA_KEY);
                                int crystalBonus = effectData.contains(CRYSTAL_BONUS_TAG) ?
                                        effectData.getInt(CRYSTAL_BONUS_TAG) : 0;
                                int permanentDecrease = effectData.contains("PermanentManaDecrease") ?
                                        effectData.getInt("PermanentManaDecrease") : 0;

                                // 保存原始值
                                player.getPersistentData().putInt(ORIGINAL_MAX_MANA_KEY, originalMana);

                                // 计算并设置正确的魔力值，使用配置的惩罚值
                                int correctMana = originalMana - permanentDecrease + crystalBonus + Config.GOBLIN_MANA_PENALTY.get();
                                ManaData.setMaxMana(player, Math.max(0, correctMana)); // 确保魔力值不会低于0

                                // 标记魔力惩罚已应用
                                player.getPersistentData().putBoolean(MANA_PENALTY_TAG, true);

                                // 保存其他相关数据
                                if (effectData.contains(CRYSTAL_BONUS_TAG)) {
                                    player.getPersistentData().putInt(CRYSTAL_BONUS_TAG, crystalBonus);
                                }
                                if (effectData.contains("PermanentManaDecrease")) {
                                    player.getPersistentData().putInt("PermanentManaDecrease", permanentDecrease);
                                }
                            }
                        }
                ));
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        // 先移除所有属性修改器
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);

        // 处理玩家特定的数据
        if (pLivingEntity instanceof Player player) {
            CompoundTag data = player.getPersistentData();
            if (data.contains(MANA_PENALTY_TAG)) {
                if (data.contains(ORIGINAL_MAX_MANA_KEY)) {
                    int baseMaxMana = data.getInt(ORIGINAL_MAX_MANA_KEY);
                    int crystalBonus = data.getInt(CRYSTAL_BONUS_TAG);
                    int permanentDecrease = data.getInt("PermanentManaDecrease");

                    // 恢复到基础值，考虑永久减少和水晶加成
                    int restoredMana = baseMaxMana - permanentDecrease + crystalBonus;
                    ManaData.setMaxMana(player, restoredMana);
                }
                // 清理标记
                data.remove(MANA_PENALTY_TAG);
                data.remove(ORIGINAL_MAX_MANA_KEY);
                data.remove("GoblinEffect");
            }

            // 处理缩小效果
            pLivingEntity.getCapability(ModCapabilities.SHRINK_CAPABILITY).ifPresent(cap -> {
                if (cap.isShrunk()) {
                    TrinketsandBaublesMod.LOGGER.debug("De-shrinking entity: {}, current scale was: {}",
                            pLivingEntity.getName().getString(), cap.scale());
                    cap.deShrink(pLivingEntity);
                    cap.sync(pLivingEntity);
                }
            });
        }
        // 强制同步玩家属性
        if (pLivingEntity instanceof Player player) {
            // 强制同步生命值
            player.setHealth(player.getHealth());
        }
    }
}