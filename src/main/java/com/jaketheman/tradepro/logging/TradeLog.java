package com.jaketheman.tradepro.logging;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jaketheman.tradepro.util.ItemFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor // Add no-argument constructor
public class TradeLog implements PostProcessor {

  @Expose
  @SerializedName("player1")
  private Trader player1;

  @Expose
  @SerializedName("player2")
  private Trader player2;

  @Expose
  @SerializedName("player1Items")
  private List<ItemFactory> player1Items;

  @Expose
  @SerializedName("player2Items")
  private List<ItemFactory> player2Items;

  @Expose
  @SerializedName("player1ExtraOffers")
  private List<ExtraOffer> player1ExtraOffers;

  @Expose
  @SerializedName("player2ExtraOffers")
  private List<ExtraOffer> player2ExtraOffers;

  @Expose
  @SerializedName("time")
  private Date time;

  public TradeLog(
          OfflinePlayer player1,
          OfflinePlayer player2,
          List<ItemFactory> player1Items,
          List<ItemFactory> player2Items,
          List<ExtraOffer> player1ExtraOffers,
          List<ExtraOffer> player2ExtraOffers) {
    this.player1 = new Trader(player1.getUniqueId(), player1.getName());
    this.player2 = new Trader(player2.getUniqueId(), player2.getName());
    player1Items.sort((o1, o2) -> Integer.compare(o2.getAmount(), o1.getAmount()));
    this.player1Items = player1Items;
    player2Items.sort((o1, o2) -> Integer.compare(o2.getAmount(), o1.getAmount()));
    this.player2Items = player2Items;
    this.player1ExtraOffers = player1ExtraOffers;
    this.player2ExtraOffers = player2ExtraOffers;
    this.time = new Date();
  }

  @Override
  public void doPostProcessing() {
    player1.updateName();
    player2.updateName();
  }

  @Override
  public String toString() {
    return "TradeLog{" +
            "player1=" + player1 +
            ", player2=" + player2 +
            ", player1Items=" + player1Items +
            ", player2Items=" + player2Items +
            ", player1ExtraOffers=" + player1ExtraOffers +
            ", player2ExtraOffers=" + player2ExtraOffers +
            ", time=" + time +
            '}';
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor  // Add NoArgsConstructor
  public static class Trader {

    @Expose
    @SerializedName("uniqueId")
    private UUID uniqueId;

    @Expose
    @SerializedName("lastKnownName")
    private String lastKnownName;

    void updateName() {
      OfflinePlayer op = Bukkit.getOfflinePlayer(uniqueId);
      if (op.getName() == null) lastKnownName = "unknown";
      else lastKnownName = op.getName();
    }

    @Override
    public String toString() {
      return "Trader{" +
              "uniqueId=" + uniqueId +
              ", lastKnownName='" + lastKnownName + '\'' +
              '}';
    }
  }

  @Getter
  @NoArgsConstructor // Add NoArgsConstructor
  public static class ExtraOffer {

    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("value")
    private double value;

    public ExtraOffer(String id, double value) {
      this.id = id;
      this.value = value;
    }

    @Override
    public String toString() {
      return "ExtraOffer{" +
              "id='" + id + '\'' +
              ", value=" + value +
              '}';
    }
  }
}