package zhangzhixian.monopoly.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zhangzhixian.monopoly.enums.GridEnum;

import java.util.ArrayList;
import java.util.List;

@Data
public class Grid {

    private String name;

    private GridEnum type;

    private String color;

    private Integer price;

    private String owner;

    private String ownerColor = "white";

    private Integer roomLevel = 0;

    private List<Integer> detail;

    private List<User> users = new ArrayList<>();

}
