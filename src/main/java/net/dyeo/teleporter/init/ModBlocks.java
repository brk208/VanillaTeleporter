package net.dyeo.teleporter.init;

import java.util.Map;
import java.util.UUID;
import com.google.common.collect.Maps;
import net.dyeo.teleporter.TeleporterMod;
import net.dyeo.teleporter.block.BlockTeleporter;
import net.dyeo.teleporter.block.BlockTeleporter.EnumType;
import net.dyeo.teleporter.common.config.ModConfiguration;
import net.dyeo.teleporter.item.ItemBlockTeleporter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ModBlocks
{

	public static final Block TELEPORTER = new BlockTeleporter().setRegistryName("teleporter").setUnlocalizedName("teleporter");


	public static void registerBlocks()
	{
		ForgeRegistries.BLOCKS.register(TELEPORTER);
		ForgeRegistries.ITEMS.register(new ItemBlockTeleporter(TELEPORTER).setRegistryName(TELEPORTER.getRegistryName()));
	}


	public static void registerBlockVariants()
	{
		ModelBakery.registerItemVariants(Item.getItemFromBlock(ModBlocks.TELEPORTER),
			new ResourceLocation(TeleporterMod.MODID, EnumType.REGULAR.getRegistryName()),
			new ResourceLocation(TeleporterMod.MODID, EnumType.ENDER.getRegistryName())
		);
	}

	public static void registerInventoryModels()
	{
		Item item = Item.getItemFromBlock(TELEPORTER);
		for ( EnumType type : EnumType.values() )
		{
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, type.getMetadata(), new ModelResourceLocation(TeleporterMod.MODID + ":" + type.getRegistryName(), "inventory"));
		}
	}


	public static void registerCraftingRecipes()
	{
		if (ModConfiguration.useDiamonds == true)
		{
			addShapedRecipe(new ItemStack(ModBlocks.TELEPORTER, ModConfiguration.numTeleporters, 0),
				new Object[] {
					"AAA", "DCD", "EBE",
					'A', Blocks.GLASS, 'B', Items.ENDER_PEARL, 'C', Blocks.REDSTONE_BLOCK, 'D', Blocks.IRON_BLOCK, 'E', Items.DIAMOND
				}
			);
			addShapedRecipe(new ItemStack(ModBlocks.TELEPORTER, ModConfiguration.numTeleporters, 1),
				new Object[] {
					"AAA", "DCD", "EBE",
					'A', Blocks.GLASS, 'B', Items.ENDER_EYE, 'C', Blocks.GLOWSTONE, 'D', Blocks.OBSIDIAN, 'E', Items.DIAMOND
				}
			);
		}
		else
		{
			addShapedRecipe(new ItemStack(ModBlocks.TELEPORTER, ModConfiguration.numTeleporters, 0),
				new Object[] {
					"AAA", "DCD", "DBD",
					'A', Blocks.GLASS, 'B', Items.ENDER_PEARL, 'C', Blocks.REDSTONE_BLOCK, 'D', Blocks.IRON_BLOCK
				}
			);
			addShapedRecipe(new ItemStack(ModBlocks.TELEPORTER, ModConfiguration.numTeleporters, 1),
				new Object[] {
					"AAA", "DCD", "DBD",
					'A', Blocks.GLASS, 'B', Items.ENDER_EYE, 'C', Blocks.GLOWSTONE, 'D', Blocks.OBSIDIAN
				}
			);
		}
	}



	private static void addShapedRecipe(ItemStack stack, Object... recipeComponents)
	{
		try
		{
			String name = UUID.randomUUID().toString();
			String s = "";
			int i = 0;
			int j = 0;
			int k = 0;

			if (recipeComponents[i] instanceof String[])
			{
				String[] astring = (String[])((String[])recipeComponents[i++]);

				for (String s2 : astring)
				{
					++k;
					j = s2.length();
					s = s + s2;
				}
			}
			else
			{
				while (recipeComponents[i] instanceof String)
				{
					String s1 = (String)recipeComponents[i++];
					++k;
					j = s1.length();
					s = s + s1;
				}
			}

			Map<Character, ItemStack> map;

			for (map = Maps.<Character, ItemStack>newHashMap(); i < recipeComponents.length; i += 2)
			{
				Character character = (Character)recipeComponents[i];
				ItemStack itemstack = ItemStack.EMPTY;

				if (recipeComponents[i + 1] instanceof Item)
				{
					itemstack = new ItemStack((Item)recipeComponents[i + 1]);
				}
				else if (recipeComponents[i + 1] instanceof Block)
				{
					itemstack = new ItemStack((Block)recipeComponents[i + 1], 1, 32767);
				}
				else if (recipeComponents[i + 1] instanceof ItemStack)
				{
					itemstack = (ItemStack)recipeComponents[i + 1];
				}

				map.put(character, itemstack);
			}

			NonNullList<Ingredient> aitemstack = NonNullList.withSize(j * k, Ingredient.EMPTY);

			for (int l = 0; l < j * k; ++l)
			{
				char c0 = s.charAt(l);

				if (map.containsKey(Character.valueOf(c0)))
				{
					aitemstack.set(l, Ingredient.fromStacks(((ItemStack)map.get(Character.valueOf(c0))).copy()));
				}
			}

			ForgeRegistries.RECIPES.register(new ShapedRecipes(name, j, k, aitemstack, stack).setRegistryName(new ResourceLocation(TeleporterMod.MODID, name)));
		}
		catch (Exception ex)
		{
			TeleporterMod.LOGGER.catching(ex);
		}
	}


}
