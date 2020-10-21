/*
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders & Matty Jones
 */

package tech.brettsaunders.craftory.tech.power.core.block.storage_drive;

import static tech.brettsaunders.craftory.tech.power.api.storage_drive.StorageDrive.isDrive;

import de.tr7zw.changeme.nbtapi.NBTItem;
import io.sentry.util.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.brettsaunders.craftory.Constants.Blocks;
import tech.brettsaunders.craftory.Constants.Items;
import tech.brettsaunders.craftory.api.blocks.CustomBlockTickManager.Ticking;
import tech.brettsaunders.craftory.api.font.Font;
import tech.brettsaunders.craftory.api.items.CustomItemManager;
import tech.brettsaunders.craftory.api.tasks.Tasks;
import tech.brettsaunders.craftory.tech.power.api.block.BaseMachine;
import tech.brettsaunders.craftory.tech.power.api.block.EnergyStorage;
import tech.brettsaunders.craftory.tech.power.api.gui_components.GBattery;
import tech.brettsaunders.craftory.tech.power.api.storage_drive.StorageDrive;

public class DriveViewer extends BaseMachine {

  private static final int DRIVE_SLOT = 12;
  private static final List<Integer> CONTENT_SLOTS = new ArrayList<>();
  private static final Set<InventoryAction> PLACE_ACTIONS = new HashSet<>(Arrays
      .asList(InventoryAction.PLACE_ALL,
          InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME, InventoryAction.SWAP_WITH_CURSOR));
  private static final Set<InventoryAction> PICKUP_ACTIONS = new HashSet<>(Arrays
      .asList(InventoryAction.PICKUP_ALL,
          InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME, InventoryAction.SWAP_WITH_CURSOR));

  private ItemStack drive;

  private boolean running = false;
  private boolean loaded = false;
  private boolean saving = false;

  private Map<String,Integer> items = new HashMap<>();
  private int types = 0;
  private int capacity = 0;

  static {
    for(int i = 14; i <= 41; i+=9) {
      for(int j = 0; j < 4; j++) {
        CONTENT_SLOTS.add(i+j);
      }
    }
  }

  public DriveViewer(Location location) {
    super(location, Blocks.DRIVE_VIEWER, (byte) 0, 200);
    setup();
    energyStorage = new EnergyStorage(10000);
  }

  public DriveViewer() {
    super();
    setup();
  }

  @Override
  public void beforeSaveUpdate() {
    saving = true;
    if(drive!=null && CustomItemManager.getCustomItemName(drive).equals(Items.BASIC_STORAGE_DRIVE)){
      saveItems();
      inventoryInterface.setItem(DRIVE_SLOT, drive);
    }
    super.beforeSaveUpdate();
  }

  @Override
  public void afterLoadUpdate() {
    super.afterLoadUpdate();
    running = false;
  }

  @Override
  public void blockBreak() {
    saveItems();
    inputSlots.set(0,drive);
    clearItems();
    super.blockBreak();
  }

  private void setup() {
    outputLocations = new ArrayList<>();
    inputLocations = new ArrayList<>();
    inputLocations.add(DRIVE_SLOT);
    interactableSlots = new HashSet<>(Collections.singletonList(DRIVE_SLOT));
    interactableSlots.addAll(CONTENT_SLOTS);
    this.energyConsumption = 10;
  }

  /* Update Loop */
  @Ticking(ticks = 1)
  @Override
  public void updateMachine() {
    if(inventoryInterface==null) return;
    drive = inventoryInterface.getItem(DRIVE_SLOT);
    inputSlots.set(0, drive);
    if(drive==null || drive.getType().equals(Material.AIR)) return;
    if(inventoryInterface.getViewers().isEmpty() || !CustomItemManager.getCustomItemName(drive).equals(Items.BASIC_STORAGE_DRIVE)) return;
    if(hasSufficientEnergy()) {
      if(!running) {
        running = true;
        loadItems();
      }
    } else {
      if(running) {
        saveItems();
        running = false;
      }
    }
  }

  @EventHandler
  public void inventoryInteract(InventoryClickEvent event) {
    if(event.getInventory()!=inventoryInterface || !running) return;
    if(event.getAction()==InventoryAction.COLLECT_TO_CURSOR) {
      event.setCancelled(true);
      return;
    }
    int slot = event.getRawSlot();
    if(slot==DRIVE_SLOT) {
      handleDriveSlotClick(event);
    } else if(CONTENT_SLOTS.contains(slot)) {
      handleContentClick(event);
    } else if(event.isShiftClick() && slot > 53) {
      handlePlayerInventoryShiftClick(event);
    }
  }

  private void handleContentClick(InventoryClickEvent event) {
    event.setCancelled(true);
    if(!loaded || !running) return;
    if(event.getCursor()!=null && event.getCursor().getType()!=Material.AIR) {
      if(CustomItemManager.getCustomItemName(event.getCursor()).equals(Items.BASIC_STORAGE_DRIVE)) {
        return;
      }
      ItemStack itemStack = event.getCursor();
      int amountSet = addItemToDrive(itemStack);
      if (amountSet > 0) {
        itemStack.setAmount(itemStack.getAmount() - amountSet);
      }
    } else if (event.getCurrentItem()!=null && event.getCurrentItem().getType()!=Material.AIR) {
      ItemStack item = event.getCurrentItem();
      int amount = getAmountFromClick(event.getClick(), item.getMaxStackSize());
      ItemStack toGive = removeItemFromDrive(item, amount);
      if(toGive==null) return;
      Map<Integer,ItemStack> failedToAdd = event.getWhoClicked().getInventory().addItem(toGive);
      if(!failedToAdd.isEmpty()) {
        addItemToDrive(failedToAdd.get(0));
      }
    }
  }

  private static int getAmountFromClick(ClickType click, int stackSize) {
    switch (click) {
      case LEFT:
        return 1;
      case SHIFT_LEFT:
        return stackSize;
      case RIGHT:
        return 5;
      case SHIFT_RIGHT:
        return stackSize * 5;
      case MIDDLE:
        return stackSize / 2;
      default:
        return 1;
    }
  }

  private ItemStack generateDisplayItem(ItemStack itemStack) {
    return generateDisplayItem(CustomItemManager.getCustomItemName(itemStack));
  }

  private ItemStack generateDisplayItem(String name) {
    if(items.containsKey(name)){
      ItemStack itemStack = CustomItemManager.getCustomItemOrDefault(name);
      itemStack.setAmount(1);
      ItemMeta meta = itemStack.getItemMeta();
      meta.setLore(Collections.singletonList(ChatColor.BLUE + " X " + items.get(name)));
      itemStack.setItemMeta(meta);
      return itemStack;
    } else {
      return null;
    }

  }
  private void handlePlayerInventoryShiftClick(InventoryClickEvent event) {
    event.setCancelled(true);
    if(inventoryInterface.getItem(DRIVE_SLOT)==null && CustomItemManager.getCustomItemName(event.getCurrentItem()).equals(Items.BASIC_STORAGE_DRIVE)){
      drive = event.getCurrentItem();
      loadItems();
      inventoryInterface.setItem(DRIVE_SLOT, drive);
      return;
    }
    ItemStack itemStack = event.getCurrentItem();
    if(itemStack==null || itemStack.getType()==Material.AIR) {
      return;
    }
    int amountSet = addItemToDrive(itemStack);
    if (amountSet > 0) {
      itemStack.setAmount(itemStack.getAmount() - amountSet);
      event.setCurrentItem(itemStack);
    }
  }

  private void handleDriveSlotClick(InventoryClickEvent event) {
    if(PICKUP_ACTIONS.contains(event.getAction()) && CustomItemManager.getCustomItemName(drive).equals(Items.BASIC_STORAGE_DRIVE)) {
      saveItems();
      event.setCurrentItem(drive);
    }
    if(PLACE_ACTIONS.contains(event.getAction())) {
      drive = event.getCursor();
      loadItems();
    }
  }

  /**
   * Adds an item to the drive
   * @param item The item to add to the drive
   * @return How many of the items in the stack were added to the drive (0 if drive is full)
   */
  private int addItemToDrive(@NonNull ItemStack item) {
    if(!running || !loaded) return 0;
    String name = CustomItemManager.getCustomItemName(item);
    int amount = item.getAmount();
    int total = StorageDrive.totalItemsInDrive(items);
    if (total + amount > capacity) {
      amount = capacity - total;
    }
    if (amount <= 0) {
      return 0;
    }
    if(items.containsKey(name)) {
      items.put(name,items.get(name) + amount);
      updateDisplayItem(name);
    } else if (items.size() < types) {
      items.put(name, amount);
      inventoryInterface.setItem(CONTENT_SLOTS.get(items.size()-1),generateDisplayItem(item));
    } else {
      return 0;
    }
    updateDrive();
    return amount;
  }

  private void updateDrive() {
    drive = StorageDrive.updateLoadedLore(drive, items, types, capacity);
    inventoryInterface.setItem(DRIVE_SLOT,drive);
  }

  /**
   * Removes the given amount of an item from the drive if possible
   * @param name The name of the item to remove
   * @param amount The amount of the item to remove
   * @return The ItemStack removed (null if unable to remove)
   */
  @Nullable
  private ItemStack removeItemFromDrive(String name, int amount) {
    if(items.containsKey(name)) {
      ItemStack item = CustomItemManager.getCustomItemOrDefault(name);
      int stored = items.get(name);
      if (stored > amount) {
        items.put(name, stored-amount);
        updateDisplayItem(name);
      } else {
        amount = stored;
        removeDisplayItem(name);
        items.remove(name);
      }
      item.setAmount(amount);
      updateDrive();
      return item;
    }
    return null;
  }

  private void removeDisplayItem(String name) {
    for(int slot: CONTENT_SLOTS) {
      ItemStack displayItem = inventoryInterface.getItem(slot);
      if(displayItem!=null && name.equals(CustomItemManager.getCustomItemName(displayItem))) {
        inventoryInterface.clear(slot);
        break;
      }
    }
  }


  private void updateDisplayItem(String name) {
    for(int slot: CONTENT_SLOTS) {
      ItemStack displayItem = inventoryInterface.getItem(slot);
      if(displayItem!=null && name.equals(CustomItemManager.getCustomItemName(displayItem))) {
        inventoryInterface.setItem(slot, generateDisplayItem(name));
        break;
      }
    }
  }
  /**
   * Removes the given amount of an item from the drive if possible
   * @param toRemove The ItemStack to remove
   * @param amount The amount of the item to remove
   * @return The ItemStack removed (null if unable to remove)
   */
  @Nullable
  private ItemStack removeItemFromDrive(ItemStack toRemove, int amount) {
    return removeItemFromDrive(CustomItemManager.getCustomItemName(toRemove),amount);
  }




  private void saveItems() {
    if(drive==null || !loaded) return;
    drive = StorageDrive.saveItemsToDrive(drive, items);
    inventoryInterface.setItem(DRIVE_SLOT, drive);
    clearItems();
  }

  private void loadItems() {
    if(loaded) return;
    if(!CustomItemManager.getCustomItemName(drive).equals(Items.BASIC_STORAGE_DRIVE)){
      return;
    }
    NBTItem nbtItem = new NBTItem(drive);
    if(isDrive(nbtItem)) {
      capacity = nbtItem.getInteger(StorageDrive.CAPACITY_KEY);
      types = nbtItem.getInteger(StorageDrive.TYPES_KEY);
    } else {
      return;
    }
    items = StorageDrive.getItemsFromDrive(drive);
    int c = 0;
    for (String name: items.keySet()) {
      inventoryInterface.setItem(CONTENT_SLOTS.get(c), generateDisplayItem(name));
      c++;
    }
    interactableSlots.removeAll(CONTENT_SLOTS);
    interactableSlots.addAll(CONTENT_SLOTS);
    loaded = true;
    refreshInventories();
  }

  private void refreshInventories() {
    Tasks.runTaskLater(() -> {
      for(HumanEntity viewer: inventoryInterface.getViewers()) {
        if(viewer instanceof Player) ((Player) viewer).updateInventory();
      }
    },1);
  }

  private void clearItems() {
    for(int slot : CONTENT_SLOTS) {
      inventoryInterface.setItem(slot, new ItemStack(Material.AIR));
    }
    interactableSlots.removeAll(CONTENT_SLOTS);
    loaded = false;
    if(!saving) refreshInventories();
  }

  @Override
  public void setupGUI() {
    Inventory inventory = createInterfaceInventory(displayName, Font.DRIVE_VIEWER.label + "");
    addGUIComponent(new GBattery(inventory, energyStorage));
    if (inputSlots.isEmpty()) {
      inputSlots.add(0, new ItemStack(Material.AIR));
    }
    this.inventoryInterface = inventory;
  }

  @Override
  protected void processComplete() {
    // Not used
  }

  @Override
  protected boolean validateContentes() {
    return false;
  }

  @Override
  protected void updateSlots() {
    // Not used
  }

}
