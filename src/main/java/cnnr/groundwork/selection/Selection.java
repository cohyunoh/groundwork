package cnnr.groundwork.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Selection {
    private BlockPos cornerA;
    private BlockPos cornerB;
    private ResourceKey<Level> dimension;

    public void setCornerA(BlockPos a, ResourceKey<Level> dim) { this.cornerA = a; this.dimension = dim; }
    public void setCornerB(BlockPos b, ResourceKey<Level> dim) { this.cornerB = b; this.dimension = dim; }

    public boolean isComplete() { return cornerA != null && cornerB != null; }
    public BlockPos getCornerA() { return cornerA; }
    public BlockPos getCornerB() { return cornerB; }
    public ResourceKey<Level> getDimension() { return dimension; }

    public BlockPos min() {
        return new BlockPos(Math.min(cornerA.getX(), cornerB.getX()),
                            Math.min(cornerA.getY(), cornerB.getY()),
                            Math.min(cornerA.getZ(), cornerB.getZ()));
    }
    public BlockPos max() {
        return new BlockPos(Math.max(cornerA.getX(), cornerB.getX()),
                            Math.max(cornerA.getY(), cornerB.getY()),
                            Math.max(cornerA.getZ(), cornerB.getZ()));
    }
    public int volume() {
        BlockPos mn = min(), mx = max();
        return (mx.getX() - mn.getX() + 1) * (mx.getY() - mn.getY() + 1) * (mx.getZ() - mn.getZ() + 1);
    }
    public boolean contains(BlockPos pos) {
        if (!isComplete()) return false;
        BlockPos mn = min(), mx = max();
        return pos.getX() >= mn.getX() && pos.getX() <= mx.getX()
            && pos.getY() >= mn.getY() && pos.getY() <= mx.getY()
            && pos.getZ() >= mn.getZ() && pos.getZ() <= mx.getZ();
    }
}
