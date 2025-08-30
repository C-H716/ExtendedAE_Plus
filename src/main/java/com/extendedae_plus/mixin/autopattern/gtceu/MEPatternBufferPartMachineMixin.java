package com.extendedae_plus.mixin.autopattern.gtceu;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.pattern.EncodedPatternItem;
import com.extendedae_plus.content.ScaledProcessingPattern;
import com.google.common.collect.BiMap;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Objects;

;

@Mixin(targets = "com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine",remap = false)
public class MEPatternBufferPartMachineMixin {

    // 重定向containsKey检查
    @Redirect(method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
              at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;containsKey(Ljava/lang/Object;)Z"))
    private boolean redirectContainsKey(BiMap<IPatternDetails, ?> detailsSlotMap, Object key) {
        try {
            // 如果key是ScaledProcessingPattern类型，尝试用其原始pattern进行判断
            if (key instanceof ScaledProcessingPattern scaled) {
                IPatternDetails base = scaled.getOriginal();
                if (base != null) {
                    // 避免递归重定向，直接遍历keySet判断
                    for (IPatternDetails d : detailsSlotMap.keySet()) {
                        if (Objects.equals(d, base)) return true;
                    }
                }
            }
            // 常规判断，遍历keySet
            for (IPatternDetails d : detailsSlotMap.keySet()) {
                if (Objects.equals(d, key)) return true;
            }
            return false;
        } catch (Throwable t) {
            // 出现异常时，回退到常规判断
            for (IPatternDetails d : detailsSlotMap.keySet()) {
                if (Objects.equals(d, key)) return true;
            }
            return false;
        }
    }

    @Redirect(method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
              at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object redirectGet(BiMap<IPatternDetails, ?> detailsSlotMap, Object key) {
        try {
            // 如果是 ScaledProcessingPattern，优先尝试其原始 pattern 对应的值
            if (key instanceof ScaledProcessingPattern scaled) {
                IPatternDetails base = scaled.getOriginal();
                if (base != null) {
                    for (Map.Entry<IPatternDetails, ?> e : detailsSlotMap.entrySet()) {
                        if (Objects.equals(e.getKey(), base)) {
                            return e.getValue();
                        }
                    }
                }
            }

            // 常规查找：遍历 entrySet 避免再次调用 BiMap.get 导致递归重定向
            for (Map.Entry<IPatternDetails, ?> e : detailsSlotMap.entrySet()) {
                if (Objects.equals(e.getKey(), key)) {
                    return e.getValue();
                }
            }
            return null;
        } catch (Throwable t) {
            for (Map.Entry<IPatternDetails, ?> e : detailsSlotMap.entrySet()) {
                if (Objects.equals(e.getKey(), key)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    @Shadow
    private ItemStackTransfer patternInventory;

    @Inject(method = "createUIWidget", at = @At("RETURN"), cancellable = true)
    private void addPatternCountLabels(CallbackInfoReturnable<Widget> cir) {
        try {
            Widget ret = cir.getReturnValue();
            if (!(ret instanceof WidgetGroup group)) return;
            int rowSize = 9;
            int colSize = 3;
            for (int i = 0; i < rowSize * colSize; i++) {
                int x = 8 + (i % rowSize) * 18;
                int y = 14 + (i / rowSize) * 18;
                int finalI = i;
                group.addWidget(new LabelWidget(x + 10, y + 10, () -> {
                    try {
                        ItemStack pattern = patternInventory.getStackInSlot(finalI);
                        if (pattern == null || pattern.isEmpty()) return "";
                        if (pattern.getItem() instanceof EncodedPatternItem iep) {
                            ItemStack out = iep.getOutput(pattern);
                            if (out != null && !out.isEmpty()) {
                                return String.valueOf(out.getCount());
                            }
                        }
                        return "";
                    } catch (Throwable t) {
                        return "";
                    }
                }));
            }
            cir.setReturnValue(group);
        } catch (Throwable ignored) {
        }
    }
}


