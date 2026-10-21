package com.rickmorty.plugin.commands;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Пустой генератор для мира "Citadel": только платформа 16x16
 * на высоте Y=70 в спавн-чанке, вокруг — пустота.
 *
 * <p>Используется подкомандой {@code /rick portal} при mode=CITADEL.</p>
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeInfo biomeInfo) {
        ChunkData data = createEmptyChunkData(world, true, (String[]) null);
        // Платформа только в чанке 0,0
        if (chunkX == 0 && chunkZ == 0) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    data.setBlock(x, 70, z, org.bukkit.Material.POLISHED_ANDESITE);
                    // Бордюр из плачущего обсидиана — атмосфера Цитадели
                    if (x == 0 || z == 0 || x == 15 || z == 15) {
                        data.setBlock(x, 71, z, org.bukkit.Material.CRYING_OBSIDIAN);
                    }
                }
            }
        }
        return data;
    }

    @Nullable
    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return null; // стандартный биом-провайдер
    }

    /** Точка появления игрока — центр платформы. */
    @Override
    public org.bukkit.Location getFixedSpawnLocation(World world, Random random) {
        return new org.bukkit.Location(world, 8.5, 71.5, 8.5);
    }
}
