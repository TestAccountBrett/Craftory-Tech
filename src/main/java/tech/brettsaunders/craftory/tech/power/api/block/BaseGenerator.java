package tech.brettsaunders.craftory.tech.power.api.block;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Time;
import org.bukkit.Location;
import tech.brettsaunders.craftory.tech.power.api.guiComponents.GBattery;
import tech.brettsaunders.craftory.utils.VariableContainer;

public abstract class BaseGenerator extends BaseProvider implements Externalizable {

  /* Static Constants Private */
  private static final long serialVersionUID = 10006L;

  /* Static Constants Protected */
  protected static final int CAPACITY_BASE = 40000;
  protected static final double[] CAPACITY_LEVEL = { 1, 1.5, 2, 3 };

  /* Per Object Variables Saved */
  protected int fuelRF;

  /* Per Object Variables Not-Saved */
  protected transient int maxFuelRF;
  protected transient int lastEnergy;
  protected transient boolean isActive;
  protected transient boolean wasActive;

  /* Construction */
  public BaseGenerator(Location location, byte level, int outputAmount) {
    super(location, level, outputAmount);
    energyStorage = new EnergyStorage((int) (CAPACITY_BASE * CAPACITY_LEVEL[level]));
    init();
  }

  /* Common Load and Construction */
  private void init() {
    isActive = true;
    isProvider = true;
    addGUIComponent(new GBattery(getInventory(), energyStorage));
  }

  /* Saving, Setup and Loading */
  public BaseGenerator() {
    super();
    init();
    wasActive = false;
    lastEnergy = 0;
    maxFuelRF = 0;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeInt(fuelRF);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    fuelRF = in.readInt();
  }

  /* Update Loop */
  @Override
  public void update() {
    super.update();

    boolean curActive = isActive;

    if (isActive) {
      processTick();

      if (canFinish()) {
        if (!canStart()) {
          processOff();
        } else {
          processStart();
        }
      }
    } else {
      if (timeCheck()) {
        if (canStart()) {
          processStart();
          processTick();
          isActive = true;
        }
      }
    }
    if (timeCheck()) {
      if (!isActive) {
        processIdle();
      }
    }
  }

  /* Internal Helper Functions */
  protected boolean timeCheck() {
    //Time check factor to slow down check speed;
    //Core Props
    //return world.getTotalWorldTime() % TIME_CONSTANT == 0;
    return true;
  }

  protected abstract boolean canStart();

  protected boolean canFinish() {
    return fuelRF <= 0;
  }

  protected abstract void processStart();

  protected void processFinish(){}

  protected void processIdle(){}

  protected void processOff() {
    isActive = false;
    wasActive = true;
  }

  protected int processTick() {
    energyStorage.modifyEnergyStored(80); //TODO need to fix look at old code
    fuelRF -= lastEnergy;
    lastEnergy = transferEnergy();
    return lastEnergy;
  }

  /* External Methods */
  public final void setEnergyStored(int quantity) {
    energyStorage.setEnergyStored(quantity);
  }

  /* IEnergyInfo */
  @Override
  public int getInfoEnergyPerTick() {
    if (!isActive) {
      return 0;
    }
    return lastEnergy;
  }

  @Override
  public int getInfoEnergyStored() {
    return energyStorage.getEnergyStored();
  }

}
