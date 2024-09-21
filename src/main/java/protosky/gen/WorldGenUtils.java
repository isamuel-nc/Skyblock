import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.Optional;
import java.util.Set;

public class WorldGenUtils {

    // Define a set of blocks that should not be deleted
    private static final Set<Block> BLOCKS_TO_KEEP = Set.of(
        Blocks.BARREL,
        Blocks.CHEST,
        Blocks.OBSIDIAN
    );

    // Define a set of structures that should not be deleted
    private static final Set<Structure> STRUCTURES_TO_KEEP = Set.of(
        Structure.STRONGHOLD,   // Needed to find the End Portal and access The End
        Structure.END_CITY,     // Needed for acquiring Elytra and Shulker Boxes after defeating the Ender Dragon
        Structure.NETHER_FORTRESS, // Required to get Blaze Rods for brewing and Eyes of Ender
        Structure.BASTION_REMNANT // Useful for getting resources like gold, Pigstep music disc, and Piglin barter opportunities
    );

    // Method to check if a block is within a whitelisted structure
    public static boolean isBlockInWhitelistedStructure(WorldAccess world, BlockPos pos) {
        StructureManager structureManager = world.getStructureManager();
        Chunk chunk = world.getChunk(pos);
        
        // Iterate through all whitelisted structures
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

    // Method to check if a block is in the whitelist of blocks to keep
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
