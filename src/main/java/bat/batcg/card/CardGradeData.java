package bat.batcg.card;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class CardGradeData {
    private CardGradeData() {}

    public static final String KEY_GRADE = "batcg_grade";              // 0..4
    public static final String KEY_ATTEMPTS = "batcg_grade_attempts";  // int

    public static int getGrade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        NbtCompound n = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return clamp(n.getInt(KEY_GRADE), 0, 4);
    }

    public static void setGrade(ItemStack stack, int grade) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound n = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        n.putInt(KEY_GRADE, clamp(grade, 0, 4));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
    }

    public static int getAttempts(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        NbtCompound n = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        return Math.max(0, n.getInt(KEY_ATTEMPTS));
    }

    public static void setAttempts(ItemStack stack, int attempts) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound n = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        n.putInt(KEY_ATTEMPTS, Math.max(0, attempts));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
    }

    public static String label(int grade) {
        return switch (clamp(grade, 0, 4)) {
            case 0 -> "None";
            case 1 -> "C";
            case 2 -> "B";
            case 3 -> "A";
            case 4 -> "S";
            default -> "?";
        };
    }

    private static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }
}