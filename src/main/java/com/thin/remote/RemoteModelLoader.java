package com.thin.remote;

import net.minecraft.client.model.geom.ModelPart;

/** Кэш запечённой геометрии пульта. */
public final class RemoteModelLoader {

    private static ModelPart bakedRoot;

    private RemoteModelLoader() {
    }

    static void bakeIfNeeded() {
        if (bakedRoot == null) {
            bakedRoot = RemoteClientEvents.createLayer().root();
        }
    }

    /** Возвращает копию корневой ModelPart для отрисовки либо null, если ещё не готова. */
    public static ModelPart bakedRoot() {
        return bakedRoot != null ? bakedRoot.copyFromDynamic() : null;
    }
}
