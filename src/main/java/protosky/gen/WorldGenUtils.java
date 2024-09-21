import net.minecraft.util.math.Box;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureStart;

package protosky.gen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;

import java.util.*;

import java.util.Optional;
import java.util.Set;

public class WorldGenUtils {

    private static final Set<Block> BLOCKS_TO_KEEP = Set.of(
        Blocks.BARREL,
        Blocks.CHEST,
        Blocks.OBSIDIAN
    );

    private static final Set<Structure> STRUCTURES_TO_KEEP = Set.of(
        Structure.STRONGHOLD,
        Structure.END_CITY,
        Structure.NETHER_FORTRESS,
        Structure.BASTION_REMNANT
    );

    public static boolean isBlockInWhitelistedStructure(WorldAccess world, BlockPos pos) {
        StructureManager structureManager = world.getStructureManager();
        Chunk chunk = world.getChunk(pos);
        
        for (Structure structure : STRUCTURES_TO_KEEP) {
            Optional<? extends StructureStart> structureStartOptional = chunk.getStructureStart(structure);
            if (structureStartOptional.isPresent()) {
                StructureStart structureStart = structureStartOptional.get();
                Box boundingBox = structureStart.getBoundingBox();
                
                // Check if the block is within the structure's bounding box
                if (boundingBox.contains(pos.getX(), pos.getY(), pos.getZ())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBlockInWhitelist(BlockState blockState) {
        return BLOCKS_TO_KEEP.contains(blockState.getBlock());
    }

    public static boolean deleteBlocks(ProtoChunk chunk, boolean check, WorldAccess world) {
        boolean reset = false;
        ChunkSection[] sections = chunk.getSectionArray();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection chunkSection = sections[i];
            if (check || !chunkSection.isEmpty()) {
                reset = true;
                PalettedContainer<BlockState> blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
                ReadableContainer<RegistryEntry<Biome>> biomeContainer = chunkSection.getBiomeContainer();

                for (BlockPos pos : BlockPos.iterate(0, 0, 0, 15, chunk.getHeight() - 1, 15)) {
                    BlockState state = chunkSection.getBlockState(pos.getX(), pos.getY(), pos.getZ());

                    boolean isInStructureWhitelist = isBlockInWhitelistedStructure(world, pos);
                    boolean isInBlockWhitelist = isBlockInWhitelist(state);
                    if (isInStructureWhitelist || isInBlockWhitelist) {
                        blockStateContainer.set(pos.getX(), pos.getY(), pos.getZ(), state);  // Keep block
                    } else {
                        blockStateContainer.set(pos.getX(), pos.getY(), pos.getZ(), Blocks.AIR.getDefaultState());  // Delete block
                    }
                }
                
                sections[i] = new ChunkSection(blockStateContainer, biomeContainer);
            }
        }

        for (BlockPos bePos : chunk.getBlockEntityPositions()) {
            chunk.removeBlockEntity(bePos);
        }

        return reset;
    }
}
