package zhangzhixian.monopoly.model;

import lombok.Data;
import zhangzhixian.monopoly.enums.CardEnum;

@Data
public class Card {

    private String name;

    private CardEnum type;

    private int price;

    private int hotelPrice;

}
