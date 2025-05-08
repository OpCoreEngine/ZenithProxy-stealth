package com.zenith.mc.block;

import java.util.List;

import static com.zenith.Globals.BLOCK_DATA;

/**
 * @param id palette blockstate id
 */
public record BlockState(Block block, int id, int x, int y, int z) {
    public boolean isSolidBlock() {
        return block.isBlock();
    }

    public boolean isShapeFullBlock() {
        List<CollisionBox> collisionBoxes = getCollisionBoxes();
        if (collisionBoxes.size() != 1) {
            return false;
        }
        var cb = collisionBoxes.getFirst();
        return cb.isFullBlock();
    }

    public List<CollisionBox> getCollisionBoxes() {
        return BLOCK_DATA.getCollisionBoxesFromBlockStateId(id);
    }

    public List<CollisionBox> getInteractionBoxes() {
        return BLOCK_DATA.getInteractionBoxesFromBlockStateId(id);
    }

    public List<LocalizedCollisionBox> getLocalizedCollisionBoxes() {
        var collisionBoxes = getCollisionBoxes();
        return BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, x, y, z);
    }

    public List<LocalizedCollisionBox> getLocalizedInteractionBoxes() {
        var collisionBoxes = getInteractionBoxes();
        return BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, x, y, z);
    }

    public boolean isPathfindable() {
        return BLOCK_DATA.isPathfindable(id);
    }
}
