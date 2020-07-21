package zhangzhixian.monopoly.model;

import lombok.Data;
import zhangzhixian.monopoly.enums.StatusEnum;

@Data
public class User {

    private String token;

    private String name;

    private String nick;

    private String color;

    private StatusEnum status;

    private int position = -1;

}
