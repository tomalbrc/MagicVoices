package de.tomalbrc.magicvoices.impl;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.*;

public class ItemConfig {
    public Set<Spell> spells = Set.of();
    public String voskModelPath = "vosk-small-en-us";

    public Map<String, Spell> byItemId = Map.of();
    public Map<String, Spell> byWord = Map.of();

    public static BuilderCodec<ItemConfig> CODEC = BuilderCodec.builder(ItemConfig.class, ItemConfig::new)
            .append(new KeyedCodec<>("VoskModelPath", Codec.STRING), (config, x, extraInfo) -> config.voskModelPath = x, (config, extraInfo) -> config.voskModelPath)
            .add()

            .append(new KeyedCodec<>("Spells", new ArrayCodec<>(Spell.CODEC, Spell[]::new)), (config, x, extraInfo) -> {
                        config.spells = new HashSet<>(Arrays.asList(x));
                        config.byItemId = new HashMap<>();
                        config.byWord = new HashMap<>();
                        for (Spell spell : x) {
                            config.byItemId.put(spell.itemId, spell);
                            config.byWord.put(spell.words, spell);
                        }
                    },
                    (config, extraInfo) -> config.spells.toArray(new Spell[0]))
            .add()

            .build();

    public static class Spell {
        public String itemId;
        public String interactionId;
        public String words;

        public static BuilderCodec<Spell> CODEC = BuilderCodec.builder(Spell.class, Spell::new)
                .append(new KeyedCodec<>("Item", Codec.STRING), (config, x, extraInfo) -> config.itemId = x, (config, extraInfo) -> config.itemId).add()
                .append(new KeyedCodec<>("Interaction", Codec.STRING), (config, x, extraInfo) -> config.interactionId = x, (config, extraInfo) -> config.interactionId).add()
                .append(new KeyedCodec<>("Words", Codec.STRING), (config, x, extraInfo) -> config.words = x, (config, extraInfo) -> config.words).add().build();
    }
}
