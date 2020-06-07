package tech.brettsaunders.craftory.tech.power.api.block;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.ItemsAdder;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.bukkit.Location;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GBattery;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GIndicator;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GOneToOneMachine;
import tech.brettsaunders.craftory.utils.RecipeUtils;
import tech.brettsaunders.craftory.utils.VariableContainer;

public class BaseElectricFurnace extends BaseMachine implements Externalizable {

  /* Static Constants Protected */
  protected static final int[] COOKING_TIME_LEVEL = {200, 150, 100, 50}; //MC 200 ticks
  protected static final int[] ENERGY_CONSUMPTION_LEVEL = {20, 30, 50, 100};
  protected static final int[] CAPACITY_LEVEL = {5000, 10000, 25000, 50000};
  /* Static Constants Private */
  private static final long serialVersionUID = 10005L;
  private static final int INPUT_LOCATION = 22;
  private static final int OUTPUT_LOCATION = 26;
  /* Per Object Variables Saved */
  private Inventory inventory;

  /* Per Object Variables Not-Saved */
  private transient ItemStack inputSlot;
  private transient ItemStack outputSlot;
  private transient int cookingTime;
  private transient int energyConsumption;
  private transient int tickCount = 0;
  private transient FurnaceRecipe currentRecipe = null;
  private transient VariableContainer<Boolean> runningContainer;
  private transient VariableContainer<Double> progressContainer;

  /* Construction */
  public BaseElectricFurnace(Location location, byte level) {
    super(location, level, ENERGY_CONSUMPTION_LEVEL[level] * 5);
    init();
    energyStorage = new EnergyStorage(CAPACITY_LEVEL[level]);
    if (ItemsAdder.areItemsLoaded()) {
      setupGUI();
    }
  }

  /* Saving, Setup and Loading */
  public BaseElectricFurnace() {
    super();
    init();
  }

  /* Common Load and Construction */
  public void init() {
    cookingTime = COOKING_TIME_LEVEL[level];
    energyConsumption = ENERGY_CONSUMPTION_LEVEL[level];
    runningContainer = new VariableContainer<>(false);
    progressContainer = new VariableContainer<>(0d);
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(inventory);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    inventory = (Inventory) in.readObject();
  }

  @Override
  public void setupGUI() {
    Inventory inventory = setInterfaceTitle("Electric Furnace", new FontImageWrapper("extra:cell"));
    this.inventory = inventory;
    addGUIComponent(new GOneToOneMachine(inventory, 24, progressContainer));
    addGUIComponent(new GBattery(inventory, energyStorage));
    addGUIComponent(new GIndicator(inventory, runningContainer));
  }

  /* Update Loop */
  @Override
  public void update() {
    super.update();
    if (inventory == null) return;
    updateSlots();
    if (validateContense() && energyStorage.getEnergyStored() >= energyConsumption) {
      energyStorage.modifyEnergyStored(-energyConsumption);
      tickCount += 1;
      if (tickCount == cookingTime) {
        tickCount = 0;
        inputSlot.setAmount(inputSlot.getAmount() - 1);
        if (outputSlot == null) {
          outputSlot = currentRecipe.getResult();
        } else {
          outputSlot.setAmount(outputSlot.getAmount() + currentRecipe.getResult().getAmount());
        }
        inventory.setItem(OUTPUT_LOCATION, outputSlot);
      }
      runningContainer.setT(true);
    } else {
      runningContainer.setT(false);
    }
    progressContainer.setT(((double) tickCount) / cookingTime);
  }

  /* Internal Helper Functions */
  private void updateSlots() {
    inputSlot = inventory.getItem(INPUT_LOCATION);
    outputSlot = inventory.getItem(OUTPUT_LOCATION);
  }

  private boolean validateContense() {
    if (inputSlot == null) {
      return false;
    }
    String inputType = inputSlot.getType().toString();
    //If the recipe is unchanged there is no need to find the recipe.
    if (currentRecipe != null && currentRecipe.getInput().getType().toString().equals(inputType)) {
      if (outputSlot == null) {
        return true;
      }
      if (outputSlot.getType().toString() == currentRecipe.getResult().getType().toString()
          && outputSlot.getAmount() < outputSlot.getMaxStackSize()) {
        return true;
      }
    }
    FurnaceRecipe furnaceRecipe;
    for (Recipe recipe : RecipeUtils.getFurnaceRecipes()) {
      furnaceRecipe = (FurnaceRecipe) recipe;
      if (furnaceRecipe.getInput().getType().toString() != inputType) {
        continue;
      }
      currentRecipe = furnaceRecipe;
      if (outputSlot == null) {
        return true;
      }
      if (outputSlot.getType().toString() == recipe.getResult().getType().toString()
          && outputSlot.getAmount() < outputSlot.getMaxStackSize()) {
        return true;
      }
    }
    currentRecipe = null;
    return false;
  }

}
