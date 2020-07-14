package tech.brettsaunders.craftory.tech.power.core.block.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tech.brettsaunders.craftory.CoreHolder.Blocks;
import tech.brettsaunders.craftory.api.font.Font;
import tech.brettsaunders.craftory.persistence.Persistent;
import tech.brettsaunders.craftory.tech.power.api.block.BaseGenerator;
import tech.brettsaunders.craftory.tech.power.api.fluids.FluidStorage;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GBattery;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GIndicator;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GOutputConfig;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GTank;

public class GeothermalGenerator extends BaseGenerator {

  /* Static Constants Private */
  private static final byte C_LEVEL = 0;
  private static final int C_OUTPUT_AMOUNT = 100;

  public static final int FUEL_SLOT = 14;
  public static final int OUT_SLOT = 32;
  /* Static Constants Protected */
  protected static final int CAPACITY_BASE = 40000;
  protected static final double[] CAPACITY_LEVEL = {1, 1.5, 2, 3};

  protected static final int LAVA_CAPACITY_BASE = 10000;

  private static final double lavaToEnergyRatio = 25;

  @Persistent
  private FluidStorage fluidStorage;

  /* Construction */
  public GeothermalGenerator() {
    super();
    init();
  }

  /* Saving, Setup and Loading */
  public GeothermalGenerator(Location location) {
    super(location, Blocks.GEOTHERMAL_GENERATOR, C_LEVEL, C_OUTPUT_AMOUNT,(int) (CAPACITY_BASE * CAPACITY_LEVEL[0]));
    init();
    fluidStorage = new FluidStorage((int) (LAVA_CAPACITY_BASE * CAPACITY_LEVEL[C_LEVEL]));
    inputSlots = new ArrayList<>();
    inputSlots.add(0,new ItemStack(Material.AIR));
    outputSlots = new ArrayList<>();
    outputSlots.add(new ItemStack(Material.AIR));
  }

  private void init() {
    interactableSlots = new HashSet<>(Arrays.asList(FUEL_SLOT,OUT_SLOT));
    inputLocations = new ArrayList<>();
    inputLocations.add(0,FUEL_SLOT);
    inputFaces = new HashMap<BlockFace, Integer>() {
      {
        put(BlockFace.NORTH, FUEL_SLOT);
        put(BlockFace.EAST, FUEL_SLOT);
        put(BlockFace.SOUTH, FUEL_SLOT);
        put(BlockFace.WEST, FUEL_SLOT);
        put(BlockFace.UP, FUEL_SLOT);
      }
    };
    outputLocations = new ArrayList<>();
    outputLocations.add(OUT_SLOT);
    outputFaces = new HashMap<BlockFace, Integer>() {
      {
        put(BlockFace.DOWN, OUT_SLOT);
      }
    };
  }

  @Override
  public void updateGenerator(){
    ItemStack input = inventoryInterface.getItem(FUEL_SLOT);
    ItemStack out = inventoryInterface.getItem(OUT_SLOT);
    if(input != null && input.getType().equals(Material.LAVA_BUCKET) && fluidStorage.getSpace() > 1000){
      if(out==null || out.getType().equals(Material.BUCKET) && out.getAmount() < out.getMaxStackSize()) {
        input.setAmount(0);
        inventoryInterface.setItem(FUEL_SLOT, input);
        fluidStorage.forceAdd(1000);
        if(out==null){
          inventoryInterface.setItem(OUT_SLOT,new ItemStack(Material.BUCKET));
        } else {
          out.setAmount(out.getAmount()+1);
          inventoryInterface.setItem(OUT_SLOT,out);
        }
      }
    }
    super.updateGenerator();
  }

  @Override
  protected boolean canStart() {
    return fluidStorage.getFluidStored() > 0 && getEnergySpace() > 0;
  }

  @Override
  protected boolean canFinish() {
    return !canStart();
  }

  @Override
  protected void processTick() {
    double change = Math.min(Math.min(fluidStorage.getFluidStored(),C_OUTPUT_AMOUNT/lavaToEnergyRatio),getEnergySpace()/lavaToEnergyRatio);
    if(change!=0) change = Math.ceil(change);
    int amount = (int) Math.round(change);
    fluidStorage.forceExtract(amount);
    energyStorage.modifyEnergyStored((int) (amount*lavaToEnergyRatio));
  }

  @Override
  public void setupGUI() {
    Inventory inventory = createInterfaceInventory(displayName, Font.GEOTHERMAL_GUI.label + "");
    addGUIComponent(new GBattery(inventory, energyStorage));
    addGUIComponent(new GTank(inventory, fluidStorage));
    addGUIComponent(new GOutputConfig(inventory, sidesConfig, 43, true));
    addGUIComponent(new GIndicator(inventory, runningContainer, 25));
    this.inventoryInterface = inventory;
  }
}