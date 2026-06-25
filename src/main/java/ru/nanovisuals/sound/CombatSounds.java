package ru.nanovisuals.sound;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Tracks entities the local player attacked, then fires KILL when those
 * entities die or vanish soon after. Pure polling against the client world —
 * no mixin needed, no server side.
 */
public final class CombatSounds {

    private static final long KILL_WINDOW_MS = 3_000L;
    private static final long DROP_AFTER_MS  = 5_000L;

    private static final CombatSounds INSTANCE = new CombatSounds();

    public static CombatSounds getInstance() {
        return INSTANCE;
    }

    private final Deque<Tracked> tracked = new ArrayDeque<>();

    private CombatSounds() {}

    public void onAttack(LivingEntity target) {
        if (target == null) return;
        SoundManager.getInstance().play(SoundManager.Sound.HIT);
        long now = System.currentTimeMillis();
        if (target.isDead() || target.isRemoved()) {
            SoundManager.getInstance().play(SoundManager.Sound.KILL);
            return;
        }
        tracked.add(new Tracked(target.getUuid(), now, target.getHealth()));
    }

    public void tick() {
        if (tracked.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) {
            tracked.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Tracked> it = tracked.iterator();
        while (it.hasNext()) {
            Tracked t = it.next();
            long age = now - t.attackedAt;
            if (age > DROP_AFTER_MS) {
                it.remove();
                continue;
            }

            Entity entity = world.getEntityLookup().get(t.id);
            if (entity == null) {
                if (age <= KILL_WINDOW_MS) {
                    SoundManager.getInstance().play(SoundManager.Sound.KILL);
                }
                it.remove();
            } else if (entity instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0f)) {
                SoundManager.getInstance().play(SoundManager.Sound.KILL);
                it.remove();
            }
        }
    }

    private record Tracked(UUID id, long attackedAt, float healthAtAttack) {}
}
