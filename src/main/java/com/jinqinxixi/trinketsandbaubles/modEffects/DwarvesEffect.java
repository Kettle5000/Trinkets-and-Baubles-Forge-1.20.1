package com.jinqinxixi.trinketsandbaubles.modEffects;

import com.jinqinxixi.trinketsandbaubles.config.Config;
import com.jinqinxixi.trinketsandbaubles.items.ModItem;
import com.jinqinxixi.trinketsandbaubles.capability.mana.ManaData;
import com.jinqinxixi.trinketsandbaubles.capability.shrink.ModCapabilities;
import com.jinqinxixi.trinketsandbaubles.TrinketsandBaublesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

import static com.jinqinxixi.trinketsandbaubles.TrinketsandBaublesMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class DwarvesEffect extends MobEffect {


    public DwarvesEffect() {
        super(MobEffectCategory.NEUTRAL, 0x7E6339); // 褐色
    }

    @Override
    public void addAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        // 攻击速度
        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                "4520f278-fb8f-4c75-9336-5c3ab7c6134a",
                Config.DWARVES_ATTACK_SPEED.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // 攻击伤害
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                "d141ef28-51c6-4b47-8a0d-6946e841c132",
                Config.DWARVES_ATTACK_DAMAGE.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // 最大生命值
        this.addAttributeModifier(
                Attributes.MAX_HEALTH,
                "dc3b4b8c-a02c-4bd8-82e9-204088927d1f",
                Config.DWARVES_MAX_HEALTH.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // 护甲韧性
        this.addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                "8fc5e73c-2cf2-4729-8128-d99f49aa37f2",
                Config.DWARVES_ARMOR_TOUGHNESS.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // 击退抗性
        this.addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                "95eb4f0a-dd60-4ada-98c1-2ce5c3d4374c",
                Config.DWARVES_KNOCKBACK_RESISTANCE.get(),
                AttributeModifier.Operation.ADDITION
        );

        // 移动速度
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                "3b8f4065-5f43-4939-8e6a-a34f2d67c55d",
                Config.DWARVES_MOVEMENT_SPEED.get(),
                AttributeModifier.Operation.MULTIPLY_TOTAL
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
        MobEffectInstance effect = player.getEffect(ModEffects.DWARVES.get());

        // 如果玩家有精灵露效果
        if (effect != null) {
            // 直接移除当前效果
            player.removeEffect(ModEffects.DWARVES.get());

            // 直接应用一个新的永久效果
            player.addEffect(new MobEffectInstance(
                    ModEffects.DWARVES.get(),
                    -1, // 永久持续
                    0,  // 0级效果
                    false,
                    false,
                    false
            ));
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


    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            if (!player.level().isClientSide) {
                CompoundTag data = player.getPersistentData();
                // 获取当前最大魔力值和修正值
                int currentMaxMana = ManaData.getMaxMana(player);
                int crystalBonus = data.getInt("CrystalManaBonus");
                int permanentDecrease = data.getInt("PermanentManaDecrease");

                // 确保魔力值正确反映水晶加成和永久减少
                int baseMaxMana = currentMaxMana - crystalBonus + permanentDecrease;
                int newMaxMana = baseMaxMana - permanentDecrease + crystalBonus;
                if (currentMaxMana != newMaxMana) {
                    ManaData.setMaxMana(player, newMaxMana);
                }

                // 处理缩放效果
                entity.getCapability(ModCapabilities.SHRINK_CAPABILITY).ifPresent(cap -> {
                    if (!cap.isShrunk()) {
                        float scaleFactor = Config.DWARVES_SCALE_FACTOR.get().floatValue();
                        TrinketsandBaublesMod.LOGGER.debug("Applying shrink effect to player: {}, setting scale to: {}",
                                player.getName().getString(), scaleFactor);
                        cap.setScale(scaleFactor);
                        cap.shrink(entity);
                    }
                });
            }


            // 原有的幸运效果检查代码
            if (entity.tickCount % 10 == 0) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof PickaxeItem) {
                    int fortuneLevel = mainHand.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);

                    if (fortuneLevel == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 30, 0, false, false));
                    }
                }
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);

        if (pLivingEntity instanceof Player player) {
            // 处理魔力值调整
            CompoundTag data = player.getPersistentData();
            // 获取当前最大魔力值
            int currentMaxMana = ManaData.getMaxMana(player);
            int crystalBonus = data.getInt("CrystalManaBonus");
            int permanentDecrease = data.getInt("PermanentManaDecrease");

            // 重新应用水晶加成和永久减少
            int baseMaxMana = currentMaxMana - crystalBonus + permanentDecrease;
            int newMaxMana = baseMaxMana - permanentDecrease + crystalBonus;
            ManaData.setMaxMana(player, newMaxMana);

            // 强制同步玩家生命值
            player.setHealth(player.getHealth());
        }

        // 处理缩放效果的移除
        pLivingEntity.getCapability(ModCapabilities.SHRINK_CAPABILITY).ifPresent(cap -> {
            if (cap.isShrunk()) {
                TrinketsandBaublesMod.LOGGER.debug("De-shrinking entity: {}, current scale was: {}",
                        pLivingEntity.getName().getString(), cap.scale());
                cap.deShrink(pLivingEntity);
            }
        });
        // 强制同步玩家属性
        if (pLivingEntity instanceof Player player) {
            // 强制同步生命值
            player.setHealth(player.getHealth());
        }
    }

//    @Override
//    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
//        if (entity instanceof Player player) {
//            ItemStack mainHand = player.getMainHandItem();
//            if (mainHand.getItem() instanceof PickaxeItem) {
//                // 通过注册表访问获取时运附魔
//                Holder<Enchantment> fortuneHolder = entity.registryAccess()
//                        .lookupOrThrow(Registries.ENCHANTMENT)
//                        .getOrThrow(Enchantments.FORTUNE);
//
//                // 使用新版 ItemStack 方法获取等级
//                int fortuneLevel = mainHand.getEnchantmentLevel(fortuneHolder);
//
//                if (fortuneLevel == 0) {
//                    player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20, 0, false, false));
//                }
//            }
//        }
//        return true;
//    }


    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getState().getBlock();

        // 检查玩家是否有矮人效果
        if (!player.hasEffect(ModEffects.DWARVES.get())) {
            return;
        }

        // 检查是否在服务器端且是 ServerLevel
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            // 处理经验值奖励
            if (block.defaultBlockState().is(BlockTags.STONE_ORE_REPLACEABLES) ||
                    block.defaultBlockState().is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) ||
                    block == Blocks.END_STONE) {  // 添加末地石的判断
                // 石头、深板岩和末地石给1点经验
                serverLevel.addFreshEntity(new ExperienceOrb(
                        serverLevel,
                        event.getPos().getX() + 0.5,
                        event.getPos().getY() + 0.5,
                        event.getPos().getZ() + 0.5,
                        1
                ));
            } else if (block.defaultBlockState().is(Tags.Blocks.ORES)) {
                // 矿石额外给0-2点经验
                int extraXp = player.getRandom().nextInt(3);
                if (extraXp > 0) {
                    serverLevel.addFreshEntity(new ExperienceOrb(
                            serverLevel,
                            event.getPos().getX() + 0.5,
                            event.getPos().getY() + 0.5,
                            event.getPos().getZ() + 0.5,
                            extraXp
                    ));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            MobEffectInstance effect = player.getEffect(ModEffects.DWARVES.get());
            if (effect != null) {
                // 将效果数据存储到玩家的 NBT 中
                CompoundTag playerData = player.getPersistentData();
                CompoundTag effectData = new CompoundTag();
                effectData.putInt("Duration", effect.getDuration());
                effectData.putInt("Amplifier", effect.getAmplifier());

                // 保存永久魔力减少值
                if (playerData.contains("PermanentManaDecrease")) {
                    effectData.putInt("PermanentManaDecrease",
                            playerData.getInt("PermanentManaDecrease"));
                }

                playerData.put("DwarvesEffect", effectData);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player player = event.getEntity();
        CompoundTag originalData = original.getPersistentData();

        if (originalData.contains("DwarvesEffect")) {
            CompoundTag effectData = originalData.getCompound("DwarvesEffect");
            int duration = effectData.getInt("Duration");
            int amplifier = effectData.getInt("Amplifier");

            // 获取服务器实例以延迟应用效果
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server != null) {
                // 延迟1tick (50ms) 后应用效果
                server.tell(new net.minecraft.server.TickTask(
                        server.getTickCount() + 1,
                        () -> {
                            // 在新玩家实体上重新应用效果
                            player.addEffect(new MobEffectInstance(
                                    ModEffects.DWARVES.get(),
                                    duration,
                                    amplifier,
                                    false,
                                    false,
                                    false
                            ));

                            // 恢复永久魔力减少值
                            if (effectData.contains("PermanentManaDecrease")) {
                                player.getPersistentData().putInt("PermanentManaDecrease",
                                        effectData.getInt("PermanentManaDecrease"));
                            }
                        }
                ));
            }
        }
    }
}