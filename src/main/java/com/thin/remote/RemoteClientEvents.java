package com.thin.remote;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Клиентская часть: загружает геометрию пульта (модель из Blockbench)
 * и подменяет отрисовку предмета на 3D-модель, которую видно в руке.
 */
@EventBusSubscriber(modid = ThinRemoteMod.MODID, value = Dist.CLIENT)
public final class RemoteClientEvents {

    /** Путь к текстуре из вашего JSON ("textures": {"0": "remote_texture"}) */
    public static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(ThinRemoteMod.MODID, "textures/entity/remote_texture.png");

    private static RenderType remoteRenderType;

    private static RenderType renderType() {
        if (remoteRenderType == null) {
            remoteRenderType = RenderType.textures(TEXTURE_LOCATION);
        }
        return remoteRenderType;
    }

    private RemoteClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Выпекаем геометрию один раз и кэшируем для рендера предмета
        RemoteModelLoader.bakeIfNeeded();
    }

    /** Создаёт LayerDefinition с геометрией пульта (все элементы из Blockbench JSON). */
    public static LayerDefinition createLayer() {
        return LayerDefinition.create(buildMesh(), 16, 16);
    }

    private static MeshDefinition buildMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // name, from[bbX,bbY,bbZ], to[bbX,bbY,bbZ], texture uv column
        box(root, "body",           4F, 3F, 7F,    12F, 16F, 9F,    0); // Main Body
        box(root, "leg_left",       4F, 0F, 7F,    6.5F, 3F, 9F,    0); // Leg Left
        box(root, "leg_right",      9.5F, 0F, 7F,  12F, 3F, 9F,     0); // Leg Right
        box(root, "screen_frame",   7F, 10F, 6.7F, 10.5F, 13.5F, 7F, 7); // Screen Frame
        box(root, "screen_glass",   7.3F, 10.3F, 6.5F, 10.2F, 13.2F, 6.8F, 1); // Screen Glass
        box(root, "ind_frame",      4.8F, 9F, 6.8F, 6F, 14.2F, 7F,  7); // Indicator Frame
        box(root, "ind_seg_1",      5.1F, 13.2F, 6.6F, 5.7F, 13.8F, 6.9F, 8); // Indicator Seg 1
        box(root, "ind_seg_2",      5.1F, 12.2F, 6.6F, 5.7F, 12.8F, 6.9F, 8); // Indicator Seg 2
        box(root, "ind_seg_3",      5.1F, 11.2F, 6.6F, 5.7F, 11.8F, 6.9F, 8); // Indicator Seg 3
        box(root, "ind_seg_4",      5.1F, 10.2F, 6.6F, 5.7F, 10.8F, 6.9F, 8); // Indicator Seg 4
        box(root, "ind_seg_5",      5.1F, 9.2F, 6.6F, 5.7F, 9.8F, 6.9F, 8);   // Indicator Seg 5
        box(root, "white_btn_1",    7F, 8F, 6.7F,  8.8F, 9.2F, 7F,  3); // White Button 1
        box(root, "white_btn_2",    9.1F, 8F, 6.7F, 10.3F, 9.2F, 7F, 3); // White Button 2
        box(root, "red_btn_1",      4.9F, 6.3F, 6.7F, 5.7F, 7.1F, 7F, 2); // Red Button 1
        box(root, "red_btn_2",      4.9F, 4.6F, 6.7F, 5.7F, 5.4F, 7F, 2); // Red Button 2
        box(root, "knob_outer",     7F, 3.4F, 6.6F, 10F, 6.4F, 7F,  6); // Knob Outer
        box(root, "knob_mid",       7.5F, 3.9F, 6.4F, 9.5F, 5.9F, 6.7F, 4); // Knob Mid
        box(root, "knob_center",    8.1F, 4.5F, 6.2F, 8.9F, 5.3F, 6.5F, 6); // Knob Center
        box(root, "bolt_tl",        4.3F, 15.2F, 6.8F, 4.8F, 15.7F, 7F, 4); // Bolt Top Left
        box(root, "bolt_tr",        11.2F, 15.2F, 6.8F, 11.7F, 15.7F, 7F, 4); // Bolt Top Right
        box(root, "bolt_bl",        4.3F, 0.4F, 6.8F, 4.8F, 0.9F, 7F, 4);  // Bolt Bottom Left
        box(root, "bolt_br",        11.2F, 0.4F, 6.8F, 11.7F, 0.9F, 7F, 4); // Bolt Bottom Right
        box(root, "lamp_base",      5.4F, 16F, 7.4F, 6.2F, 16.5F, 8.2F, 4);  // Lamp Base
        box(root, "lamp_bulb",      5.55F, 16.5F, 7.55F, 6.05F, 17.2F, 8.05F, 5); // Lamp Bulb Green
        box(root, "antenna_base",   10F, 16F, 7.5F, 10.8F, 16.6F, 8.3F, 4); // Antenna Base
        box(root, "antenna_rod_1",  10.25F, 16.6F, 7.75F, 10.55F, 19.5F, 8.05F, 9); // Antenna Rod 1
        box(root, "antenna_rod_2",  10.33F, 19.5F, 7.83F, 10.47F, 21.5F, 7.97F, 9); // Antenna Rod 2

        return mesh;
    }

    /** Куб по координатам Blockbench (from/to) с переводом в систему Minecraft EntityModel. */
    private static void box(PartDefinition root, String name,
                            float fx, float fy, float fz,
                            float tx, float ty, float tz,
                            float texU) {
        float mcX = fx - 8F;
        float mcY = fy - 24F;
        float mcZ = fz - 8F;
        float sizeX = tx - fx;
        float sizeY = ty - fy;
        float sizeZ = tz - fz;

        root.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs((int) (texU * 8), 0)
                        .addBox(mcX, mcY, mcZ, sizeX, sizeY, sizeZ),
                PartPose.ZERO);
    }

    /**
     * Рендерит 3D-модель вместо обычной иконки предмета — её видно в руке,
     * при block/place display контекстах.
     */
    public static void renderRemote(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                    ItemDisplayContext displayContext, boolean leftHand, ModelPart root) {
        if (root == null) {
            return;
        }
        poseStack.pushPose();
        // Масштаб: модель ~16x24 пикселей, приводим к размеру предмета
        float scale = switch (displayContext) {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 0.6F;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.45F;
            case GROUND -> 0.5F;
            case FIXED -> 0.8F;
            default -> 0.55F; // HEAD и прочие
        };
        // Модель в координатах BB (y вверх) перевёрнута в MC-системе — разворачиваем и центрируем
        poseStack.translate(0.5F, 0.75F, 0.5F);
        if (leftHand) {
            poseStack.scale(-1F, 1F, 1F);
        }
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(180F)); // разворот: модель стоит вертикально
        poseStack.translate(4F, 10.6F, 1F); // компенсация центра модели в MC-координатах

        VertexConsumer builder = bufferSource.getBuffer(renderType());
        root.render(poseStack.last().pose(), builder, packedLight,
                0xF000F0FF /* полное освещение модели */,
                poseStack.last().pose(), poseStack.last().normal());
        poseStack.popPose();
    }
}
