package zhangzhixian.monopoly.model.dto;

import lombok.Builder;
import lombok.Data;
import zhangzhixian.monopoly.model.Grid;
import zhangzhixian.monopoly.model.User;

import java.util.List;

@Data
@Builder
public class MapDto {

    private List<Grid> map;

    private List<User> users;

}
