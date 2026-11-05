package com.madreaper16.turkeytime.mixin;

import com.madreaper16.turkeytime.registry.TurkeyItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.UUID;

/** Adds player attribution to completed campfire recipes, without changing cooking or drops. */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireCookingMixin {
    @Shadow @Final private NonNullList<ItemStack> items;
    @Unique private final UUID[] turkeytime$cooks = new UUID[4];
    @Unique private int turkeytime$slot = -1;
    @Unique private UUID turkeytime$cook;

    @Inject(method = "placeFood", at = @At("HEAD"))
    private void turkeytime$remember(ServerLevel level, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        turkeytime$slot = -1;
        turkeytime$cook = stack.is(TurkeyItems.RAW_TURKEY) && entity instanceof ServerPlayer ? entity.getUUID() : null;
        for (int i = 0; i < items.size(); i++) if (items.get(i).isEmpty()) { turkeytime$slot = i; break; }
    }

    @Inject(method = "placeFood", at = @At("RETURN"))
    private void turkeytime$record(ServerLevel level, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ() && turkeytime$slot >= 0) turkeytime$cooks[turkeytime$slot] = turkeytime$cook;
    }

    @Redirect(method = "cookTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private static Object turkeytime$finished(NonNullList<ItemStack> list, int slot, Object empty,
            ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity campfire,
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes) {
        var self = (CampfireCookingMixin) (Object) campfire;
        var raw = list.get(slot);
        var cook = self.turkeytime$cooks[slot];
        if (cook != null && raw.is(TurkeyItems.RAW_TURKEY)) {
            var player = level.getServer().getPlayerList().getPlayer(cook);
            if (player != null) {
                var input = new SingleRecipeInput(raw);
                recipes.getRecipeFor(input, level).filter(r -> r.value().assemble(input).is(TurkeyItems.COOKED_TURKEY))
                        .ifPresent(r -> player.triggerRecipeCrafted(r, List.of(raw.copy())));
            }
        }
        self.turkeytime$cooks[slot] = null;
        return list.set(slot, (ItemStack) empty);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void turkeytime$save(ValueOutput output, CallbackInfo ci) {
        for (int i = 0; i < 4; i++) if (turkeytime$cooks[i] != null)
            output.putString("TurkeyCook" + i, turkeytime$cooks[i].toString());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void turkeytime$load(ValueInput input, CallbackInfo ci) {
        for (int i = 0; i < 4; i++) {
            turkeytime$cooks[i] = null;
            try { turkeytime$cooks[i] = UUID.fromString(input.getStringOr("TurkeyCook" + i, "")); }
            catch (IllegalArgumentException ignored) { /* Old saves have no attribution. */ }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void turkeytime$clear(CallbackInfo ci) { java.util.Arrays.fill(turkeytime$cooks, null); }
}
