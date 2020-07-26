package zhangzhixian.monopoly.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import zhangzhixian.monopoly.model.Card;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.User;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "customize")
public class CustomizeConfiguration {

    private int initMoney;

    private List<Grid> maps;

    private List<User> users;

    private List<Card> chances;
    private List<Card> destiny;
    private List<Card> happiness;

}
