package com.thin.remote;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

/**
 * Встроенный спец-рендерер: вместо плоской иконки рисует полноценную
 * 3D-модель пульта (видно в руке от первого/третьего лица, на земле, в рамке).
 */
public final class RemoteSpecialRenderer implements SpecialModelRenderer<ModelPart> {

    public static final RemoteSpecialRenderer INSTANCE = new RemoteSpecialRenderer();

    private RemoteSpecialRenderer() {
    }

    @Override
    public void render(@Nullable ModelPart modelPart, ItemDisplayContext displayContext,
                       boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, boolean hasFoil, int seed) {
        ModelPart root = modelPart != null ? modelPart : RemoteModelLoader.bakedRoot();
        if (root == null) {
            return; // геометрия ещё не готова
        }
        RemoteClientEvents.renderRemote(poseStack, bufferSource, packedLight, displayContext, leftHand, root);
    }

    @Override
    public @Nullable ModelPart extractData(net.minecraft.world.item.ItemStack stack,
                                           @Nullable ItemDisplayContext displayContext,
                                           @Nullable net.minecraft.world.level.Level level, int seed) {
        return RemoteModelLoader.bakedRoot();
    }
}
