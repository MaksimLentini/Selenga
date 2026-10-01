package com.thin.remote;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

@Mod(ThinRemoteMod.MODID)
public final class ThinRemoteMod {

    public static final String MODID = "remote";

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    /** Предмет «Пульт» — 3D-модель из Blockbench JSON. */
    public static final DeferredItem<Item> REMOTE_ITEM =
            ITEMS.registerSimpleItem("remote",
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public ThinRemoteMod(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        modBus.addListener(ThinRemoteMod::onClientExtensions);
    }

    /** Подключаем собственный рендер предмета на клиенте. */
    @OnlyOn(Dist.class)
    private static void onClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.renderer.special.SpecialModelRenderer getBuiltInRenderer() {
                return RemoteSpecialRenderer.INSTANCE;
            }
        }, ThinRemoteMod.REMOTE_ITEM.get());
    }
}
