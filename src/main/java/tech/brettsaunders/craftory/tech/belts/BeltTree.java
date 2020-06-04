package tech.brettsaunders.craftory.tech.belts;

import java.io.Serializable;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import tech.brettsaunders.craftory.utils.Logger;

public class BeltTree implements Serializable {
  private static final long serialVersionUID = 10003L;

  private final HashMap<Location, BeltNode> parents = new HashMap<>();
  private final HashMap<Location, BeltNode> mapper = new HashMap<>();
  private BeltNode root;

  public BeltTree(Location location) {
    root = new BeltNode(location);
    parents.put(location, root);
    mapper.put(location, root);
  }

  public BeltNode getRoot() {
    return root;
  }

  public void setRoot(BeltNode root) {
    this.root = root;
  }

  public BeltNode getParent(Location location) {
    return parents.get(location);
  }

  public HashMap<Location, BeltNode> getParents() {
    return parents;
  }

  public void replaceParent(Location toRemove, Location toAdd, BeltNode node) {
    parents.remove(toRemove);
    parents.put(toAdd, node);
  }

  public void replaceIfParent(Location toRemove, Location toAdd, BeltNode node) {
    if (parents.remove(toRemove) != null) {
      parents.put(toAdd, node);
    }
  }

  public void addParent(Location toAdd, BeltNode node) {
    parents.put(toAdd, node);
  }

  public void addToMapper(Location location, BeltNode beltNode) {
    mapper.put(location, beltNode);
  }

  public BeltNode getNodeAt(Location location) {
    return mapper.get(location);
  }

  public void print(Player player) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("\n");
    printResuriveFancy(player, stringBuilder, "", "", root, true);
    Logger.info(stringBuilder.toString());
  }

  private void printResurive(BeltNode node) {
    if (node == null) {
      return;
    }
    Logger.infoDiscrete("Node :: " + node.toString());

    Logger.info("Behind");
    printResurive(node.getParentBehind());
    Logger.info("Left");
    printResurive(node.getParentLeft());
    Logger.info("Right");
    printResurive(node.getParentRight());
  }

  private void printResuriveFancy(Player player, StringBuilder stringBuilder, String padding,
      String pointer, BeltNode node, Boolean recus) {
    if (node == null) {
      return;
    }
    stringBuilder.append(padding);
    stringBuilder.append(pointer);
    stringBuilder.append(node.toString());
    stringBuilder.append("\n");
    Location location = node.getLocation().clone();
    location.add(0.5, 1.5, 0.5);
    player.spawnParticle(Particle.FLAME, location, 5, 0, 0, 0, 0);

    StringBuilder paddingBuilder = new StringBuilder(padding);
    paddingBuilder.append("│  ");

    String paddingForBoth = paddingBuilder.toString();
    String pointerForRight = "└──";
    String pointerForLeft = "├──";

    if (!recus) {
      return;
    }

    printResuriveFancy(player, stringBuilder, paddingForBoth, pointerForLeft, node.getParentLeft(),
        node.getParentLeft() != node);
    printResuriveFancy(player, stringBuilder, paddingForBoth, pointerForLeft,
        node.getParentBehind(), node.getParentBehind() != node);
    printResuriveFancy(player, stringBuilder, paddingForBoth, pointerForRight,
        node.getParentRight(), node.getParentRight() != node);
  }
}
