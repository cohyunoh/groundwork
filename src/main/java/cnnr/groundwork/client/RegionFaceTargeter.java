package cnnr.groundwork.client;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only ray/AABB slab intersection: determines which face of a box a look ray passes
 * through first (the entry face). If the eye is already inside the box, falls back to the exit
 * face instead, so looking around from inside the region still highlights something sensible.
 */
public final class RegionFaceTargeter {
    private RegionFaceTargeter() {}

    public static Direction aimedFace(Vec3 eye, Vec3 look, AABB box, double maxDist) {
        double tNear = Double.NEGATIVE_INFINITY;
        double tFar = maxDist;
        Direction nearFace = null;
        Direction farFace = null;

        // X slab: the min-X plane's outward normal is -X (WEST), max-X's is +X (EAST).
        if (look.x != 0.0) {
            double inv = 1.0 / look.x;
            double t1 = (box.minX - eye.x) * inv;
            double t2 = (box.maxX - eye.x) * inv;
            Direction f1 = Direction.WEST, f2 = Direction.EAST;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; Direction f = f1; f1 = f2; f2 = f; }
            if (t1 > tNear) { tNear = t1; nearFace = f1; }
            if (t2 < tFar) { tFar = t2; farFace = f2; }
            if (tNear > tFar) return null;
        } else if (eye.x < box.minX || eye.x > box.maxX) {
            return null; // parallel to this axis and outside the slab: can never hit
        }

        // Y slab: min-Y faces -Y (DOWN), max-Y faces +Y (UP).
        if (look.y != 0.0) {
            double inv = 1.0 / look.y;
            double t1 = (box.minY - eye.y) * inv;
            double t2 = (box.maxY - eye.y) * inv;
            Direction f1 = Direction.DOWN, f2 = Direction.UP;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; Direction f = f1; f1 = f2; f2 = f; }
            if (t1 > tNear) { tNear = t1; nearFace = f1; }
            if (t2 < tFar) { tFar = t2; farFace = f2; }
            if (tNear > tFar) return null;
        } else if (eye.y < box.minY || eye.y > box.maxY) {
            return null;
        }

        // Z slab: min-Z faces -Z (NORTH), max-Z faces +Z (SOUTH).
        if (look.z != 0.0) {
            double inv = 1.0 / look.z;
            double t1 = (box.minZ - eye.z) * inv;
            double t2 = (box.maxZ - eye.z) * inv;
            Direction f1 = Direction.NORTH, f2 = Direction.SOUTH;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; Direction f = f1; f1 = f2; f2 = f; }
            if (t1 > tNear) { tNear = t1; nearFace = f1; }
            if (t2 < tFar) { tFar = t2; farFace = f2; }
            if (tNear > tFar) return null;
        } else if (eye.z < box.minZ || eye.z > box.maxZ) {
            return null;
        }

        if (tFar < 0.0) return null;       // box is entirely behind the eye
        if (tNear < 0.0) return farFace;   // eye starts inside the box: highlight the exit face
        return nearFace;
    }
}
