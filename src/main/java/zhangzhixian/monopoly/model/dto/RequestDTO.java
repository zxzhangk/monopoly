package zhangzhixian.monopoly.model.dto;

import lombok.Data;
import zhangzhixian.monopoly.enums.RequestMethod;

@Data
public class RequestDTO {

    private RequestMethod method;

    private String token;

    private String sellType;

    private String sellName;

}
